package com.groom.payment.adapter.outbound.kafka

import com.groom.ecommerce.payment.event.avro.PaymentCancellationReason
import com.groom.ecommerce.payment.event.avro.PaymentCancelled
import com.groom.ecommerce.payment.event.avro.PaymentCompleted
import com.groom.ecommerce.payment.event.avro.PaymentFailed
import com.groom.ecommerce.payment.event.avro.PaymentMethod
import com.groom.ecommerce.saga.event.avro.PaymentInitializationFailed
import com.groom.payment.configuration.kafka.KafkaTopics
import com.groom.payment.domain.port.PaymentEventPublishPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Payment 이벤트 Kafka Producer
 * - PaymentCompleted: 결제 완료 시 Product Service에 통보 (재고 확정)
 * - PaymentFailed: 결제 실패 시 Order Service에 통보
 * - PaymentCancelled: 결제 취소 시 Order Service에 통보
 * - PaymentInitializationFailed: SAGA 결제 초기화 실패 시 Order Service에 통보
 */
@Component
class PaymentEventKafkaProducer(
    private val paymentCompletedTemplate: KafkaTemplate<String, PaymentCompleted>,
    private val paymentFailedTemplate: KafkaTemplate<String, PaymentFailed>,
    private val paymentCancelledTemplate: KafkaTemplate<String, PaymentCancelled>,
    private val paymentInitializationFailedTemplate: KafkaTemplate<String, PaymentInitializationFailed>,
) : PaymentEventPublishPort {
    /**
     * 결제 완료 이벤트 발행
     */
    override fun publishPaymentCompleted(
        paymentId: String,
        orderId: String,
        userId: String,
        totalAmount: BigDecimal,
        paymentMethod: String,
        pgApprovalNumber: String,
    ) {
        val event =
            PaymentCompleted
                .newBuilder()
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

        paymentCompletedTemplate
            .send(KafkaTopics.PAYMENT_COMPLETED, orderId, event)
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
    override fun publishPaymentFailed(
        paymentId: String,
        orderId: String,
        userId: String,
        failureReason: String,
    ) {
        val event =
            PaymentFailed
                .newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventTimestamp(System.currentTimeMillis())
                .setPaymentId(paymentId)
                .setOrderId(orderId)
                .setUserId(userId)
                .setFailureReason(failureReason)
                .setFailedAt(System.currentTimeMillis())
                .build()

        paymentFailedTemplate
            .send(KafkaTopics.PAYMENT_FAILED, orderId, event)
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
    override fun publishPaymentCancelled(
        paymentId: String,
        orderId: String,
        userId: String,
        cancellationReason: String,
    ) {
        val event =
            PaymentCancelled
                .newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventTimestamp(System.currentTimeMillis())
                .setPaymentId(paymentId)
                .setOrderId(orderId)
                .setUserId(userId)
                .setCancellationReason(convertToCancellationReason(cancellationReason))
                .setCancelledAt(System.currentTimeMillis())
                .build()

        paymentCancelledTemplate
            .send(KafkaTopics.PAYMENT_CANCELLED, orderId, event)
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
     * BigDecimal을 Avro Decimal(BigDecimal)로 변환
     * - Avro의 decimal logical type은 BigDecimal을 직접 사용
     */
    private fun convertToAvroDecimal(value: BigDecimal): BigDecimal = value.setScale(2, BigDecimal.ROUND_HALF_UP)

    /**
     * String을 PaymentMethod Enum으로 변환
     */
    private fun convertToPaymentMethod(method: String): PaymentMethod =
        when (method.uppercase()) {
            "CARD" -> PaymentMethod.CARD
            "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
            "KAKAO_PAY" -> PaymentMethod.KAKAO_PAY
            "NAVER_PAY" -> PaymentMethod.NAVER_PAY
            "TOSS" -> PaymentMethod.TOSS
            else -> PaymentMethod.CARD
        }

    /**
     * SAGA 결제 초기화 실패 이벤트 발행
     *
     * order.confirmed 이벤트 처리 중 결제 대기 생성에 실패한 경우 발행
     * Order Service가 이를 수신하여 주문 취소 처리
     */
    override fun publishPaymentInitializationFailed(
        orderId: String,
        failureReason: String,
    ) {
        val event =
            PaymentInitializationFailed
                .newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventTimestamp(System.currentTimeMillis())
                .setOrderId(orderId)
                .setFailureReason(failureReason)
                .setFailedAt(System.currentTimeMillis())
                .build()

        paymentInitializationFailedTemplate
            .send(KafkaTopics.SAGA_PAYMENT_INITIALIZATION_FAILED, orderId, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info {
                        "PaymentInitializationFailed 이벤트 발행 성공: orderId=$orderId, " +
                            "reason=$failureReason"
                    }
                } else {
                    logger.error(ex) { "PaymentInitializationFailed 이벤트 발행 실패: orderId=$orderId" }
                }
            }
    }

    /**
     * String을 PaymentCancellationReason Enum으로 변환
     */
    private fun convertToCancellationReason(reason: String): PaymentCancellationReason =
        when (reason.uppercase()) {
            "STOCK_UNAVAILABLE" -> PaymentCancellationReason.STOCK_UNAVAILABLE
            "ADMIN_CANCEL" -> PaymentCancellationReason.ADMIN_CANCEL
            "USER_CANCEL" -> PaymentCancellationReason.USER_CANCEL
            "SYSTEM_ERROR" -> PaymentCancellationReason.SYSTEM_ERROR
            else -> PaymentCancellationReason.SYSTEM_ERROR
        }
}
