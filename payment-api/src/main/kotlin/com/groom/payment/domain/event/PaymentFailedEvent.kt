package com.groom.payment.domain.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 실패 이벤트
 *
 * PG사에서 결제 실패 응답을 받았을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.markFailed() 호출 시
 * 처리:
 * - PaymentFailedEventHandler: Kafka payment.failed 이벤트 발행
 * - Order Service가 소비하여 주문 취소
 */
data class PaymentFailedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val userId: UUID,
    val reason: String,
    val occurredAt: LocalDateTime,
)
