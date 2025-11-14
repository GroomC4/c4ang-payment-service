package com.groom.payment.domain.event

import com.groom.payment.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 결제 취소 이벤트
 *
 * 사용자 또는 시스템이 결제를 취소했을 때 발행되는 도메인 이벤트
 *
 * 발행 시점: payment.cancel() 호출 시
 * 처리:
 * - OrderPaymentCancelledEventHandler: 주문 상태를 ORDER_CANCELLED로 변경
 * - 재고 복구 (이미 PAYMENT_WAIT/PAYMENT_REQUEST 상태에서 취소되었다면 재고는 이미 예약 상태)
 * - 외부 감사 로그 시스템으로 전송
 */
data class PaymentCancelledEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val previousStatus: PaymentStatus,
    val reason: String,
    val occurredAt: LocalDateTime,
)
