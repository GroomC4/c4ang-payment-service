package com.groom.payment.presentation.web.dto

import com.groom.payment.domain.model.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 환불 요청 Response
 */
@Schema(description = "환불 요청 응답")
data class RequestPaymentRefundResponse(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val paymentId: UUID,
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "결제 상태", example = "REFUND_REQUESTED")
    val status: PaymentStatus,
    @Schema(description = "환불 금액", example = "48000.00")
    val refundAmount: BigDecimal,
    @Schema(description = "환불 사유", example = "상품 불량")
    val reason: String,
    @Schema(description = "환불 요청 시각", example = "2025-10-29T15:00:00")
    val occurredAt: LocalDateTime,
)
