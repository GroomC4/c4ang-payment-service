package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 취소 Result
 */
data class CancelPaymentResult(
    val paymentId: UUID,
    val orderId: UUID,
    val previousStatus: PaymentStatus,
    val currentStatus: PaymentStatus,
    val reason: String,
    val cancelledAt: LocalDateTime,
)
