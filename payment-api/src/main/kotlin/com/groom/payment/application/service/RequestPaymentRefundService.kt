package com.groom.payment.application.service

import com.groom.payment.application.dto.RequestPaymentRefundCommand
import com.groom.payment.application.dto.RequestPaymentRefundResult
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 환불 요청 Application 서비스
 *
 * 고객이 환불을 요청
 *
 * 사용 시점:
 * - 상품 반품 후 환불 요청
 *
 * 동시성 제어:
 * - PaymentLockManager: 분산 락 관리
 *
 * 트랜잭션:
 * - @Transactional: JPA dirty checking으로 자동 저장
 */
@Service
class RequestPaymentRefundService(
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentLockManager: PaymentLockManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentEventFactory: PaymentEventFactory,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: RequestPaymentRefundCommand): RequestPaymentRefundResult =
        paymentLockManager.executeWithLock(command.paymentId) {
            // Payment 조회
            val payment =
                loadPaymentPort.loadById(command.paymentId)
                    ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

            // 환불 요청 (도메인 메서드)
            payment.requestRefund(command.reason, command.refundAmount)

            // JPA dirty checking으로 자동 저장
            logger.info {
                "Payment refund requested: paymentId=${payment.id}, orderId=${payment.orderId}, " +
                    "refundAmount=${command.refundAmount}, reason=${command.reason}"
            }

            // 도메인 이벤트 생성 및 발행 (AFTER_COMMIT)
            val event = paymentEventFactory.createPaymentRefundRequestedEvent(payment, command.refundAmount, command.reason)
            eventPublisher.publishEvent(event)

            RequestPaymentRefundResult(
                paymentId = payment.id,
                orderId = payment.orderId,
                status = payment.status,
                refundAmount = command.refundAmount,
                reason = command.reason,
                occurredAt = LocalDateTime.now(),
            )
        }
}
