package com.groom.payment.presentation.web.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 요청 Response
 */
@Schema(description = "결제 요청 응답")
data class RequestPaymentResponse(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val paymentId: UUID,
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "결제 상태", example = "PAYMENT_REQUEST")
    val status: String,
    @Schema(description = "PG사 거래 ID", example = "PG-TXN-123456")
    val pgTransactionId: String,
    @Schema(description = "PG 결제 페이지 URL", example = "https://pg.example.com/payment/123456")
    val pgUrl: String,
    @Schema(description = "결제 요청 시각", example = "2025-10-29T14:30:00")
    val requestedAt: LocalDateTime,
)
