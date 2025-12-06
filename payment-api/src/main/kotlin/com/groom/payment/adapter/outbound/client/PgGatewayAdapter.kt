package com.groom.payment.adapter.outbound.client

import com.groom.payment.domain.port.PaymentGatewayPort
import com.groom.payment.domain.port.PgCancelResult
import com.groom.payment.domain.port.PgRefundResult
import com.groom.payment.domain.port.PgRequestResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * PG(Payment Gateway) Fake Adapter
 *
 * PaymentGatewayPort의 Fake 구현체
 *
 * 실제 PG사 연동 없이 항상 성공 응답을 반환합니다.
 * 테스트 및 개발 환경에서 사용됩니다.
 */
@Component
class PgGatewayAdapter : PaymentGatewayPort {
    private val logger = KotlinLogging.logger {}

    override fun requestPayment(
        paymentId: UUID,
        amount: BigDecimal,
        orderNumber: String,
    ): PgRequestResult {
        logger.info { "PG Fake - 결제 요청: paymentId=$paymentId, amount=$amount, orderNumber=$orderNumber" }

        val pgTransactionId = "PG-${UUID.randomUUID()}"
        val paymentUrl = "https://pg.example.com/pay/$paymentId"
        val expiresAt = LocalDateTime.now().plusMinutes(10)

        logger.info { "PG Fake - 결제 요청 성공: pgTransactionId=$pgTransactionId, expiresAt=$expiresAt" }

        return PgRequestResult(
            pgTransactionId = pgTransactionId,
            paymentUrl = paymentUrl,
            expiresAt = expiresAt,
        )
    }

    override fun cancelPayment(pgTransactionId: String): PgCancelResult {
        logger.info { "PG Fake - 결제 취소 요청: pgTransactionId=$pgTransactionId" }

        val pgCancelId = "CANCEL-${UUID.randomUUID()}"

        logger.info { "PG Fake - 결제 취소 성공: pgTransactionId=$pgTransactionId, pgCancelId=$pgCancelId" }

        return PgCancelResult(
            success = true,
            cancelledAt = LocalDateTime.now(),
            pgCancelId = pgCancelId,
        )
    }

    override fun requestRefund(
        pgTransactionId: String,
        amount: BigDecimal,
    ): PgRefundResult {
        logger.info { "PG Fake - 환불 요청: pgTransactionId=$pgTransactionId, amount=$amount" }

        val refundId = "REFUND-${UUID.randomUUID()}"

        logger.info { "PG Fake - 환불 성공: pgTransactionId=$pgTransactionId, refundId=$refundId, amount=$amount" }

        return PgRefundResult(
            success = true,
            refundId = refundId,
            refundedAmount = amount,
            refundedAt = LocalDateTime.now(),
        )
    }
}
