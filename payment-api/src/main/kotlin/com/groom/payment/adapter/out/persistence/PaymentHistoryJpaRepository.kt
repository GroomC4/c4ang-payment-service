package com.groom.payment.adapter.out.persistence

import com.groom.payment.domain.model.PaymentHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/**
 * PaymentHistory JPA Repository (Adapter 내부)
 *
 * Hexagonal Architecture에서 이 Repository는 Adapter 내부에만 위치합니다.
 */
interface PaymentHistoryJpaRepository : JpaRepository<PaymentHistory, UUID> {
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.payment.id = :paymentId")
    fun findByPaymentId(
        @Param("paymentId") paymentId: UUID,
    ): List<PaymentHistory>

    fun findByEventType(eventType: String): List<PaymentHistory>
}
