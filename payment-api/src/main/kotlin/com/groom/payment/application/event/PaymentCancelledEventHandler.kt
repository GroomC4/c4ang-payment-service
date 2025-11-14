package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentCancelledEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 결제 취소 이벤트 핸들러
 *
 * PaymentCancelledEvent 수신 시:
 * - Order 취소 (ORDER_CANCELLED)
 * - 재고 복구 (필요 시)
 *
 * 발생 시점:
 * - 사용자가 결제를 취소한 경우
 * - 시스템이 결제를 취소한 경우 (타임아웃 등)
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 */
@Component
class PaymentCancelledEventHandler {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentCancelledEvent) {
        logger.info {
            "PaymentCancelledEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, reason=${event.reason}"
        }
        TODO("Order 서비스 연동 필요")
    }
}
