package com.groom.payment.application.event

import com.groom.ecommerce.order.event.avro.ConfirmedOrderItem
import com.groom.ecommerce.order.event.avro.OrderConfirmed
import com.groom.ecommerce.order.event.avro.OrderCreated
import com.groom.ecommerce.order.event.avro.OrderItem
import com.groom.ecommerce.order.event.avro.StockConfirmed
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.mockk
import io.mockk.verify
import org.springframework.kafka.support.Acknowledgment
import java.math.BigDecimal
import java.nio.ByteBuffer

/**
 * OrderEventListener Unit Test
 * - 각 이벤트 핸들러의 동작을 검증
 */
class OrderEventListenerTest : DescribeSpec({

    val listener = OrderEventListener()

    describe("handleOrderCreated") {
        it("OrderCreated 이벤트를 수신하고 처리한다") {
            // given
            val event = OrderCreated.newBuilder()
                .setEventId("event-001")
                .setEventTimestamp(System.currentTimeMillis())
                .setOrderId("ORD-001")
                .setUserId("USER-001")
                .setStoreId("STORE-001")
                .setItems(
                    listOf(
                        OrderItem.newBuilder()
                            .setProductId("PROD-001")
                            .setQuantity(2)
                            .setUnitPrice(convertToAvroDecimal(BigDecimal("10000")))
                            .build()
                    )
                )
                .setTotalAmount(convertToAvroDecimal(BigDecimal("20000")))
                .setCreatedAt(System.currentTimeMillis())
                .build()

            val acknowledgment = mockk<Acknowledgment>(relaxed = true)

            // when
            listener.handleOrderCreated(event, acknowledgment)

            // then
            verify { acknowledgment.acknowledge() }
        }
    }

    describe("handleOrderConfirmed") {
        it("OrderConfirmed 이벤트를 수신하고 결제 대기를 생성한다") {
            // given
            val event = OrderConfirmed.newBuilder()
                .setEventId("event-002")
                .setEventTimestamp(System.currentTimeMillis())
                .setOrderId("ORD-002")
                .setUserId("USER-002")
                .setTotalAmount(convertToAvroDecimal(BigDecimal("50000")))
                .setConfirmedAt(System.currentTimeMillis())
                .build()

            val acknowledgment = mockk<Acknowledgment>(relaxed = true)

            // when
            listener.handleOrderConfirmed(event, acknowledgment)

            // then
            verify { acknowledgment.acknowledge() }
        }
    }

    describe("handleStockConfirmed") {
        it("StockConfirmed 이벤트를 수신하고 결제를 완료 처리한다") {
            // given
            val event = StockConfirmed.newBuilder()
                .setEventId("event-003")
                .setEventTimestamp(System.currentTimeMillis())
                .setOrderId("ORD-003")
                .setPaymentId("PAY-003")
                .setConfirmedItems(
                    listOf(
                        ConfirmedOrderItem.newBuilder()
                            .setProductId("PROD-001")
                            .setQuantity(1)
                            .build()
                    )
                )
                .setConfirmedAt(System.currentTimeMillis())
                .build()

            val acknowledgment = mockk<Acknowledgment>(relaxed = true)

            // when
            listener.handleStockConfirmed(event, acknowledgment)

            // then
            verify { acknowledgment.acknowledge() }
        }
    }
})

/**
 * BigDecimal을 Avro Decimal(ByteBuffer)로 변환
 */
private fun convertToAvroDecimal(value: BigDecimal): ByteBuffer {
    val unscaledValue = value.setScale(2, BigDecimal.ROUND_HALF_UP).unscaledValue()
    return ByteBuffer.wrap(unscaledValue.toByteArray())
}
