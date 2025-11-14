package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentEventType
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 상세 조회 Result
 *
 * PAYMENT_WAIT 상태에서는 금액 정보가 null일 수 있습니다.
 * 사용자가 결제 요청 시 금액 정보가 설정됩니다.
 */
data class GetPaymentResult(
    val paymentId: UUID,
    val orderId: UUID,
    val totalAmount: BigDecimal?,
    val paymentAmount: BigDecimal?,
    val discountAmount: BigDecimal?,
    val deliveryFee: BigDecimal?,
    val method: PaymentMethod?,
    val status: PaymentStatus,
    val pgTransactionId: String?,
    val pgApprovalNumber: String?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val orderInfo: OrderInfo?,
    val history: List<PaymentHistoryItem>,
)

/**
 * Order 정보 (상품 정보 포함)
 */
data class OrderInfo(
    val orderId: UUID,
    val orderNumber: String,
    val items: List<OrderItemInfo>,
)

/**
 * Order Item 정보
 */
data class OrderItemInfo(
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal,
)

/**
 * Payment History 항목
 */
data class PaymentHistoryItem(
    val eventType: PaymentEventType,
    val changeSummary: String,
    val recordedAt: LocalDateTime,
)
