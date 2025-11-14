package com.groom.payment.domain.event

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 환불 요청 이벤트
 *
 * 고객이 환불을 요청했을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.requestRefund() 호출 시
 * 처리:
 * - PG사에 환불 API 호출 (비동기)
 * - OrderRefundRequestedEventHandler: 주문 상태를 REFUND_REQUESTED로 변경
 * - 외부 감사 로그 시스템으로 전송
 */
data class PaymentRefundRequestedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val refundAmount: BigDecimal,
    val reason: String,
    val occurredAt: LocalDateTime,
)
