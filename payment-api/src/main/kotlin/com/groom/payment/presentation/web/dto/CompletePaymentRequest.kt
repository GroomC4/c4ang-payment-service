package com.groom.payment.presentation.web.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 결제 완료 Request
 *
 * PG 콜백 API: PG사가 호출
 */
@Schema(description = "결제 완료 요청 (PG 콜백)")
data class CompletePaymentRequest(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    val paymentId: UUID,
    @Schema(description = "PG사 승인 번호", example = "APPROVAL-12345678", required = true)
    val pgApprovalNumber: String,
    @Schema(description = "멱등성 키 (PG 거래 ID)", example = "PG-TXN-123456", required = true)
    val idempotencyKey: String,
)
