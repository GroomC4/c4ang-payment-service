package com.groom.payment.presentation.web.dto

import com.groom.payment.application.dto.GetPaymentResult
import com.groom.payment.application.dto.OrderInfo
import com.groom.payment.application.dto.PaymentHistoryItem
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 상세 조회 Response
 *
 * PAYMENT_WAIT 상태에서는 금액 정보가 null일 수 있습니다.
 */
@Schema(description = "결제 상세 조회 응답")
data class GetPaymentResponse(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val paymentId: UUID,
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "총 주문 금액", example = "50000.00", nullable = true)
    val totalAmount: BigDecimal?,
    @Schema(description = "실제 결제 금액", example = "48000.00", nullable = true)
    val paymentAmount: BigDecimal?,
    @Schema(description = "할인 금액", example = "5000.00", nullable = true)
    val discountAmount: BigDecimal?,
    @Schema(description = "배송비", example = "3000.00", nullable = true)
    val deliveryFee: BigDecimal?,
    @Schema(description = "결제 수단", example = "CARD", nullable = true)
    val method: String?,
    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    val status: String,
    @Schema(description = "PG사 거래 ID", example = "PG-TXN-123456", nullable = true)
    val pgTransactionId: String?,
    @Schema(description = "PG사 승인 번호", example = "APPROVAL-12345678", nullable = true)
    val pgApprovalNumber: String?,
    @Schema(description = "결제 완료 시각", example = "2025-10-29T14:35:00", nullable = true)
    val completedAt: LocalDateTime?,
    @Schema(description = "결제 생성 시각", example = "2025-10-29T14:30:00", nullable = true)
    val createdAt: LocalDateTime?,
    @Schema(description = "결제 수정 시각", example = "2025-10-29T14:35:00", nullable = true)
    val updatedAt: LocalDateTime?,
    @Schema(description = "주문 정보", nullable = true)
    val orderInfo: OrderInfoResponse?,
    @Schema(description = "결제 이력 목록")
    val history: List<PaymentHistoryItemResponse>,
) {
    companion object {
        fun from(result: GetPaymentResult): GetPaymentResponse =
            GetPaymentResponse(
                paymentId = result.paymentId,
                orderId = result.orderId,
                totalAmount = result.totalAmount,
                paymentAmount = result.paymentAmount,
                discountAmount = result.discountAmount,
                deliveryFee = result.deliveryFee,
                method = result.method?.name,
                status = result.status.name,
                pgTransactionId = result.pgTransactionId,
                pgApprovalNumber = result.pgApprovalNumber,
                completedAt = result.completedAt,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
                orderInfo = result.orderInfo?.let { OrderInfoResponse.from(it) },
                history = result.history.map { PaymentHistoryItemResponse.from(it) },
            )
    }
}

/**
 * 주문 정보 Response
 */
@Schema(description = "주문 정보")
data class OrderInfoResponse(
    @Schema(description = "주문 ID", example = "660e8400-e29b-41d4-a716-446655440001")
    val orderId: UUID,
    @Schema(description = "주문 번호", example = "ORD-20251029-001")
    val orderNumber: String,
    @Schema(description = "주문 상품 목록")
    val items: List<OrderItemInfoResponse>,
) {
    companion object {
        fun from(orderInfo: OrderInfo): OrderInfoResponse =
            OrderInfoResponse(
                orderId = orderInfo.orderId,
                orderNumber = orderInfo.orderNumber,
                items = orderInfo.items.map { OrderItemInfoResponse.from(it) },
            )
    }
}

/**
 * 주문 상품 정보 Response
 */
@Schema(description = "주문 상품 정보")
data class OrderItemInfoResponse(
    @Schema(description = "상품 ID", example = "770e8400-e29b-41d4-a716-446655440002")
    val productId: UUID,
    @Schema(description = "상품명", example = "무선 이어폰")
    val productName: String,
    @Schema(description = "수량", example = "2")
    val quantity: Int,
    @Schema(description = "가격", example = "25000.00")
    val price: BigDecimal,
) {
    companion object {
        fun from(item: com.groom.payment.application.dto.OrderItemInfo): OrderItemInfoResponse =
            OrderItemInfoResponse(
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                price = item.price,
            )
    }
}

/**
 * 결제 이력 Response
 */
@Schema(description = "결제 이력 항목")
data class PaymentHistoryItemResponse(
    @Schema(description = "이벤트 유형", example = "PAYMENT_COMPLETED")
    val eventType: String,
    @Schema(description = "변경 내용 요약", example = "결제가 완료되었습니다.")
    val changeSummary: String,
    @Schema(description = "기록 시각", example = "2025-10-29T14:35:00")
    val recordedAt: LocalDateTime,
) {
    companion object {
        fun from(item: PaymentHistoryItem): PaymentHistoryItemResponse =
            PaymentHistoryItemResponse(
                eventType = item.eventType.name,
                changeSummary = item.changeSummary,
                recordedAt = item.recordedAt,
            )
    }
}
