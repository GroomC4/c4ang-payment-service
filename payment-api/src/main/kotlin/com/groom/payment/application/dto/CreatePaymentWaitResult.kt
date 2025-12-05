package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 대기 생성 결과
 *
 * @param paymentId 생성된 Payment ID
 * @param orderId 주문 ID
 * @param userId 사용자 ID
 * @param status 결제 상태 (PAYMENT_WAIT)
 * @param createdAt 생성 시각
 */
data class CreatePaymentWaitResult(
    val paymentId: UUID,
    val orderId: UUID,
    val userId: UUID,
    val status: PaymentStatus,
    val createdAt: LocalDateTime,
)
