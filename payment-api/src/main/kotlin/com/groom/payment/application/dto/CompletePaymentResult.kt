package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 완료 Result
 */
data class CompletePaymentResult(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val pgApprovalNumber: String,
    val completedAt: LocalDateTime,
    val alreadyProcessed: Boolean = false, // 이미 처리된 요청인지 여부
)
