package com.groom.payment.domain.port

import java.math.BigDecimal

/**
 * Payment 이벤트 발행 Port
 * - 결제 관련 이벤트를 외부 시스템(Kafka)에 발행하기 위한 추상화
 */
interface PaymentEventPublishPort {
    /**
     * 결제 완료 이벤트 발행
     */
    fun publishPaymentCompleted(
        paymentId: String,
        orderId: String,
        userId: String,
        totalAmount: BigDecimal,
        paymentMethod: String,
        pgApprovalNumber: String,
    )

    /**
     * 결제 실패 이벤트 발행
     */
    fun publishPaymentFailed(
        paymentId: String,
        orderId: String,
        userId: String,
        failureReason: String,
    )

    /**
     * 결제 취소 이벤트 발행
     */
    fun publishPaymentCancelled(
        paymentId: String,
        orderId: String,
        userId: String,
        cancellationReason: String,
    )
}
