package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.util.UUID

/**
 * 결제 목록 조회 Query
 */
data class ListPaymentsQuery(
    val userId: UUID,
    val status: PaymentStatus? = null,
    val page: Int = 1,
    val limit: Int = 20,
)
