package com.groom.payment.application.event

import com.groom.payment.domain.event.PaymentRefundRequestedEvent
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.PaymentGatewayPort
import com.groom.payment.domain.service.PaymentEventFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 환불 요청 이벤트 핸들러
 *
 * PaymentRefundRequestedEvent 수신 시:
 * 1. PG사에 환불 요청 API 호출
 * 2. 환불 성공 시 Payment 상태 업데이트 및 PaymentRefundCompletedEvent 발행
 *
 * 발생 시점:
 * - 고객이 상품 반품 후 환불을 요청한 경우
 *
 * 트랜잭션:
 * - AFTER_COMMIT: Payment 트랜잭션이 커밋된 후 실행
 * - REQUIRES_NEW: 독립 트랜잭션 (Payment와 분리)
 */
@Component
class PaymentRefundRequestedEventHandler(
    private val paymentGatewayPort: PaymentGatewayPort,
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentEventFactory: PaymentEventFactory,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentRefundRequestedEvent) {
        logger.info {
            "PaymentRefundRequestedEvent received: orderId=${event.orderId}, " +
                "paymentId=${event.paymentId}, refundAmount=${event.refundAmount}, " +
                "reason=${event.reason}"
        }

        val payment = loadPaymentPort.loadById(event.paymentId)
            ?: throw IllegalArgumentException("Payment not found: ${event.paymentId}")

        // PG사 환불 요청
        val refundResult = paymentGatewayPort.requestRefund(
            pgTransactionId = payment.pgTransactionId
                ?: throw IllegalStateException("PG transaction ID is null for payment: ${event.paymentId}"),
            amount = event.refundAmount,
        )

        if (refundResult.success) {
            logger.info {
                "PG 환불 성공: paymentId=${event.paymentId}, refundId=${refundResult.refundId}"
            }

            // Payment 상태 업데이트
            payment.completeRefund(refundResult.refundId)

            // 환불 완료 이벤트 발행
            val refundCompletedEvent = paymentEventFactory.createPaymentRefundCompletedEvent(payment)
            applicationEventPublisher.publishEvent(refundCompletedEvent)

            logger.info {
                "환불 완료 처리: paymentId=${event.paymentId}, status=${payment.status}"
            }
        } else {
            logger.error {
                "PG 환불 실패: paymentId=${event.paymentId}, refundId=${refundResult.refundId}"
            }
            // TODO: 환불 실패 처리 로직 (재시도, 알림 등)
        }
    }
}
