package com.groom.payment.domain.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 환불 완료 이벤트
 *
 * PG사에서 환불이 성공적으로 처리되었을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.completeRefund() 호출 시
 * 처리:
 * - OrderRefundCompletedEventHandler: 주문 상태를 REFUND_COMPLETED로 변경
 * - 재고 복구 (환불 완료 시 재고 증가)
 * - 외부 감사 로그 시스템으로 전송
 */
data class PaymentRefundCompletedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val refundTransactionId: String,
    val refundedAt: LocalDateTime,
    val occurredAt: LocalDateTime,
)
