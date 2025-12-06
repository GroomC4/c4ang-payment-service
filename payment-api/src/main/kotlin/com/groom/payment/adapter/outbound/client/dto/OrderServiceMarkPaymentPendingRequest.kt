package com.groom.payment.adapter.outbound.client.dto

import java.util.UUID

/**
 * Order Service 결제 대기 상태 변경 API 요청
 */
data class OrderServiceMarkPaymentPendingRequest(
    val paymentId: UUID,
)

