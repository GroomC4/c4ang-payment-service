package com.groom.payment.application.dto

import java.util.UUID

/**
 * 결제 취소 Command
 *
 * 사용자 또는 시스템이 결제를 취소하는 커맨드
 *
 * 사용 시점:
 * - 주문 타임아웃
 * - 사용자 취소 요청
 * - 시스템 취소 (재고 부족 등)
 */
data class CancelPaymentCommand(
    val paymentId: UUID,
    val reason: String,
)
