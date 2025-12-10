package com.groom.payment.adapter.inbound.kafka

import com.groom.ecommerce.order.event.avro.OrderConfirmed
import com.groom.payment.application.dto.CreatePaymentWaitCommand
import com.groom.payment.application.service.CreatePaymentWaitService
import com.groom.payment.common.idempotency.IdempotencyService
import com.groom.payment.configuration.kafka.KafkaTopics
import com.groom.payment.domain.port.PaymentEventPublishPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Order 이벤트 Kafka Consumer
 *
 * Payment Service가 구독하는 Order 관련 이벤트:
 * - OrderConfirmed: 재고 예약 완료 → 결제 대기(PAYMENT_WAIT) 생성
 *
 * 멱등성:
 * - eventId 기반 중복 처리 방지
 * - Redis SETNX를 통한 원자적 체크
 *
 * 실패 시:
 * - saga.payment-initialization.failed 이벤트 발행
 * - Order Service가 주문 취소 처리
 *
 * 참고: c4ang-contract-hub/docs/event-flows/order-creation/success.md
 */
@Component
class OrderEventKafkaConsumer(
    private val createPaymentWaitService: CreatePaymentWaitService,
    private val paymentEventPublishPort: PaymentEventPublishPort,
    private val idempotencyService: IdempotencyService,
) {
    companion object {
        private val IDEMPOTENCY_TTL = Duration.ofHours(24)
        private const val IDEMPOTENCY_PREFIX = "order-confirmed"
    }

    /**
     * OrderConfirmed 이벤트 처리
     *
     * 발행자: Order Service
     * 처리: 결제 대기 상태(PAYMENT_WAIT) Payment 생성
     *
     * SAGA 플로우: stock.reserved → order.confirmed → Payment 대기 생성
     *
     * 실패 시: saga.payment-initialization.failed 발행 → Order Service가 주문 취소
     */
    @KafkaListener(
        topics = [KafkaTopics.ORDER_CONFIRMED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun handleOrderConfirmed(
        @Payload event: OrderConfirmed,
        acknowledgment: Acknowledgment,
    ) {
        val idempotencyKey = "$IDEMPOTENCY_PREFIX:${event.eventId}"

        // 멱등성 체크
        if (!idempotencyService.ensureIdempotency(idempotencyKey, IDEMPOTENCY_TTL)) {
            logger.warn { "중복 이벤트 무시: eventId=${event.eventId}, orderId=${event.orderId}" }
            acknowledgment.acknowledge()
            return
        }

        try {
            logger.info {
                "OrderConfirmed 이벤트 수신: orderId=${event.orderId}, " +
                    "userId=${event.userId}, totalAmount=${event.totalAmount}, " +
                    "eventId=${event.eventId}"
            }

            // 결제 대기 생성
            val command = CreatePaymentWaitCommand(
                eventId = event.eventId,
                orderId = UUID.fromString(event.orderId),
                userId = UUID.fromString(event.userId),
                totalAmount = event.totalAmount,
            )

            val result = createPaymentWaitService.execute(command)

            // 수동 커밋 (처리 완료 후)
            acknowledgment.acknowledge()

            logger.info {
                "OrderConfirmed 이벤트 처리 완료: orderId=${event.orderId}, " +
                    "paymentId=${result.paymentId}, status=${result.status}"
            }
        } catch (e: Exception) {
            logger.error(e) { "OrderConfirmed 이벤트 처리 실패: orderId=${event.orderId}" }

            // 멱등성 키 해제 (재시도 허용)
            idempotencyService.release(idempotencyKey)

            // SAGA 보상: 결제 초기화 실패 이벤트 발행
            try {
                paymentEventPublishPort.publishPaymentInitializationFailed(
                    orderId = event.orderId,
                    failureReason = e.message ?: "Unknown error during payment initialization",
                )
                logger.info { "PaymentInitializationFailed 이벤트 발행 완료: orderId=${event.orderId}" }

                // 보상 이벤트 발행 후 커밋 (재처리 방지)
                acknowledgment.acknowledge()
            } catch (publishEx: Exception) {
                logger.error(publishEx) {
                    "PaymentInitializationFailed 이벤트 발행 실패: orderId=${event.orderId}"
                }
                // 보상 이벤트 발행도 실패하면 재처리를 위해 throw
                throw e
            }
        }
    }
}
