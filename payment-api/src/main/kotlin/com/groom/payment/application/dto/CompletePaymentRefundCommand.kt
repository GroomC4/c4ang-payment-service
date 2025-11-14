package com.groom.payment.application.dto

import java.util.UUID

/**
 * 환불 완료 Command
 *
 * PG사 콜백으로 환불 완료 처리하는 커맨드
 *
 * 사용 시점: PG사에서 환불 완료 콜백 수신 시
 */
data class CompletePaymentRefundCommand(
    val paymentId: UUID,
    val refundTransactionId: String,
    val idempotencyKey: String, // 멱등성 키
)
