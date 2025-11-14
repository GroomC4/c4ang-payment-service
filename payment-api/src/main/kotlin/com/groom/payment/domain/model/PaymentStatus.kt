package com.groom.payment.domain.model

/**
 * Payment 상태 값 객체
 */
enum class PaymentStatus {
    /**
     * 결제 대기 (Payment 레코드 생성 직후)
     */
    PAYMENT_WAIT,

    /**
     * 결제 요청 (PG사로 요청 전송)
     */
    PAYMENT_REQUEST,

    /**
     * 결제 완료 (PG사 콜백 수신)
     */
    PAYMENT_COMPLETED,

    /**
     * 결제 실패 (PG사 오류 또는 타임아웃)
     */
    PAYMENT_FAILED,

    /**
     * 결제 취소 (사용자 또는 시스템 취소)
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
