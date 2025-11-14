package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 환불 요청 Result
 */
data class RequestPaymentRefundResult(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val refundAmount: BigDecimal,
    val reason: String,
    val occurredAt: LocalDateTime,
)
