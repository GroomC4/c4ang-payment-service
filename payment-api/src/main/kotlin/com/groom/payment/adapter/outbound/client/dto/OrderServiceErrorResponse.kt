package com.groom.payment.adapter.outbound.client.dto

/**
 * Order Service 에러 응답
 */
data class OrderServiceErrorResponse(
    val code: String,
    val message: String,
)

