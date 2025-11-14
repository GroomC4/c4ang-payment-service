package com.groom.payment.application.service

import com.groom.payment.application.dto.CompletePaymentCommand
import com.groom.payment.application.dto.CompletePaymentResult
import com.groom.payment.domain.port.IdempotencyPort
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.OrderPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 결제 완료 Application 서비스
 *
 * PG사 콜백으로 결제 완료 처리
 *
 * 멱등성 보장:
 * - IdempotencyPort: 중복 콜백 방지
 *
 * 동시성 제어:
 * - PaymentLockManager: 분산 락 관리
 *
 * 트랜잭션:
 * - @Transactional: JPA dirty checking으로 자동 저장
 *
 * DDD 패턴:
 * - OrderPort를 통한 다른 도메인 연동 (Hexagonal Architecture)
 * - IdempotencyPort를 통한 인프라 기술 추상화
 */
@Service
class CompletePaymentService(
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentLockManager: PaymentLockManager,
    private val idempotencyPort: IdempotencyPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val orderPort: OrderPort,
    private val paymentEventFactory: PaymentEventFactory,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(
        command: CompletePaymentCommand,
        now: LocalDateTime = LocalDateTime.now(),
    ): CompletePaymentResult {
        return paymentLockManager.executeWithLock(command.paymentId) {
            // 멱등성 확인 (PG 콜백 중복 방지)
            if (!idempotencyPort.ensureIdempotency(command.idempotencyKey, Duration.ofHours(24))) {
                logger.info { "Duplicate payment completion callback: ${command.idempotencyKey}" }

                // 이미 처리된 요청 - 기존 Payment 조회하여 반환
                val payment =
                    loadPaymentPort.loadById(command.paymentId)
                        ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

                logger.info { "Payment already processed (idempotent): paymentId=${payment.id}" }

                return@executeWithLock CompletePaymentResult(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    status = payment.status,
                    pgApprovalNumber = payment.pgApprovalNumber ?: command.pgApprovalNumber,
                    completedAt = payment.completedAt ?: now,
                    alreadyProcessed = true,
                )
            }

            // Payment 조회
            val payment =
                loadPaymentPort.loadById(command.paymentId)
                    ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

            // 결제 완료 (도메인 메서드)
            // JPA dirty checking으로 자동 저장
            payment.complete(command.pgApprovalNumber)
            logger.info {
                "Payment completed: paymentId=${payment.id}, orderId=${payment.orderId}, " +
                    "pgApprovalNumber=${command.pgApprovalNumber}"
            }

            // 재고 예약 확정 (Redis → DB) - Port를 통한 위임
            orderPort.confirmStockReservation(payment.orderId)

            // 도메인 이벤트 생성 및 발행 (AFTER_COMMIT)
            val event = paymentEventFactory.createPaymentCompletedEvent(payment)
            eventPublisher.publishEvent(event)

            logger.info { "Payment completed successfully: paymentId=${payment.id}, orderId=${payment.orderId}" }

            CompletePaymentResult(
                paymentId = payment.id,
                orderId = payment.orderId,
                status = payment.status,
                pgApprovalNumber = command.pgApprovalNumber,
                completedAt = payment.completedAt!!,
                alreadyProcessed = false,
            )
        }
    }
}
