package com.groom.payment.domain.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 완료 이벤트
 *
 * PG사 콜백으로 결제가 성공적으로 완료되었을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.complete() 호출 시
 * 처리:
 * - OrderPaymentCompletedEventHandler: 주문 상태를 PAYMENT_COMPLETED로 변경
 * - 외부 감사 로그 시스템으로 전송
 */
data class PaymentCompletedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val pgApprovalNumber: String,
    val completedAt: LocalDateTime,
    val occurredAt: LocalDateTime,
)
