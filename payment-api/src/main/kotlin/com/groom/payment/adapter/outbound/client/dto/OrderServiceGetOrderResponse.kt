package com.groom.payment.adapter.outbound.client.dto

import com.groom.payment.application.dto.OrderInfo
import java.math.BigDecimal
import java.util.UUID

/**
 * Order Service 주문 조회 API 응답
 */
data class OrderServiceGetOrderResponse(
    val orderId: UUID,
    val userId: UUID,
    val orderNumber: String,
    val status: String,
    val totalAmount: BigDecimal,
    val items: List<OrderServiceGetOrderItemResponse>,
) {
    fun toOrderInfo(): OrderInfo =
        OrderInfo(
            orderId = orderId,
            orderNumber = orderNumber,
            items = items.map { it.toOrderItemInfo() },
        )
}

