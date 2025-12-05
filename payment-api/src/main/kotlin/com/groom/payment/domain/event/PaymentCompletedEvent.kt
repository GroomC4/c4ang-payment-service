package com.groom.payment.domain.event

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 완료 이벤트
 *
 * PG사 콜백으로 결제가 성공적으로 완료되었을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.complete() 호출 시
 * 처리:
 * - PaymentCompletedEventHandler: Kafka payment.completed 이벤트 발행
 * - Product Service가 소비하여 재고 확정
 */
data class PaymentCompletedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val userId: UUID,
    val totalAmount: BigDecimal,
    val paymentMethod: String,
    val pgApprovalNumber: String,
    val completedAt: LocalDateTime,
    val occurredAt: LocalDateTime,
)
