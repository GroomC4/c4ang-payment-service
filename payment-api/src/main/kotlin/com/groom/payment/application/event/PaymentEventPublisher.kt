package com.groom.payment.application.event

import com.groom.ecommerce.payment.event.avro.PaymentCancellationReason
import com.groom.ecommerce.payment.event.avro.PaymentCancelled
import com.groom.ecommerce.payment.event.avro.PaymentCompleted
import com.groom.ecommerce.payment.event.avro.PaymentFailed
import com.groom.ecommerce.payment.event.avro.PaymentMethod
import com.groom.payment.configuration.kafka.KafkaTopics
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Payment 이벤트 발행
 * - PaymentCompleted: 결제 완료 시 Order Service에 통보
 * - PaymentFailed: 결제 실패 시 Order Service에 통보
 * - PaymentCancelled: 결제 취소 시 Order Service에 통보
 */
@Component
class PaymentEventPublisher(
    private val paymentCompletedTemplate: KafkaTemplate<String, PaymentCompleted>,
    private val paymentFailedTemplate: KafkaTemplate<String, PaymentFailed>,
    private val paymentCancelledTemplate: KafkaTemplate<String, PaymentCancelled>,
) {

    /**
     * 결제 완료 이벤트 발행
     */
    fun publishPaymentCompleted(
        paymentId: String,
        orderId: String,
        userId: String,
        totalAmount: BigDecimal,
        paymentMethod: String,
        pgApprovalNumber: String,
    ) {
        val event = PaymentCompleted.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventTimestamp(System.currentTimeMillis())
            .setPaymentId(paymentId)
            .setOrderId(orderId)
            .setUserId(userId)
            .setTotalAmount(convertToAvroDecimal(totalAmount))
            .setPaymentMethod(convertToPaymentMethod(paymentMethod))
            .setPgApprovalNumber(pgApprovalNumber)
            .setCompletedAt(System.currentTimeMillis())
            .build()

        paymentCompletedTemplate.send(KafkaTopics.PAYMENT_COMPLETED, orderId, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info {
                        "PaymentCompleted 이벤트 발행 성공: paymentId=$paymentId, orderId=$orderId, " +
                            "partition=${result?.recordMetadata?.partition()}, offset=${result?.recordMetadata?.offset()}"
                    }
                } else {
                    logger.error(ex) { "PaymentCompleted 이벤트 발행 실패: paymentId=$paymentId, orderId=$orderId" }
                }
            }
    }

    /**
     * 결제 실패 이벤트 발행
     */
    fun publishPaymentFailed(
        paymentId: String,
        orderId: String,
        userId: String,
        failureReason: String,
    ) {
        val event = PaymentFailed.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventTimestamp(System.currentTimeMillis())
            .setPaymentId(paymentId)
            .setOrderId(orderId)
            .setUserId(userId)
            .setFailureReason(failureReason)
            .setFailedAt(System.currentTimeMillis())
            .build()

        paymentFailedTemplate.send(KafkaTopics.PAYMENT_FAILED, orderId, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info {
                        "PaymentFailed 이벤트 발행 성공: paymentId=$paymentId, orderId=$orderId, " +
                            "reason=$failureReason"
                    }
                } else {
                    logger.error(ex) { "PaymentFailed 이벤트 발행 실패: paymentId=$paymentId, orderId=$orderId" }
                }
            }
    }

    /**
     * 결제 취소 이벤트 발행
     */
    fun publishPaymentCancelled(
        paymentId: String,
        orderId: String,
        userId: String,
        cancellationReason: String,
    ) {
        val event = PaymentCancelled.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventTimestamp(System.currentTimeMillis())
            .setPaymentId(paymentId)
            .setOrderId(orderId)
            .setUserId(userId)
            .setCancellationReason(convertToCancellationReason(cancellationReason))
            .setCancelledAt(System.currentTimeMillis())
            .build()

        paymentCancelledTemplate.send(KafkaTopics.PAYMENT_CANCELLED, orderId, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info {
                        "PaymentCancelled 이벤트 발행 성공: paymentId=$paymentId, orderId=$orderId, " +
                            "reason=$cancellationReason"
                    }
                } else {
                    logger.error(ex) { "PaymentCancelled 이벤트 발행 실패: paymentId=$paymentId, orderId=$orderId" }
                }
            }
    }

    /**
     * BigDecimal을 Avro Decimal(ByteBuffer)로 변환
     */
    private fun convertToAvroDecimal(value: BigDecimal): ByteBuffer {
        val unscaledValue = value.setScale(2, BigDecimal.ROUND_HALF_UP).unscaledValue()
        return ByteBuffer.wrap(unscaledValue.toByteArray())
    }

    /**
     * String을 PaymentMethod Enum으로 변환
     */
    private fun convertToPaymentMethod(method: String): PaymentMethod {
        return when (method.uppercase()) {
            "CARD" -> PaymentMethod.CARD
            "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
            "KAKAO_PAY" -> PaymentMethod.KAKAO_PAY
            "NAVER_PAY" -> PaymentMethod.NAVER_PAY
            "TOSS" -> PaymentMethod.TOSS
            else -> PaymentMethod.CARD
        }
    }

    /**
     * String을 PaymentCancellationReason Enum으로 변환
     */
    private fun convertToCancellationReason(reason: String): PaymentCancellationReason {
        return when (reason.uppercase()) {
            "STOCK_UNAVAILABLE" -> PaymentCancellationReason.STOCK_UNAVAILABLE
            "ADMIN_CANCEL" -> PaymentCancellationReason.ADMIN_CANCEL
            "USER_CANCEL" -> PaymentCancellationReason.USER_CANCEL
            "SYSTEM_ERROR" -> PaymentCancellationReason.SYSTEM_ERROR
            else -> PaymentCancellationReason.SYSTEM_ERROR
        }
    }
}
