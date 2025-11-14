package com.groom.payment.infrastructure.adapter

import com.groom.payment.domain.port.PaymentGatewayPort
import com.groom.payment.domain.port.PgRequestResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * PG(Payment Gateway) Adapter
 *
 * PaymentGatewayPort의 구현체
 *
 * 현재는 Stub 구현으로 항상 성공 응답을 반환합니다.
 * 실제 PG사 연동 시 이 클래스의 구현을 교체하면 됩니다.
 *
 * 실제 구현 예시:
 * - Toss Payments API 호출
 * - Kakao Pay API 호출
 * - NHN KCP API 호출
 */
@Component
class PgGatewayAdapter : PaymentGatewayPort {
    private val logger = KotlinLogging.logger {}

    override fun requestPayment(
        paymentId: UUID,
        amount: BigDecimal,
        orderNumber: String,
    ): PgRequestResult {
        logger.info { "PG Stub - 결제 요청: paymentId=$paymentId, amount=$amount, orderNumber=$orderNumber" }

        // TODO: PG사 연동을 하지않아 무조건 성공응답이 내려온다. 향후 연동을 하게되면 실제 응답값을 반영해야한다.
        val pgTransactionId = "PG-${UUID.randomUUID()}"
        val paymentUrl = "https://pg.example.com/pay/$paymentId"
        val expiresAt = LocalDateTime.now().plusMinutes(10)

        logger.info { "PG Stub - 결제 요청 성공: pgTransactionId=$pgTransactionId, expiresAt=$expiresAt" }

        return PgRequestResult(
            pgTransactionId = pgTransactionId,
            paymentUrl = paymentUrl,
            expiresAt = expiresAt,
        )
    }
}
