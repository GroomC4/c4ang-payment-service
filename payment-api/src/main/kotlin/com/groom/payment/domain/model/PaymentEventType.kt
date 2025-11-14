package com.groom.payment.domain.model

/**
 * Payment 이벤트 타입
 *
 * PaymentHistory 테이블에 기록되는 이벤트 종류
 */
enum class PaymentEventType {
    /**
     * 결제 요청
     */
    PAYMENT_REQUESTED,

    /**
     * 결제 완료
     */
    PAYMENT_COMPLETED,

    /**
     * 결제 실패
     */
    PAYMENT_FAILED,

    /**
     * 결제 취소
     */
    PAYMENT_CANCELLED,

    /**
     * 환불 요청
     */
    REFUND_REQUESTED,

    /**
     * 환불 완료
     */
    REFUND_COMPLETED,
}
