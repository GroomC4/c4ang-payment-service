package com.groom.payment.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * PaymentHistory 엔티티
 *
 * Payment 상태 변경 이력을 자동으로 기록하는 감사 로그 테이블
 *
 * Payment 도메인 메서드 호출 시 자동으로 생성됨:
 * - payment.requestPayment() → PAYMENT_REQUESTED
 * - payment.complete() → PAYMENT_COMPLETED
 * - payment.cancel() → PAYMENT_CANCELLED
 * - payment.requestRefund() → REFUND_REQUESTED
 * - payment.completeRefund() → REFUND_COMPLETED
 * - payment.markFailed() → PAYMENT_FAILED
 *
 * DDL: p_payment_history 테이블
 */
@Entity
@Table(name = "p_payment_history")
class PaymentHistory(
    payment: Payment,
    eventType: PaymentEventType,
    changeSummary: String,
    recordedAt: LocalDateTime,
) {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()
        private set

    /**
     * Payment 애그리게이트와의 관계
     * Payment.addHistory()에서 자동으로 설정됨
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    var payment: Payment = payment
        private set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var eventType: PaymentEventType = eventType
        private set

    @Column(nullable = false, columnDefinition = "TEXT")
    var changeSummary: String = changeSummary
        private set

    @Column(nullable = false)
    var recordedAt: LocalDateTime = recordedAt
        private set

    /**
     * Payment ID를 가져오는 편의 메서드
     */
    val paymentId: UUID
        get() = payment.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentHistory) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "PaymentHistory(id=$id, paymentId=${payment.id}, eventType=$eventType, recordedAt=$recordedAt)"
}
