package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 목록 조회 Result
 */
data class ListPaymentsResult(
    val payments: List<PaymentSummary>,
    val pagination: PaginationInfo,
)

/**
 * Payment 요약 정보
 *
 * PAYMENT_WAIT 상태에서는 금액 정보가 null일 수 있습니다.
 */
data class PaymentSummary(
    val paymentId: UUID,
    val orderId: UUID,
    val totalAmount: BigDecimal?,
    val paymentAmount: BigDecimal?,
    val method: PaymentMethod?,
    val status: PaymentStatus,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
)

/**
 * 페이지네이션 정보
 */
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Long,
)
