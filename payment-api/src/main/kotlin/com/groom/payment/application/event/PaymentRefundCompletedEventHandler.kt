package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentRefundCompletedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 환불 완료 이벤트 핸들러
 *
 * PaymentRefundCompletedEvent 수신 시:
 * - 환불 완료 로깅
 * - 필요 시 Kafka 이벤트 발행 (payment.refunded)
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 *
 * 참고:
 * - 환불 완료 알림은 Order Service가 처리
 * - 재고 복구는 Product Service가 처리 (필요 시)
 */
@Component
class PaymentRefundCompletedEventHandler {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentRefundCompletedEvent) {
        logger.info {
            "PaymentRefundCompletedEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, refundTransactionId=${event.refundTransactionId}, " +
                "refundedAt=${event.refundedAt}"
        }

        // Kafka payment.refunded 이벤트 발행은 필요 시 추가
        // paymentEventPublishPort.publishPaymentRefunded(...)

        logger.info {
            "환불 완료 처리 종료: paymentId=${event.paymentId}"
        }
    }
}
