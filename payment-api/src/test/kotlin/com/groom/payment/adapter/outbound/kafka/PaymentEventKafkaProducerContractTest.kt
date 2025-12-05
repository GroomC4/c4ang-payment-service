package com.groom.payment.adapter.outbound.kafka

import com.groom.payment.common.ContractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

/**
 * Payment Event Kafka Producer Contract Test
 * - Contract 파일에 정의된 이벤트를 실제로 발행하는지 검증
 * - Spring Cloud Contract Auto-generated tests가 이 클래스의 메서드를 호출
 */
class PaymentEventKafkaProducerContractTest : ContractTestBase() {
    @Autowired
    private lateinit var paymentEventKafkaProducer: PaymentEventKafkaProducer

    /**
     * Contract: should_publish_payment_completed_event.yml
     */
    open fun publishPaymentCompletedEvent() {
        paymentEventKafkaProducer.publishPaymentCompleted(
            paymentId = "PAY-12345",
            orderId = "ORD-12345",
            userId = "USER-001",
            totalAmount = BigDecimal("50000.00"),
            paymentMethod = "CARD",
            pgApprovalNumber = "APPROVE-12345",
        )
    }

    /**
     * Contract: should_publish_payment_failed_event.yml
     */
    open fun publishPaymentFailedEvent() {
        paymentEventKafkaProducer.publishPaymentFailed(
            paymentId = "PAY-12346",
            orderId = "ORD-12346",
            userId = "USER-002",
            failureReason = "카드 승인 거부",
        )
    }

    /**
     * Contract: should_publish_payment_cancelled_event.yml
     */
    open fun publishPaymentCancelledEvent() {
        paymentEventKafkaProducer.publishPaymentCancelled(
            paymentId = "PAY-12347",
            orderId = "ORD-12347",
            userId = "USER-003",
            cancellationReason = "STOCK_UNAVAILABLE",
        )
    }
}
