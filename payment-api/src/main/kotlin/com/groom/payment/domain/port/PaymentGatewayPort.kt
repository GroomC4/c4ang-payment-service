package com.groom.payment.domain.port

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Payment Gateway Port (Hexagonal Architecture)
 *
 * PG사(결제 게이트웨이) 연동을 위한 Port 인터페이스
 *
 * 구현체:
 * - PgGatewayAdapter: 실제 PG사 API 연동 (Stub으로 시작)
 *
 * 사용처:
 * - RequestPaymentService: PG사로 결제 요청 전송
 */
interface PaymentGatewayPort {
    /**
     * PG사에 결제 요청
     *
     * @param paymentId 결제 ID
     * @param amount 결제 금액
     * @param orderNumber 주문 번호
     * @return PG 결제 요청 결과
     */
    fun requestPayment(
        paymentId: UUID,
        amount: BigDecimal,
        orderNumber: String,
    ): PgRequestResult

    /**
     * PG사에 결제 취소 요청
     *
     * 결제 완료 상태(PAYMENT_COMPLETED)에서 취소하는 경우 호출
     *
     * @param pgTransactionId PG사 거래 ID
     * @return 결제 취소 결과
     */
    fun cancelPayment(pgTransactionId: String): PgCancelResult

    /**
     * PG사에 환불 요청
     *
     * 부분 환불 지원
     *
     * @param pgTransactionId PG사 거래 ID
     * @param amount 환불 금액
     * @return 환불 결과
     */
    fun requestRefund(
        pgTransactionId: String,
        amount: BigDecimal,
    ): PgRefundResult
}

/**
 * PG 결제 요청 결과
 *
 * @property pgTransactionId PG사 거래 ID
 * @property paymentUrl 결제 페이지 URL (사용자가 접속할 URL)
 * @property expiresAt 결제 만료 시간
 */
data class PgRequestResult(
    val pgTransactionId: String,
    val paymentUrl: String,
    val expiresAt: LocalDateTime,
)

/**
 * PG 결제 취소 결과
 *
 * @property success 취소 성공 여부
 * @property cancelledAt 취소 시각
 * @property pgCancelId PG사 취소 거래 ID (선택)
 */
data class PgCancelResult(
    val success: Boolean,
    val cancelledAt: LocalDateTime,
    val pgCancelId: String? = null,
)

/**
 * PG 환불 결과
 *
 * @property success 환불 성공 여부
 * @property refundId 환불 ID
 * @property refundedAmount 환불 금액
 * @property refundedAt 환불 시각
 */
data class PgRefundResult(
    val success: Boolean,
    val refundId: String,
    val refundedAmount: BigDecimal,
    val refundedAt: LocalDateTime,
)
