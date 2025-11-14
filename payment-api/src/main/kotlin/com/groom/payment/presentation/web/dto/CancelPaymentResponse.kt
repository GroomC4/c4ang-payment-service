package com.groom.payment.presentation.web.dto

import com.groom.payment.domain.model.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 취소 Response
 */
@Schema(description = "결제 취소 응답")
data class CancelPaymentResponse(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val paymentId: UUID,
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "이전 결제 상태", example = "PAYMENT_REQUEST")
    val previousStatus: PaymentStatus,
    @Schema(description = "현재 결제 상태", example = "PAYMENT_CANCELLED")
    val currentStatus: PaymentStatus,
    @Schema(description = "취소 사유", example = "사용자 요청")
    val reason: String,
    @Schema(description = "취소 시각", example = "2025-10-29T14:40:00")
    val cancelledAt: LocalDateTime,
)
