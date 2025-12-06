package com.groom.payment.adapter.outbound.client.dto

/**
 * Order Service 주문 결제 존재 여부 확인 API 응답
 */
data class OrderServiceHasPaymentResponse(
    val hasPayment: Boolean,
)

