package com.groom.payment.adapter.inbound.kafka

import com.groom.ecommerce.order.event.avro.OrderConfirmed
import com.groom.payment.application.dto.CreatePaymentWaitCommand
import com.groom.payment.application.service.CreatePaymentWaitService
import com.groom.payment.configuration.kafka.KafkaTopics
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Order 이벤트 Kafka Consumer
 *
 * Payment Service가 구독하는 Order 관련 이벤트:
 * - OrderConfirmed: 재고 예약 완료 → 결제 대기(PAYMENT_WAIT) 생성
 *
 * 참고: c4ang-contract-hub/docs/event-flows/order-creation/success.md
 */
@Component
class OrderEventKafkaConsumer(
    private val createPaymentWaitService: CreatePaymentWaitService,
) {
    /**
     * OrderConfirmed 이벤트 처리
     *
     * 발행자: Order Service
     * 처리: 결제 대기 상태(PAYMENT_WAIT) Payment 생성
     *
     * SAGA 플로우: stock.reserved → order.confirmed → Payment 대기 생성
     */
    @KafkaListener(
        topics = [KafkaTopics.ORDER_CONFIRMED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun handleOrderConfirmed(
        event: OrderConfirmed,
        acknowledgment: Acknowledgment,
    ) {
        try {
            logger.info {
                "OrderConfirmed 이벤트 수신: orderId=${event.orderId}, " +
                    "userId=${event.userId}, totalAmount=${event.totalAmount}"
            }

            // 결제 대기 생성 (멱등성은 서비스 레이어에서 보장)
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
            throw e
        }
    }
}
