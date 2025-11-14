package com.groom.payment.presentation.web.dto

import com.groom.payment.domain.model.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 완료 Response
 */
@Schema(description = "결제 완료 응답")
data class CompletePaymentResponse(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val paymentId: UUID,
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    val status: PaymentStatus,
    @Schema(description = "PG사 승인 번호", example = "APPROVAL-12345678")
    val pgApprovalNumber: String,
    @Schema(description = "결제 완료 시각", example = "2025-10-29T14:35:00")
    val completedAt: LocalDateTime,
    @Schema(description = "이미 처리된 요청 여부 (멱등성)", example = "false")
    val alreadyProcessed: Boolean = false,
)
