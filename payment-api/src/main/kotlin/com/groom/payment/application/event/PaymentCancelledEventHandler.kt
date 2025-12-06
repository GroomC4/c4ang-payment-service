package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentCancelledEvent
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.PaymentEventPublishPort
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
 * - Kafka payment.cancelled 이벤트 발행
 * - Order Service가 소비하여 주문 취소 처리
 *
 * 발생 시점:
 * - 사용자가 결제를 취소한 경우
 * - 시스템이 결제를 취소한 경우 (타임아웃 등)
 * - SAGA 보상 트랜잭션 (재고 확정 실패 등)
 *
 * PG 취소 처리:
 * - 결제 완료 상태(PAYMENT_COMPLETED)에서 취소: PG 취소 API 호출 필요
 * - 결제 대기/요청 상태에서 취소: PG 호출 불필요
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 *
 * 참고: c4ang-contract-hub/docs/event-flows/payment-processing/stock-confirmation-failed.md
 */
@Component
class PaymentCancelledEventHandler(
    private val paymentEventPublishPort: PaymentEventPublishPort,
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentCancelledEvent) {
        logger.info {
            "PaymentCancelledEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, reason=${event.reason}, " +
                "previousStatus=${event.previousStatus}"
        }

        // PG 취소가 필요한 경우 (결제 완료 상태에서 취소)
        // 현재 Fake PG 구현으로 실제 취소 호출은 생략
        if (event.previousStatus == PaymentStatus.PAYMENT_COMPLETED) {
            logger.info {
                "PG 취소 필요 (Fake PG 사용 중): paymentId=${event.paymentId}, previousStatus=${event.previousStatus}"
            }
        }

        // Kafka payment.cancelled 이벤트 발행
        paymentEventPublishPort.publishPaymentCancelled(
            paymentId = event.paymentId.toString(),
            orderId = event.orderId.toString(),
            userId = event.userId.toString(),
            cancellationReason = event.reason,
        )

        logger.info {
            "PaymentCancelled Kafka 이벤트 발행 완료: orderId=${event.orderId}, paymentId=${event.paymentId}"
        }
    }
}
