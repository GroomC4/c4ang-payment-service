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
