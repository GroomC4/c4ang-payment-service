package com.groom.payment.presentation.web.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

/**
 * 환불 요청 Request
 *
 * 사용자 API: 고객이 환불 요청
 */
@Schema(description = "환불 요청")
data class RequestPaymentRefundRequest(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    val paymentId: UUID,
    @Schema(description = "환불 금액", example = "48000.00", required = true)
    val refundAmount: BigDecimal,
    @Schema(description = "환불 사유", example = "상품 불량", required = true)
    val reason: String,
)
