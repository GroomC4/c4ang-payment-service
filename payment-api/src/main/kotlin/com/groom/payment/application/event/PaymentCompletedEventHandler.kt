package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentCompletedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 결제 완료 이벤트 핸들러
 *
 * PaymentCompletedEvent 수신 시:
 * - Order 상태를 PAYMENT_COMPLETED로 변경
 * - Order에 paymentId 기록
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 */
@Component("paymentDomainPaymentCompletedEventHandler")
class PaymentCompletedEventHandler {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentCompletedEvent) {
        logger.info {
            "PaymentCompletedEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, pgApprovalNumber=${event.pgApprovalNumber}"
        }
        TODO("Order 서비스 연동 필요")
    }
}
