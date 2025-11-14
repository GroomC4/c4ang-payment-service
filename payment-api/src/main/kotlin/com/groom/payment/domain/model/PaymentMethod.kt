package com.groom.payment.domain.model

/**
 * Payment 결제 수단
 */
enum class PaymentMethod {
    /**
     * 카드 결제
     */
    CARD,

    /**
     * 토스페이
     */
    TOSS_PAY,
}
