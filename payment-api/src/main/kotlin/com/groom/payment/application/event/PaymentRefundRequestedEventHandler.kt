package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentRefundRequestedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 환불 요청 이벤트 핸들러
 *
 * PaymentRefundRequestedEvent 수신 시:
 * - Order 상태를 REFUND_PROCESSING으로 변경
 *
 * 발생 시점:
 * - 고객이 상품 반품 후 환불을 요청한 경우
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 */
@Component
class PaymentRefundRequestedEventHandler {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentRefundRequestedEvent) {
        logger.info {
            "PaymentRefundRequestedEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, refundAmount=${event.refundAmount}, " +
                "reason=${event.reason}"
        }
        TODO("Order 서비스 연동 필요")
    }
}
