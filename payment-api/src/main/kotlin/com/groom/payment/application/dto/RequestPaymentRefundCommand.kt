package com.groom.payment.application.dto

import java.math.BigDecimal
import java.util.UUID

/**
 * 환불 요청 Command
 *
 * 고객이 환불을 요청하는 커맨드
 *
 * 사용 시점: 상품 반품 후 환불 요청 시
 */
data class RequestPaymentRefundCommand(
    val paymentId: UUID,
    val refundAmount: BigDecimal,
    val reason: String,
)
