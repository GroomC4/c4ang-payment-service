package com.groom.payment.application.service

import com.groom.payment.application.dto.CompletePaymentRefundCommand
import com.groom.payment.application.dto.CompletePaymentRefundResult
import com.groom.payment.domain.port.IdempotencyPort
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 환불 완료 Application 서비스
 *
 * PG사 콜백으로 환불 완료 처리
 *
 * 멱등성 보장:
 * - IdempotencyPort: 중복 콜백 방지
 *
 * 동시성 제어:
 * - PaymentLockManager: 분산 락 관리
 *
 * 트랜잭션:
 * - @Transactional: JPA dirty checking으로 자동 저장
 */
@Service
class CompletePaymentRefundService(
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentLockManager: PaymentLockManager,
    private val idempotencyPort: IdempotencyPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentEventFactory: PaymentEventFactory,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: CompletePaymentRefundCommand): CompletePaymentRefundResult {
        return paymentLockManager.executeWithLock(command.paymentId) {
            // 멱등성 확인 (PG 콜백 중복 방지)
            if (!idempotencyPort.ensureIdempotency(command.idempotencyKey, Duration.ofHours(24))) {
                logger.info { "Duplicate refund completion callback: ${command.idempotencyKey}" }

                // 이미 처리된 요청 - 기존 Payment 조회하여 반환
                val payment =
                    loadPaymentPort.loadById(command.paymentId)
                        ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

                return@executeWithLock CompletePaymentRefundResult(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    status = payment.status,
                    refundTransactionId = payment.refundTransactionId ?: command.refundTransactionId,
                    refundedAt = payment.refundedAt ?: LocalDateTime.now(),
                    alreadyProcessed = true,
                )
            }

            // Payment 조회
            val payment =
                loadPaymentPort.loadById(command.paymentId)
                    ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

            // 환불 완료 (도메인 메서드)
            payment.completeRefund(command.refundTransactionId)

            // JPA dirty checking으로 자동 저장
            logger.info {
                "Payment refund completed: paymentId=${payment.id}, orderId=${payment.orderId}, " +
                    "refundTransactionId=${command.refundTransactionId}"
            }

            // 도메인 이벤트 생성 및 발행 (AFTER_COMMIT)
            val event = paymentEventFactory.createPaymentRefundCompletedEvent(payment)
            eventPublisher.publishEvent(event)

            CompletePaymentRefundResult(
                paymentId = payment.id,
                orderId = payment.orderId,
                status = payment.status,
                refundTransactionId = command.refundTransactionId,
                refundedAt = payment.refundedAt!!,
                alreadyProcessed = false,
            )
        }
    }
}
