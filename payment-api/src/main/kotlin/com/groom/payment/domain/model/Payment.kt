package com.groom.payment.domain.model

import com.groom.payment.configuration.jpa.CreatedAndUpdatedAtAuditEntity
import com.groom.payment.domain.event.PaymentCancelledEvent
import com.groom.payment.domain.event.PaymentCompletedEvent
import com.groom.payment.domain.event.PaymentFailedEvent
import com.groom.payment.domain.event.PaymentRefundCompletedEvent
import com.groom.payment.domain.event.PaymentRefundRequestedEvent
import com.groom.payment.domain.event.PaymentRequestedEvent
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Payment 애그리게이트 루트
 *
 * DDD 원칙:
 * - private setter로 불변성 보장
 * - 도메인 메서드를 통한 상태 변경
 * - 도메인 이벤트 발행
 * - PaymentHistory 자동 기록
 *
 * 동시성 제어:
 * - @Version: JPA Optimistic Lock (선택적 안전장치)
 * - Redis 분산 락: lock:payment:{paymentId} (주요 제어)
 *
 * Payment 생성 전략:
 * - 재고 예약 완료 시 PAYMENT_WAIT 상태로 생성 (금액 정보 없음)
 * - 사용자 결제 요청 시 금액 정보 설정 및 PAYMENT_REQUEST로 변경
 */
@Entity
@Table(name = "p_payment")
class Payment(
    orderId: UUID,
    userId: UUID,
    totalAmount: BigDecimal? = null,
    paymentAmount: BigDecimal? = null,
    discountAmount: BigDecimal? = null,
    deliveryFee: BigDecimal? = null,
    method: PaymentMethod? = null,
    status: PaymentStatus = PaymentStatus.PAYMENT_WAIT,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()
        private set

    @Version
    @Column(nullable = false)
    var version: Long = 0L
        private set

    @Column(nullable = false)
    var orderId: UUID = orderId
        private set

    @Column(nullable = false)
    var userId: UUID = userId
        private set

    @Column(precision = 12, scale = 2)
    var totalAmount: BigDecimal? = totalAmount
        private set

    @Column(precision = 12, scale = 2)
    var paymentAmount: BigDecimal? = paymentAmount
        private set

    @Column(precision = 12, scale = 2)
    var discountAmount: BigDecimal? = discountAmount
        private set

    @Column(precision = 12, scale = 2)
    var deliveryFee: BigDecimal? = deliveryFee
        private set

    @Column
    @Enumerated(EnumType.STRING)
    var method: PaymentMethod? = method
        private set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = status
        private set

    @Column(nullable = false)
    var requestedAt: LocalDateTime = LocalDateTime.now()
        private set

    @Column
    var completedAt: LocalDateTime? = null
        private set

    @Column
    var cancelledAt: LocalDateTime? = null
        private set

    @Column
    var refundedAt: LocalDateTime? = null
        private set

    @Column
    var pgTransactionId: String? = null
        private set

    @Column
    var pgApprovalNumber: String? = null
        private set

    @Column
    var refundTransactionId: String? = null
        private set

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    var histories: MutableList<PaymentHistory> = mutableListOf()
        private set

    /**
     * PG사로 결제 요청 전송
     *
     * PAYMENT_WAIT → PAYMENT_REQUEST 상태 전이
     * 사용자가 결제 수단과 금액 정보를 선택/확인한 후 호출됩니다.
     *
     * @param pgTransactionId PG사 거래 ID
     * @param totalAmount 주문 총액
     * @param paymentAmount 실제 결제 금액 (할인 적용 후)
     * @param discountAmount 할인 금액
     * @param deliveryFee 배송비
     * @param method 결제 수단
     * @throws IllegalArgumentException PAYMENT_WAIT 상태가 아닌 경우
     */
    fun requestPayment(
        pgTransactionId: String,
        totalAmount: BigDecimal,
        paymentAmount: BigDecimal,
        discountAmount: BigDecimal,
        deliveryFee: BigDecimal,
        method: PaymentMethod,
    ) {
        require(status == PaymentStatus.PAYMENT_WAIT) {
            "Only PAYMENT_WAIT payments can be requested"
        }

        // 금액 정보 설정
        this.totalAmount = totalAmount
        this.paymentAmount = paymentAmount
        this.discountAmount = discountAmount
        this.deliveryFee = deliveryFee
        this.method = method

        // 상태 변경
        this.status = PaymentStatus.PAYMENT_REQUEST
        this.pgTransactionId = pgTransactionId

        addHistory(
            eventType = PaymentEventType.PAYMENT_REQUESTED,
            changeSummary =
                "PG사로 결제 요청 전송 (txId: $pgTransactionId, " +
                    "amount: $paymentAmount, method: ${method.name})",
        )
    }

    /**
     * PG사 콜백 - 결제 완료
     */
    fun complete(pgApprovalNumber: String) {
        require(status == PaymentStatus.PAYMENT_REQUEST) {
            "Only PAYMENT_REQUEST payments can be completed"
        }

        this.status = PaymentStatus.PAYMENT_COMPLETED
        this.completedAt = LocalDateTime.now()
        this.pgApprovalNumber = pgApprovalNumber

        addHistory(
            eventType = PaymentEventType.PAYMENT_COMPLETED,
            changeSummary = "결제 완료 (승인번호: $pgApprovalNumber)",
        )
    }

    /**
     * 결제 취소
     */
    fun cancel(reason: String) {
        require(status.isCancellable()) {
            "Cannot cancel payment in status: $status"
        }

        this.status = PaymentStatus.PAYMENT_CANCELLED
        this.cancelledAt = LocalDateTime.now()

        addHistory(
            eventType = PaymentEventType.PAYMENT_CANCELLED,
            changeSummary = "결제 취소: $reason",
        )
    }

    /**
     * 환불 요청
     */
    fun requestRefund(
        reason: String,
        refundAmount: BigDecimal,
    ) {
        require(status == PaymentStatus.PAYMENT_COMPLETED) {
            "Only PAYMENT_COMPLETED payments can be refunded"
        }
        require(refundAmount > BigDecimal.ZERO && refundAmount <= paymentAmount) {
            "Refund amount must be between 0 and payment amount"
        }

        this.status = PaymentStatus.REFUND_REQUESTED

        addHistory(
            eventType = PaymentEventType.REFUND_REQUESTED,
            changeSummary = "환불 요청: $reason (금액: $refundAmount)",
        )
    }

    /**
     * 환불 완료
     */
    fun completeRefund(refundTransactionId: String) {
        require(status == PaymentStatus.REFUND_REQUESTED) {
            "Only REFUND_REQUESTED payments can complete refund"
        }

        this.status = PaymentStatus.REFUND_COMPLETED
        this.refundedAt = LocalDateTime.now()
        this.refundTransactionId = refundTransactionId

        addHistory(
            eventType = PaymentEventType.REFUND_COMPLETED,
            changeSummary = "환불 완료 (거래ID: $refundTransactionId)",
        )
    }

    /**
     * 결제 실패 처리
     */
    fun markFailed(reason: String) {
        require(status == PaymentStatus.PAYMENT_REQUEST) {
            "Only PAYMENT_REQUEST payments can fail"
        }

        this.status = PaymentStatus.PAYMENT_FAILED

        addHistory(
            eventType = PaymentEventType.PAYMENT_FAILED,
            changeSummary = "결제 실패: $reason",
        )
    }

    /**
     * PaymentHistory 자동 기록
     * 모든 상태 변경 시 자동으로 이력 추가
     */
    private fun addHistory(
        eventType: PaymentEventType,
        changeSummary: String,
    ) {
        val history =
            PaymentHistory(
                payment = this,
                eventType = eventType,
                changeSummary = changeSummary,
                recordedAt = LocalDateTime.now(),
            )
        histories.add(history)
    }

    private fun PaymentStatus.isCancellable(): Boolean =
        when (this) {
            PaymentStatus.PAYMENT_WAIT,
            PaymentStatus.PAYMENT_REQUEST,
            -> true

            else -> false
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Payment) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Payment(id=$id, orderId=$orderId, userId=$userId, totalAmount=$totalAmount, status=$status)"
}
