package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 요청 Result
 */
data class RequestPaymentResult(
    val paymentId: UUID,
    val orderId: UUID,
    val status: PaymentStatus,
    val pgTransactionId: String,
    val pgUrl: String, // 사용자가 접속할 PG 결제 페이지 URL
    val occurredAt: LocalDateTime,
)
