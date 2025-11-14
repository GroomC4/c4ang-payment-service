package com.groom.payment.application.service

import com.groom.payment.application.dto.MarkPaymentFailedCommand
import com.groom.payment.application.dto.MarkPaymentFailedResult
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 결제 실패 처리 Application 서비스
 *
 * PG사에서 결제 실패 응답 수신 시 사용
 *
 * 동시성 제어:
 * - PaymentLockManager: 분산 락 관리
 *
 * 트랜잭션:
 * - @Transactional: JPA dirty checking으로 자동 저장
 */
@Service
class MarkPaymentFailedService(
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentLockManager: PaymentLockManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentEventFactory: PaymentEventFactory,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: MarkPaymentFailedCommand): MarkPaymentFailedResult =
        paymentLockManager.executeWithLock(command.paymentId) {
            // Payment 조회
            val payment =
                loadPaymentPort.loadById(command.paymentId)
                    ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

            // 결제 실패 (도메인 메서드)
            payment.markFailed(command.reason)

            // JPA dirty checking으로 자동 저장
            logger.info {
                "Payment failed: paymentId=${payment.id}, orderId=${payment.orderId}, " +
                    "reason=${command.reason}"
            }

            // 도메인 이벤트 생성 및 발행 (AFTER_COMMIT)
            val event = paymentEventFactory.createPaymentFailedEvent(payment, command.reason)
            eventPublisher.publishEvent(event)

            MarkPaymentFailedResult(
                paymentId = payment.id,
                orderId = payment.orderId,
                status = payment.status,
                reason = command.reason,
                occurredAt = LocalDateTime.now(),
            )
        }
}
