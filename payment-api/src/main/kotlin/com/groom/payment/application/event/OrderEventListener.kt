package com.groom.payment.application.event

import com.groom.ecommerce.order.event.avro.OrderConfirmed
import com.groom.ecommerce.order.event.avro.OrderCreated
import com.groom.ecommerce.order.event.avro.StockConfirmed
import com.groom.payment.configuration.kafka.KafkaTopics
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Order 이벤트 리스너
 * - Payment SAGA 플로우의 Consumer
 * - OrderCreated: 초기 주문 생성 (현재는 사용 안 함)
 * - OrderConfirmed: 재고 예약 완료 → 결제 대기 생성
 * - StockConfirmed: 재고 확정 완료 → 주문 완료 처리
 */
@Component
class OrderEventListener(
    // TODO: PaymentService 주입 (실제 결제 처리 로직)
) {

    /**
     * OrderCreated 이벤트 처리 (현재는 사용 안 함)
     * - 향후 확장을 위해 유지
     */
    @KafkaListener(
        topics = [KafkaTopics.ORDER_CREATED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderCreated(event: OrderCreated, acknowledgment: Acknowledgment) {
        try {
            logger.info {
                "OrderCreated 이벤트 수신: orderId=${event.orderId}, " +
                    "userId=${event.userId}, totalAmount=${event.totalAmount}"
            }

            // 현재는 별도 처리 없음 (OrderConfirmed 이벤트에서 처리)
            acknowledgment.acknowledge()

            logger.info { "OrderCreated 이벤트 처리 완료: orderId=${event.orderId}" }

        } catch (e: Exception) {
            logger.error(e) { "OrderCreated 이벤트 처리 실패: orderId=${event.orderId}" }
            throw e
        }
    }

    /**
     * OrderConfirmed 이벤트 처리
     * - 재고 예약 완료 후 결제 대기 상태 생성
     * - SAGA 플로우: stock.reserved → order.confirmed → Payment 대기 생성
     */
    @KafkaListener(
        topics = [KafkaTopics.ORDER_CONFIRMED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderConfirmed(event: OrderConfirmed, acknowledgment: Acknowledgment) {
        try {
            logger.info {
                "OrderConfirmed 이벤트 수신: orderId=${event.orderId}, " +
                    "userId=${event.userId}, totalAmount=${event.totalAmount}"
            }

            // 멱등성 체크 (이미 처리된 이벤트인지 확인)
            // TODO: 이벤트 ID 기반 중복 처리 방지

            // 결제 대기 생성
            // TODO: PaymentService.createPaymentWait(event) 호출
            createPaymentWait(event)

            // 수동 커밋 (처리 완료 후)
            acknowledgment.acknowledge()

            logger.info { "OrderConfirmed 이벤트 처리 완료: orderId=${event.orderId}" }

        } catch (e: Exception) {
            logger.error(e) { "OrderConfirmed 이벤트 처리 실패: orderId=${event.orderId}" }
            throw e
        }
    }

    /**
     * StockConfirmed 이벤트 처리
     * - 결제 완료 후 재고 확정 완료 → 주문 완료 처리
     * - SAGA 플로우: payment.completed → stock.confirmed → Order COMPLETED
     */
    @KafkaListener(
        topics = [KafkaTopics.STOCK_CONFIRMED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleStockConfirmed(event: StockConfirmed, acknowledgment: Acknowledgment) {
        try {
            logger.info {
                "StockConfirmed 이벤트 수신: orderId=${event.orderId}, " +
                    "paymentId=${event.paymentId}"
            }

            // 멱등성 체크
            // TODO: 이벤트 ID 기반 중복 처리 방지

            // 결제 완료 처리 (Payment 상태를 CONFIRMED로 업데이트)
            // TODO: PaymentService.completePayment(event) 호출
            completePayment(event)

            // 수동 커밋
            acknowledgment.acknowledge()

            logger.info { "StockConfirmed 이벤트 처리 완료: paymentId=${event.paymentId}" }

        } catch (e: Exception) {
            logger.error(e) { "StockConfirmed 이벤트 처리 실패: paymentId=${event.paymentId}" }
            throw e
        }
    }

    /**
     * 결제 대기 생성 (임시 구현)
     */
    private fun createPaymentWait(event: OrderConfirmed) {
        // TODO: 실제 구현
        // 1. Payment 엔티티 생성 (status: PAYMENT_WAIT)
        // 2. 사용자에게 결제 요청 알림
        logger.info { "결제 대기 생성: orderId=${event.orderId}, amount=${event.totalAmount}" }
    }

    /**
     * 결제 완료 처리 (임시 구현)
     */
    private fun completePayment(event: StockConfirmed) {
        // TODO: 실제 구현
        // 1. Payment 상태를 CONFIRMED로 업데이트
        // 2. 주문 완료 처리
        logger.info { "결제 완료 처리: paymentId=${event.paymentId}, orderId=${event.orderId}" }
    }
}
