package com.groom.payment.adapter.outbound.client.dto

import com.groom.payment.application.dto.OrderItemInfo
import java.math.BigDecimal
import java.util.UUID

/**
 * Order Service 주문 조회 API 상품 응답
 */
data class OrderServiceGetOrderItemResponse(
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
) {
    fun toOrderItemInfo(): OrderItemInfo =
        OrderItemInfo(
            productId = productId,
            productName = productName,
            quantity = quantity,
            price = unitPrice,
        )
}

