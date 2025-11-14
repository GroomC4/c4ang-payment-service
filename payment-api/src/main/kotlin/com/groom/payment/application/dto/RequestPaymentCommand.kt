package com.groom.payment.application.dto

import com.groom.payment.domain.model.PaymentMethod
import java.math.BigDecimal
import java.util.UUID

/**
 * 결제 요청 Command
 *
 * 사용자가 결제 수단과 금액 정보를 확인하고 PG사로 결제 요청을 시작하는 커맨드
 *
 * 사용 시점: 사용자가 결제 화면에서 결제 수단을 선택하고 "결제하기" 버튼 클릭 시
 *
 * 금액 정보:
 * - totalAmount: 주문 총액 (상품 금액 합계)
 * - paymentAmount: 실제 결제 금액 (할인 적용 후)
 * - discountAmount: 할인 금액
 * - deliveryFee: 배송비
 */
data class RequestPaymentCommand(
    val paymentId: UUID,
    val paymentMethod: PaymentMethod,
    val totalAmount: BigDecimal,
    val paymentAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val deliveryFee: BigDecimal,
)
