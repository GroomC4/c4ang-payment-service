package com.groom.payment.domain.model

import com.groom.payment.configuration.jpa.CreatedAndUpdatedAtAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * PaymentGatewayLog 엔티티.
 * DDL: p_payment_gateway_log 테이블
 */
@Entity
@Table(name = "p_payment_gateway_log")
class PaymentGatewayLog(
    @Column(nullable = false)
    val pgCode: String,
    @Column(nullable = false)
    val status: String, // REQUEST, APPROVED, FAILED
    @Column(nullable = false, columnDefinition = "text")
    val externalPaymentData: String,
    @Column
    val deletedAt: LocalDateTime? = null,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    var payment: Payment? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentGatewayLog) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "PaymentGatewayLog(id=$id, pgCode=$pgCode, status=$status)"
}
