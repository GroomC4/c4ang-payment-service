package com.groom.payment.presentation.web.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 결제 취소 Request
 *
 * 내부 API: 사용자 또는 시스템이 호출
 */
@Schema(description = "결제 취소 요청")
data class CancelPaymentRequest(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    val paymentId: UUID,
    @Schema(description = "취소 사유", example = "사용자 요청", required = true)
    val reason: String,
)
