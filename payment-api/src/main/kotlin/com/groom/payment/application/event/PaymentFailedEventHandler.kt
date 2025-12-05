package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentFailedEvent
import com.groom.payment.domain.port.PaymentEventPublishPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 결제 실패 이벤트 핸들러
 *
 * PaymentFailedEvent 수신 시:
 * - Kafka payment.failed 이벤트 발행
 * - Order Service가 소비하여 주문 취소 처리
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 *
 * 참고: c4ang-contract-hub/docs/event-flows/payment-processing/payment-failed.md
 */
@Component
class PaymentFailedEventHandler(
    private val paymentEventPublishPort: PaymentEventPublishPort,
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentFailedEvent) {
        logger.info {
            "PaymentFailedEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, reason=${event.reason}"
        }

        paymentEventPublishPort.publishPaymentFailed(
            paymentId = event.paymentId.toString(),
            orderId = event.orderId.toString(),
            userId = event.userId.toString(),
            failureReason = event.reason,
        )

        logger.info {
            "PaymentFailed Kafka 이벤트 발행 완료: orderId=${event.orderId}, paymentId=${event.paymentId}"
        }
    }
}
