package com.groom.payment.adapter.inbound.kafka

import com.groom.ecommerce.order.event.avro.OrderConfirmed
import com.groom.payment.application.dto.CreatePaymentWaitResult
import com.groom.payment.application.service.CreatePaymentWaitService
import com.groom.payment.common.idempotency.IdempotencyService
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.PaymentEventPublishPort
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
        val paymentEventPublishPort = mockk<PaymentEventPublishPort>()
        val idempotencyService = mockk<IdempotencyService>()
        val consumer = OrderEventKafkaConsumer(
            createPaymentWaitService,
            paymentEventPublishPort,
            idempotencyService,
        )

        beforeEach {
            clearMocks(createPaymentWaitService, paymentEventPublishPort, idempotencyService)
        }

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

                every { idempotencyService.ensureIdempotency(any(), any()) } returns true
                every { createPaymentWaitService.execute(any()) } returns mockResult

                // when
                consumer.handleOrderConfirmed(event, acknowledgment)

                // then
                verify { idempotencyService.ensureIdempotency(any(), any()) }
                verify { createPaymentWaitService.execute(any()) }
                verify { acknowledgment.acknowledge() }
            }

            it("중복 이벤트는 무시한다") {
                // given
                val orderId = UUID.randomUUID()
                val userId = UUID.randomUUID()
                val totalAmount = BigDecimal("50000.00")

                val event =
                    OrderConfirmed
                        .newBuilder()
                        .setEventId("event-duplicate")
                        .setEventTimestamp(System.currentTimeMillis())
                        .setOrderId(orderId.toString())
                        .setUserId(userId.toString())
                        .setTotalAmount(totalAmount)
                        .setConfirmedAt(System.currentTimeMillis())
                        .build()

                val acknowledgment = mockk<Acknowledgment>(relaxed = true)

                every { idempotencyService.ensureIdempotency(any(), any()) } returns false

                // when
                consumer.handleOrderConfirmed(event, acknowledgment)

                // then
                verify { idempotencyService.ensureIdempotency(any(), any()) }
                verify(exactly = 0) { createPaymentWaitService.execute(any()) }
                verify { acknowledgment.acknowledge() }
            }

            it("결제 대기 생성 실패 시 PaymentInitializationFailed 이벤트를 발행한다") {
                // given
                val orderId = UUID.randomUUID()
                val userId = UUID.randomUUID()
                val totalAmount = BigDecimal("50000.00")

                val event =
                    OrderConfirmed
                        .newBuilder()
                        .setEventId("event-003")
                        .setEventTimestamp(System.currentTimeMillis())
                        .setOrderId(orderId.toString())
                        .setUserId(userId.toString())
                        .setTotalAmount(totalAmount)
                        .setConfirmedAt(System.currentTimeMillis())
                        .build()

                val acknowledgment = mockk<Acknowledgment>(relaxed = true)

                every { idempotencyService.ensureIdempotency(any(), any()) } returns true
                every { idempotencyService.release(any()) } just runs
                every { createPaymentWaitService.execute(any()) } throws RuntimeException("Test error")
                every { paymentEventPublishPort.publishPaymentInitializationFailed(any(), any()) } just runs

                // when
                consumer.handleOrderConfirmed(event, acknowledgment)

                // then
                verify { idempotencyService.release(any()) }
                verify { paymentEventPublishPort.publishPaymentInitializationFailed(orderId.toString(), any()) }
                verify { acknowledgment.acknowledge() }
            }
        }
    })
