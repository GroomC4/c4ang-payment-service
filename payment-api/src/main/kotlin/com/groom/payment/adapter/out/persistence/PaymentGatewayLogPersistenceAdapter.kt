package com.groom.payment.adapter.out.persistence

import com.groom.payment.domain.model.PaymentGatewayLog
import com.groom.payment.domain.port.LoadPaymentGatewayLogPort
import com.groom.payment.domain.port.SavePaymentGatewayLogPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * PaymentGatewayLog Persistence Adapter (Hexagonal Architecture)
 *
 * Port 인터페이스를 구현하여 JPA를 통한 영속성을 제공합니다.
 */
@Component
class PaymentGatewayLogPersistenceAdapter(
    private val paymentGatewayLogJpaRepository: PaymentGatewayLogJpaRepository,
) : LoadPaymentGatewayLogPort,
    SavePaymentGatewayLogPort {
    override fun loadByPaymentId(paymentId: UUID): List<PaymentGatewayLog> = paymentGatewayLogJpaRepository.findByPayment_Id(paymentId)

    override fun loadByPgCode(pgCode: String): List<PaymentGatewayLog> = paymentGatewayLogJpaRepository.findByPgCode(pgCode)

    override fun loadByStatus(status: String): List<PaymentGatewayLog> = paymentGatewayLogJpaRepository.findByStatus(status)

    override fun save(log: PaymentGatewayLog): PaymentGatewayLog = paymentGatewayLogJpaRepository.save(log)
}
