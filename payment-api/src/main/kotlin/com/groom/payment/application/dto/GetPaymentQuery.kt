package com.groom.payment.application.dto

import java.util.UUID

/**
 * 결제 상세 조회 Query
 */
data class GetPaymentQuery(
    val paymentId: UUID,
    val includeHistory: Boolean = false,
)
