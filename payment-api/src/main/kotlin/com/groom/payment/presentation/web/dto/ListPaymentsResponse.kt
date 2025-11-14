package com.groom.payment.presentation.web.dto

import com.groom.payment.application.dto.ListPaymentsResult
import com.groom.payment.application.dto.PaginationInfo
import com.groom.payment.application.dto.PaymentSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 목록 조회 Response
 */
@Schema(description = "결제 목록 조회 응답")
data class ListPaymentsResponse(
    @Schema(description = "결제 목록")
    val payments: List<PaymentSummaryResponse>,
    @Schema(description = "페이지네이션 정보")
    val pagination: PaginationInfoResponse,
) {
    companion object {
        fun from(result: ListPaymentsResult): ListPaymentsResponse =
            ListPaymentsResponse(
                payments = result.payments.map { PaymentSummaryResponse.from(it) },
                pagination = PaginationInfoResponse.from(result.pagination),
            )
    }
}

/**
 * 결제 요약 정보 Response
 *
 * PAYMENT_WAIT 상태에서는 금액 정보가 null일 수 있습니다.
 */
@Schema(description = "결제 요약 정보")
data class PaymentSummaryResponse(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val paymentId: UUID,
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "총 주문 금액", example = "50000.00", nullable = true)
    val totalAmount: BigDecimal?,
    @Schema(description = "실제 결제 금액", example = "48000.00", nullable = true)
    val paymentAmount: BigDecimal?,
    @Schema(description = "결제 수단", example = "CARD", nullable = true)
    val method: String?,
    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    val status: String,
    @Schema(description = "결제 완료 시각", example = "2025-10-29T14:35:00", nullable = true)
    val completedAt: LocalDateTime?,
    @Schema(description = "결제 생성 시각", example = "2025-10-29T14:30:00", nullable = true)
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(summary: PaymentSummary): PaymentSummaryResponse =
            PaymentSummaryResponse(
                paymentId = summary.paymentId,
                orderId = summary.orderId,
                totalAmount = summary.totalAmount,
                paymentAmount = summary.paymentAmount,
                method = summary.method?.name,
                status = summary.status.name,
                completedAt = summary.completedAt,
                createdAt = summary.createdAt,
            )
    }
}

/**
 * 페이지네이션 정보 Response
 */
@Schema(description = "페이지네이션 정보")
data class PaginationInfoResponse(
    @Schema(description = "현재 페이지 번호", example = "1")
    val page: Int,
    @Schema(description = "페이지당 개수", example = "20")
    val limit: Int,
    @Schema(description = "전체 결제 개수", example = "100")
    val total: Long,
) {
    companion object {
        fun from(pagination: PaginationInfo): PaginationInfoResponse =
            PaginationInfoResponse(
                page = pagination.page,
                limit = pagination.limit,
                total = pagination.total,
            )
    }
}
