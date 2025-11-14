package com.groom.payment.adapter.out.persistence

import com.groom.payment.domain.model.PaymentGatewayLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * PaymentGatewayLog JPA Repository (Adapter 내부)
 *
 * Hexagonal Architecture에서 이 Repository는 Adapter 내부에만 위치합니다.
 */
interface PaymentGatewayLogJpaRepository : JpaRepository<PaymentGatewayLog, UUID> {
    fun findByPayment_Id(paymentId: UUID): List<PaymentGatewayLog>

    fun findByPgCode(pgCode: String): List<PaymentGatewayLog>

    fun findByStatus(status: String): List<PaymentGatewayLog>
}
