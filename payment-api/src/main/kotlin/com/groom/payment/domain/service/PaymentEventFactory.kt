package com.groom.payment.domain.service

import com.groom.payment.domain.event.PaymentCancelledEvent
import com.groom.payment.domain.event.PaymentCompletedEvent
import com.groom.payment.domain.event.PaymentFailedEvent
import com.groom.payment.domain.event.PaymentRefundCompletedEvent
import com.groom.payment.domain.event.PaymentRefundRequestedEvent
import com.groom.payment.domain.event.PaymentRequestedEvent
import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentStatus
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Payment 도메인 이벤트 팩토리
 *
 * Payment 엔티티의 상태 변경 후 적절한 도메인 이벤트를 생성합니다.
 *
 * 책임 분리:
 * - Payment 엔티티: 상태 변경 로직
 * - PaymentEventFactory: 도메인 이벤트 생성
 *
 * 이를 통해 단일 책임 원칙(SRP)을 준수합니다.
 */
@Component
class PaymentEventFactory {
    /**
     * 결제 요청 이벤트 생성
     */
    fun createPaymentRequestedEvent(payment: Payment): PaymentRequestedEvent {
        require(payment.status == PaymentStatus.PAYMENT_REQUEST) {
            "Payment status must be PAYMENT_REQUEST"
        }
        require(payment.pgTransactionId != null) {
            "PG transaction ID must not be null"
        }

        return PaymentRequestedEvent(
            paymentId = payment.id,
            orderId = payment.orderId,
            pgTransactionId = payment.pgTransactionId!!,
            occurredAt = LocalDateTime.now(),
        )
    }

    /**
     * 결제 완료 이벤트 생성
     */
    fun createPaymentCompletedEvent(payment: Payment): PaymentCompletedEvent {
        require(payment.status == PaymentStatus.PAYMENT_COMPLETED) {
            "Payment status must be PAYMENT_COMPLETED"
        }
        require(payment.pgApprovalNumber != null) {
            "PG approval number must not be null"
        }
        require(payment.completedAt != null) {
            "Completed at must not be null"
        }

        return PaymentCompletedEvent(
            paymentId = payment.id,
            orderId = payment.orderId,
            pgApprovalNumber = payment.pgApprovalNumber!!,
            completedAt = payment.completedAt!!,
            occurredAt = LocalDateTime.now(),
        )
    }

    /**
     * 결제 취소 이벤트 생성
     */
    fun createPaymentCancelledEvent(
        payment: Payment,
        previousStatus: PaymentStatus,
        reason: String,
    ): PaymentCancelledEvent {
        require(payment.status == PaymentStatus.PAYMENT_CANCELLED) {
            "Payment status must be PAYMENT_CANCELLED"
        }

        return PaymentCancelledEvent(
            paymentId = payment.id,
            orderId = payment.orderId,
            previousStatus = previousStatus,
            reason = reason,
            occurredAt = LocalDateTime.now(),
        )
    }

    /**
     * 결제 실패 이벤트 생성
     */
    fun createPaymentFailedEvent(
        payment: Payment,
        reason: String,
    ): PaymentFailedEvent {
        require(payment.status == PaymentStatus.PAYMENT_FAILED) {
            "Payment status must be PAYMENT_FAILED"
        }

        return PaymentFailedEvent(
            paymentId = payment.id,
            orderId = payment.orderId,
            reason = reason,
            occurredAt = LocalDateTime.now(),
        )
    }

    /**
     * 환불 요청 이벤트 생성
     */
    fun createPaymentRefundRequestedEvent(
        payment: Payment,
        refundAmount: BigDecimal,
        reason: String,
    ): PaymentRefundRequestedEvent {
        require(payment.status == PaymentStatus.REFUND_REQUESTED) {
            "Payment status must be REFUND_REQUESTED"
        }

        return PaymentRefundRequestedEvent(
            paymentId = payment.id,
            orderId = payment.orderId,
            refundAmount = refundAmount,
            reason = reason,
            occurredAt = LocalDateTime.now(),
        )
    }

    /**
     * 환불 완료 이벤트 생성
     */
    fun createPaymentRefundCompletedEvent(payment: Payment): PaymentRefundCompletedEvent {
        require(payment.status == PaymentStatus.REFUND_COMPLETED) {
            "Payment status must be REFUND_COMPLETED"
        }
        require(payment.refundTransactionId != null) {
            "Refund transaction ID must not be null"
        }
        require(payment.refundedAt != null) {
            "Refunded at must not be null"
        }

        return PaymentRefundCompletedEvent(
            paymentId = payment.id,
            orderId = payment.orderId,
            refundTransactionId = payment.refundTransactionId!!,
            refundedAt = payment.refundedAt!!,
            occurredAt = LocalDateTime.now(),
        )
    }
}
