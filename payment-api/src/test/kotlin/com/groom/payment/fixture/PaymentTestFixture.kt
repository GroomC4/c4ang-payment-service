package com.groom.payment.fixture

import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Payment 엔티티 테스트 픽스처
 *
 * JPA Auditing으로 설정되는 createdAt/updatedAt 같은 필드를
 * 리플렉션으로 초기화하여 테스트용 Payment 객체를 생성합니다.
 */
object PaymentTestFixture {
    /**
     * 기본 Payment 생성
     */
    fun createPayment(
        id: UUID = UUID.randomUUID(),
        orderId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        totalAmount: BigDecimal = BigDecimal("50000"),
        paymentAmount: BigDecimal = BigDecimal("50000"),
        discountAmount: BigDecimal = BigDecimal.ZERO,
        deliveryFee: BigDecimal = BigDecimal.ZERO,
        method: PaymentMethod = PaymentMethod.CARD,
        status: PaymentStatus = PaymentStatus.PAYMENT_WAIT,
        requestedAt: LocalDateTime = LocalDateTime.now(),
        completedAt: LocalDateTime? = null,
        cancelledAt: LocalDateTime? = null,
        refundedAt: LocalDateTime? = null,
        pgTransactionId: String? = null,
        pgApprovalNumber: String? = null,
        refundTransactionId: String? = null,
        version: Long = 0L,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
    ): Payment {
        val payment =
            Payment(
                orderId = orderId,
                userId = userId,
                totalAmount = totalAmount,
                paymentAmount = paymentAmount,
                discountAmount = discountAmount,
                deliveryFee = deliveryFee,
                method = method,
                status = status,
            )

        // 리플렉션으로 protected 필드 설정
        setField(payment, "id", id)
        setField(payment, "version", version)
        setField(payment, "requestedAt", requestedAt)
        setField(payment, "completedAt", completedAt)
        setField(payment, "cancelledAt", cancelledAt)
        setField(payment, "refundedAt", refundedAt)
        setField(payment, "pgTransactionId", pgTransactionId)
        setField(payment, "pgApprovalNumber", pgApprovalNumber)
        setField(payment, "refundTransactionId", refundTransactionId)
        setField(payment, "createdAt", createdAt)
        setField(payment, "updatedAt", updatedAt)

        return payment
    }

    /**
     * PAYMENT_WAIT 상태의 Payment 생성
     * (Payment 생성 직후 상태)
     */
    fun createPaymentWait(
        orderId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        paymentAmount: BigDecimal = BigDecimal("50000"),
    ): Payment =
        createPayment(
            orderId = orderId,
            userId = userId,
            totalAmount = paymentAmount,
            paymentAmount = paymentAmount,
            status = PaymentStatus.PAYMENT_WAIT,
        )

    /**
     * PAYMENT_REQUEST 상태의 Payment 생성
     * (PG 결제 요청 전송 후 상태)
     */
    fun createPaymentRequest(
        orderId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        paymentAmount: BigDecimal = BigDecimal("50000"),
        pgTransactionId: String = "PG-TX-${UUID.randomUUID().toString().take(8)}",
    ): Payment =
        createPayment(
            orderId = orderId,
            userId = userId,
            totalAmount = paymentAmount,
            paymentAmount = paymentAmount,
            status = PaymentStatus.PAYMENT_REQUEST,
            pgTransactionId = pgTransactionId,
        )

    /**
     * PAYMENT_COMPLETED 상태의 Payment 생성
     * (결제 완료 상태)
     */
    fun createPaymentCompleted(
        orderId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        paymentAmount: BigDecimal = BigDecimal("50000"),
        pgTransactionId: String = "PG-TX-${UUID.randomUUID().toString().take(8)}",
        pgApprovalNumber: String = "PG-APPROVAL-${UUID.randomUUID().toString().take(8)}",
    ): Payment =
        createPayment(
            orderId = orderId,
            userId = userId,
            totalAmount = paymentAmount,
            paymentAmount = paymentAmount,
            status = PaymentStatus.PAYMENT_COMPLETED,
            pgTransactionId = pgTransactionId,
            pgApprovalNumber = pgApprovalNumber,
            completedAt = LocalDateTime.now(),
        )

    /**
     * 리플렉션으로 필드 설정 (private/protected 필드 접근)
     */
    fun setField(
        target: Any,
        fieldName: String,
        value: Any?,
    ) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, value)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchFieldException("Field $fieldName not found in ${target.javaClass}")
    }
}
