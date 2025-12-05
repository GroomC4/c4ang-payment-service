package com.groom.payment.adapter.inbound.kafka

import com.groom.ecommerce.saga.event.avro.StockConfirmationFailed
import com.groom.payment.application.dto.CancelPaymentCommand
import com.groom.payment.application.service.CancelPaymentService
import com.groom.payment.configuration.kafka.KafkaTopics
import com.groom.payment.domain.port.LoadPaymentPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * SAGA 보상 트랜잭션 Kafka Consumer
 *
 * 재고 확정 실패 시 결제 취소를 처리합니다.
 *
 * 플로우:
 * 1. Product Service에서 saga.stock-confirmation.failed 발행
 * 2. Payment Service가 소비하여 결제 취소
 * 3. payment.cancelled 이벤트 발행 (PaymentCancelledEventHandler)
 * 4. Order Service가 주문 취소
 *
 * 참고: c4ang-contract-hub/docs/event-flows/payment-processing/stock-confirmation-failed.md
 */
@Component
class SagaCompensationKafkaConsumer(
    private val loadPaymentPort: LoadPaymentPort,
    private val cancelPaymentService: CancelPaymentService,
) {
    /**
     * StockConfirmationFailed 이벤트 처리
     *
     * 발행자: Product Service
     * 처리: 결제 취소 → payment.cancelled 발행 → Order Service가 주문 취소
     */
    @KafkaListener(
        topics = [KafkaTopics.SAGA_STOCK_CONFIRMATION_FAILED],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun handleStockConfirmationFailed(
        event: StockConfirmationFailed,
        acknowledgment: Acknowledgment,
    ) {
        try {
            logger.info {
                "StockConfirmationFailed 이벤트 수신: orderId=${event.orderId}, " +
                    "paymentId=${event.paymentId}, reason=${event.failureReason}"
            }

            val paymentId = UUID.fromString(event.paymentId)

            // Payment 존재 여부 확인
            val payment = loadPaymentPort.loadById(paymentId)
            if (payment == null) {
                logger.warn { "Payment not found, skipping: paymentId=${event.paymentId}" }
                acknowledgment.acknowledge()
                return
            }

            // 이미 취소된 경우 스킵 (멱등성)
            if (payment.status.name == "PAYMENT_CANCELLED") {
                logger.info { "Payment already cancelled, skipping: paymentId=${event.paymentId}" }
                acknowledgment.acknowledge()
                return
            }

            // 결제 취소 실행
            val command = CancelPaymentCommand(
                paymentId = paymentId,
                reason = "STOCK_UNAVAILABLE: ${event.failureReason}",
            )

            val result = cancelPaymentService.execute(command)

            // 수동 커밋
            acknowledgment.acknowledge()

            logger.info {
                "StockConfirmationFailed 처리 완료: paymentId=${result.paymentId}, " +
                    "previousStatus=${result.previousStatus}, currentStatus=${result.currentStatus}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "StockConfirmationFailed 이벤트 처리 실패: orderId=${event.orderId}, " +
                    "paymentId=${event.paymentId}"
            }
            throw e
        }
    }
}
