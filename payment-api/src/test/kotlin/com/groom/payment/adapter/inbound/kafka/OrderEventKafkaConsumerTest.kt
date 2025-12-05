package com.groom.payment.adapter.inbound.kafka

import com.groom.ecommerce.order.event.avro.OrderConfirmed
import com.groom.payment.application.dto.CreatePaymentWaitResult
import com.groom.payment.application.service.CreatePaymentWaitService
import com.groom.payment.domain.model.PaymentStatus
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.kafka.support.Acknowledgment
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * OrderEventKafkaConsumer Unit Test
 * - OrderConfirmed 이벤트 핸들러의 동작을 검증
 */
class OrderEventKafkaConsumerTest :
    DescribeSpec({

        val createPaymentWaitService = mockk<CreatePaymentWaitService>()
        val consumer = OrderEventKafkaConsumer(createPaymentWaitService)

        describe("handleOrderConfirmed") {
            it("OrderConfirmed 이벤트를 수신하고 결제 대기를 생성한다") {
                // given
                val orderId = UUID.randomUUID()
                val userId = UUID.randomUUID()
                val paymentId = UUID.randomUUID()
                val totalAmount = BigDecimal("50000.00")

                val event =
                    OrderConfirmed
                        .newBuilder()
                        .setEventId("event-002")
                        .setEventTimestamp(System.currentTimeMillis())
                        .setOrderId(orderId.toString())
                        .setUserId(userId.toString())
                        .setTotalAmount(totalAmount)
                        .setConfirmedAt(System.currentTimeMillis())
                        .build()

                val acknowledgment = mockk<Acknowledgment>(relaxed = true)

                val mockResult = CreatePaymentWaitResult(
                    paymentId = paymentId,
                    orderId = orderId,
                    userId = userId,
                    status = PaymentStatus.PAYMENT_WAIT,
                    createdAt = LocalDateTime.now(),
                )

                every { createPaymentWaitService.execute(any()) } returns mockResult

                // when
                consumer.handleOrderConfirmed(event, acknowledgment)

                // then
                verify { createPaymentWaitService.execute(any()) }
                verify { acknowledgment.acknowledge() }
            }
        }
    })
