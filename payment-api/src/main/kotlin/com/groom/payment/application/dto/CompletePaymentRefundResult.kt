package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 환불 완료 Result
 */
data class CompletePaymentRefundResult(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val refundTransactionId: String,
    val refundedAt: LocalDateTime,
    val alreadyProcessed: Boolean = false, // 이미 처리된 요청인지 여부
)
