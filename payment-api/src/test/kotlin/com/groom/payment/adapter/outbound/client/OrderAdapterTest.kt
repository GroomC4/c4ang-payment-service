package com.groom.payment.adapter.outbound.client

import com.groom.payment.adapter.outbound.client.dto.OrderServiceGetOrderItemResponse
import com.groom.payment.adapter.outbound.client.dto.OrderServiceGetOrderResponse
import com.groom.payment.adapter.outbound.client.dto.OrderServiceHasPaymentResponse
import com.groom.payment.adapter.outbound.client.dto.OrderServiceMarkPaymentPendingResponse
import com.groom.payment.common.annotation.UnitTest
import com.groom.payment.common.exception.OrderServiceException
import com.groom.payment.configuration.feign.FeignClientException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.util.UUID

/**
 * OrderAdapter Unit Test
 *
 * OrderAdapter가 OrderServiceClient를 올바르게 호출하고,
 * 예외를 적절하게 변환하는지 검증합니다.
 */
@UnitTest
class OrderAdapterTest :
    DescribeSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val orderServiceClient = mockk<OrderServiceClient>()
        val orderAdapter = OrderAdapter(orderServiceClient)

        beforeEach {
            clearMocks(orderServiceClient)
        }

        describe("findById") {
            context("주문이 존재하는 경우") {
                it("OrderInfo를 반환한다") {
                    // given
                    val orderId = UUID.randomUUID()
                    val userId = UUID.randomUUID()
                    val productId = UUID.randomUUID()

                    val response =
                        OrderServiceGetOrderResponse(
                            orderId = orderId,
                            userId = userId,
                            orderNumber = "ORD-2024-001",
                            status = "ORDER_CONFIRMED",
                            totalAmount = BigDecimal("50000"),
                            items =
                                listOf(
                                    OrderServiceGetOrderItemResponse(
                                        productId = productId,
                                        productName = "테스트 상품",
                                        quantity = 2,
                                        unitPrice = BigDecimal("25000"),
                                    ),
                                ),
                        )

                    every { orderServiceClient.getOrder(orderId) } returns response

                    // when
                    val result = orderAdapter.findById(orderId)

                    // then
                    result.shouldNotBeNull()
                    result.orderId shouldBe orderId
                    result.orderNumber shouldBe "ORD-2024-001"
                    result.items.size shouldBe 1
                    result.items[0].productId shouldBe productId
                    result.items[0].productName shouldBe "테스트 상품"
                    result.items[0].quantity shouldBe 2
                    result.items[0].price shouldBe BigDecimal("25000")

                    verify(exactly = 1) { orderServiceClient.getOrder(orderId) }
                }
            }

            context("주문이 존재하지 않는 경우") {
                it("null을 반환한다") {
                    // given
                    val orderId = UUID.randomUUID()

                    every { orderServiceClient.getOrder(orderId) } throws
                        FeignClientException.NotFound("Order not found")

                    // when
                    val result = orderAdapter.findById(orderId)

                    // then
                    result.shouldBeNull()

                    verify(exactly = 1) { orderServiceClient.getOrder(orderId) }
                }
            }

            context("Order Service 호출 실패") {
                it("ServiceCallFailed 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()

                    every { orderServiceClient.getOrder(orderId) } throws
                        FeignClientException.InternalServerError("Internal server error")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.ServiceCallFailed> {
                            orderAdapter.findById(orderId)
                        }

                    exception.operation shouldBe "findById(orderId=$orderId)"
                    exception.cause shouldNotBeNull { }

                    verify(exactly = 1) { orderServiceClient.getOrder(orderId) }
                }
            }
        }

        describe("markOrderPaymentPending") {
            context("정상적으로 결제 대기 상태로 변경되는 경우") {
                it("예외 없이 완료된다") {
                    // given
                    val orderId = UUID.randomUUID()
                    val paymentId = UUID.randomUUID()

                    val response =
                        OrderServiceMarkPaymentPendingResponse(
                            orderId = orderId,
                            status = "PAYMENT_PENDING",
                            paymentId = paymentId,
                        )

                    every { orderServiceClient.markPaymentPending(orderId, any()) } returns response

                    // when
                    orderAdapter.markOrderPaymentPending(orderId, paymentId)

                    // then
                    verify(exactly = 1) { orderServiceClient.markPaymentPending(orderId, any()) }
                }
            }

            context("주문이 존재하지 않는 경우") {
                it("OrderNotFound 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()
                    val paymentId = UUID.randomUUID()

                    every { orderServiceClient.markPaymentPending(orderId, any()) } throws
                        FeignClientException.NotFound("Order not found")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.OrderNotFound> {
                            orderAdapter.markOrderPaymentPending(orderId, paymentId)
                        }

                    exception.orderId shouldBe orderId

                    verify(exactly = 1) { orderServiceClient.markPaymentPending(orderId, any()) }
                }
            }

            context("주문 상태가 결제 대기로 변경할 수 없는 경우") {
                it("InvalidOrderStatus 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()
                    val paymentId = UUID.randomUUID()

                    every { orderServiceClient.markPaymentPending(orderId, any()) } throws
                        FeignClientException.Conflict("Order status is not ORDER_CONFIRMED")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.InvalidOrderStatus> {
                            orderAdapter.markOrderPaymentPending(orderId, paymentId)
                        }

                    exception.orderId shouldBe orderId
                    // FeignClientException.Conflict의 message가 그대로 전달됨
                    exception.reason shouldBe "Order status is not ORDER_CONFIRMED"

                    verify(exactly = 1) { orderServiceClient.markPaymentPending(orderId, any()) }
                }
            }

            context("이미 결제가 연결된 주문인 경우") {
                it("InvalidOrderStatus 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()
                    val paymentId = UUID.randomUUID()

                    every { orderServiceClient.markPaymentPending(orderId, any()) } throws
                        FeignClientException.Conflict("Payment already exists for this order")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.InvalidOrderStatus> {
                            orderAdapter.markOrderPaymentPending(orderId, paymentId)
                        }

                    exception.orderId shouldBe orderId

                    verify(exactly = 1) { orderServiceClient.markPaymentPending(orderId, any()) }
                }
            }

            context("Order Service 호출 실패") {
                it("ServiceCallFailed 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()
                    val paymentId = UUID.randomUUID()

                    every { orderServiceClient.markPaymentPending(orderId, any()) } throws
                        FeignClientException.ServiceUnavailable("Service unavailable")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.ServiceCallFailed> {
                            orderAdapter.markOrderPaymentPending(orderId, paymentId)
                        }

                    exception.operation shouldBe "markOrderPaymentPending(orderId=$orderId, paymentId=$paymentId)"

                    verify(exactly = 1) { orderServiceClient.markPaymentPending(orderId, any()) }
                }
            }
        }

        describe("hasPayment") {
            context("결제가 연결되어 있지 않은 경우") {
                it("false를 반환한다") {
                    // given
                    val orderId = UUID.randomUUID()

                    every { orderServiceClient.hasPayment(orderId) } returns
                        OrderServiceHasPaymentResponse(hasPayment = false)

                    // when
                    val result = orderAdapter.hasPayment(orderId)

                    // then
                    result shouldBe false

                    verify(exactly = 1) { orderServiceClient.hasPayment(orderId) }
                }
            }

            context("결제가 연결되어 있는 경우") {
                it("true를 반환한다") {
                    // given
                    val orderId = UUID.randomUUID()

                    every { orderServiceClient.hasPayment(orderId) } returns
                        OrderServiceHasPaymentResponse(hasPayment = true)

                    // when
                    val result = orderAdapter.hasPayment(orderId)

                    // then
                    result shouldBe true

                    verify(exactly = 1) { orderServiceClient.hasPayment(orderId) }
                }
            }

            context("주문이 존재하지 않는 경우") {
                it("OrderNotFound 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()

                    every { orderServiceClient.hasPayment(orderId) } throws
                        FeignClientException.NotFound("Order not found")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.OrderNotFound> {
                            orderAdapter.hasPayment(orderId)
                        }

                    exception.orderId shouldBe orderId

                    verify(exactly = 1) { orderServiceClient.hasPayment(orderId) }
                }
            }

            context("Order Service 호출 실패") {
                it("ServiceCallFailed 예외를 던진다") {
                    // given
                    val orderId = UUID.randomUUID()

                    every { orderServiceClient.hasPayment(orderId) } throws
                        FeignClientException.InternalServerError("Internal server error")

                    // when & then
                    val exception =
                        shouldThrow<OrderServiceException.ServiceCallFailed> {
                            orderAdapter.hasPayment(orderId)
                        }

                    exception.operation shouldBe "hasPayment(orderId=$orderId)"

                    verify(exactly = 1) { orderServiceClient.hasPayment(orderId) }
                }
            }
        }

        // confirmStockReservation 메서드는 제거됨
        // 재고 예약 확정은 이벤트 기반으로 처리됩니다.
        // Payment Service → payment.completed 발행 → Product Service가 재고 확정
        // 참고: docs/INTEGRATION-v2.md
    })
