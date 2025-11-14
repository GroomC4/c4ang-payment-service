package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 실패 처리 Result
 */
data class MarkPaymentFailedResult(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val reason: String,
    val occurredAt: LocalDateTime,
)
