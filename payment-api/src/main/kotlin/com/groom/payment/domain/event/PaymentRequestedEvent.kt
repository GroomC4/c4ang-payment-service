package com.groom.payment.domain.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 요청 이벤트
 *
 * PG사로 결제 요청을 전송했을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.requestPayment() 호출 시
 * 처리: 외부 감사 로그 시스템으로 전송 (선택적)
 */
data class PaymentRequestedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val pgTransactionId: String,
    val occurredAt: LocalDateTime,
)
