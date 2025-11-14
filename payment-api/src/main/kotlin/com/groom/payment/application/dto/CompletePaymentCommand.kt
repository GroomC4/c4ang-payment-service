package com.groom.payment.application.dto

import java.util.UUID

/**
 * 결제 완료 Command
 *
 * PG사 콜백으로 결제 완료 처리하는 커맨드
 *
 * 사용 시점: PG사에서 결제 성공 콜백 수신 시
 */
data class CompletePaymentCommand(
    val paymentId: UUID,
    val pgApprovalNumber: String,
    val idempotencyKey: String, // 멱등성 키 (PG 거래 ID 등)
)
