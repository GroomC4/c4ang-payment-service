package com.groom.payment.application.dto

import java.util.UUID

/**
 * 결제 실패 처리 Command
 *
 * PG사에서 결제 실패 응답 수신 시 사용하는 커맨드
 *
 * 사용 시점: PG사에서 결제 실패 콜백 수신 시
 */
data class MarkPaymentFailedCommand(
    val paymentId: UUID,
    val reason: String,
)
