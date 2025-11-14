package com.groom.payment.application.service

import com.groom.payment.application.dto.CancelPaymentCommand
import com.groom.payment.application.dto.CancelPaymentResult
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 취소 Application 서비스
 *
 * 사용자 또는 시스템이 결제를 취소
 *
 * 사용 시점:
 * - 주문 타임아웃
 * - 사용자 취소 요청
 * - 시스템 취소 (재고 부족 등)
 *
 * 동시성 제어:
 * - PaymentLockManager: 분산 락 관리
 *
 * 트랜잭션:
 * - @Transactional: JPA dirty checking으로 자동 저장
 */
@Service
class CancelPaymentService(
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentLockManager: PaymentLockManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentEventFactory: PaymentEventFactory,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: CancelPaymentCommand): CancelPaymentResult =
        paymentLockManager.executeWithLock(command.paymentId) {
            // Payment 조회
            val payment =
                loadPaymentPort.loadById(command.paymentId)
                    ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

            val previousStatus = payment.status

            // 결제 취소 (도메인 메서드)
            payment.cancel(command.reason)

            // JPA dirty checking으로 자동 저장
            logger.info {
                "Payment cancelled: paymentId=${payment.id}, orderId=${payment.orderId}, " +
                    "reason=${command.reason}, previousStatus=$previousStatus"
            }

            // 도메인 이벤트 생성 및 발행 (AFTER_COMMIT)
            val event = paymentEventFactory.createPaymentCancelledEvent(payment, previousStatus, command.reason)
            eventPublisher.publishEvent(event)

            CancelPaymentResult(
                paymentId = payment.id,
                orderId = payment.orderId,
                previousStatus = previousStatus,
                currentStatus = payment.status,
                reason = command.reason,
                cancelledAt = payment.cancelledAt!!,
            )
        }
}
