package com.groom.payment.application.dto

import java.math.BigDecimal
import java.util.UUID

/**
 * 결제 대기 생성 Command
 *
 * OrderConfirmed 이벤트 수신 시 Payment 엔티티를 PAYMENT_WAIT 상태로 생성합니다.
 *
 * SAGA 플로우:
 * - Order Service에서 stock.reserved 이벤트를 받아 주문을 확정하고 order.confirmed 발행
 * - Payment Service에서 order.confirmed 이벤트를 받아 결제 대기 상태 생성
 *
 * @param eventId 이벤트 ID (멱등성 보장용)
 * @param orderId 주문 ID
 * @param userId 사용자 ID
 * @param totalAmount 주문 총액
 */
data class CreatePaymentWaitCommand(
    val eventId: String,
    val orderId: UUID,
    val userId: UUID,
    val totalAmount: BigDecimal,
)
