package com.groom.payment.application.service

import com.groom.payment.application.dto.RequestPaymentCommand
import com.groom.payment.common.annotation.UnitTest
import com.groom.payment.domain.event.PaymentRequestedEvent
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.PaymentGatewayPort
import com.groom.payment.domain.port.PgRequestResult
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import com.groom.payment.fixture.PaymentTestFixture
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@UnitTest
class RequestPaymentServiceTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        Given("정상적인 결제 요청") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentGatewayPort = mockk<PaymentGatewayPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                RequestPaymentService(
                    loadPaymentPort,
                    paymentGatewayPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                )

            val payment = PaymentTestFixture.createPaymentWait()
            val paymentId = payment.id
            val orderId = payment.orderId

            val command =
                RequestPaymentCommand(
                    paymentId = paymentId,
                    paymentMethod = PaymentMethod.CARD,
                    totalAmount = BigDecimal("50000"),
                    paymentAmount = BigDecimal("48000"),
                    discountAmount = BigDecimal("2000"),
                    deliveryFee = BigDecimal("3000"),
                )

            val pgTransactionId = "PG-TXN-${UUID.randomUUID()}"
            val pgUrl = "https://pg.example.com/pay/$paymentId"
            val expiresAt = LocalDateTime.now().plusMinutes(10)

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }
            every { loadPaymentPort.loadById(paymentId) } returns payment
            every { paymentGatewayPort.requestPayment(paymentId, command.paymentAmount, any()) } returns
                PgRequestResult(
                    pgTransactionId = pgTransactionId,
                    paymentUrl = pgUrl,
                    expiresAt = expiresAt,
                )

            val eventSlot = slot<PaymentRequestedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } just runs
            every { paymentEventFactory.createPaymentRequestedEvent(payment) } answers {
                PaymentRequestedEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    pgTransactionId = payment.pgTransactionId!!,
                    occurredAt = LocalDateTime.now(),
                )
            }

            When("execute 호출") {
                val result = service.execute(command)

                Then("결과가 정상적으로 반환되어야 한다") {
                    result.paymentId shouldBe paymentId
                    result.orderId shouldBe orderId
                    result.status shouldBe PaymentStatus.PAYMENT_REQUEST
                    result.pgTransactionId shouldBe pgTransactionId
                    result.pgUrl shouldBe pgUrl
                    result.occurredAt shouldNotBe null
                }

                Then("Payment 상태가 PAYMENT_REQUEST로 변경되어야 한다") {
                    payment.status shouldBe PaymentStatus.PAYMENT_REQUEST
                    payment.pgTransactionId shouldBe pgTransactionId
                    payment.method shouldBe PaymentMethod.CARD
                    payment.totalAmount shouldBe BigDecimal("50000")
                    payment.paymentAmount shouldBe BigDecimal("48000")
                }

                Then("PG 게이트웨이가 호출되어야 한다") {
                    verify(exactly = 1) {
                        paymentGatewayPort.requestPayment(
                            paymentId,
                            command.paymentAmount,
                            any(),
                        )
                    }
                }

                Then("PaymentRequestedEvent가 발행되어야 한다") {
                    val publishedEvent = eventSlot.captured
                    publishedEvent.paymentId shouldBe paymentId
                    publishedEvent.orderId shouldBe orderId
                }
            }
        }

        Given("존재하지 않는 Payment") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentGatewayPort = mockk<PaymentGatewayPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                RequestPaymentService(
                    loadPaymentPort,
                    paymentGatewayPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                )

            val paymentId = UUID.randomUUID()
            val command =
                RequestPaymentCommand(
                    paymentId = paymentId,
                    paymentMethod = PaymentMethod.CARD,
                    totalAmount = BigDecimal("50000"),
                    paymentAmount = BigDecimal("50000"),
                    discountAmount = BigDecimal.ZERO,
                    deliveryFee = BigDecimal.ZERO,
                )

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }
            every { loadPaymentPort.loadById(paymentId) } returns null

            When("execute 호출") {
                Then("IllegalArgumentException이 발생해야 한다") {
                    try {
                        service.execute(command)
                        throw AssertionError("Expected IllegalArgumentException")
                    } catch (e: IllegalArgumentException) {
                        e.message shouldBe "Payment not found: $paymentId"
                    }
                }
            }
        }

        Given("이미 PAYMENT_REQUEST 상태인 Payment") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentGatewayPort = mockk<PaymentGatewayPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                RequestPaymentService(
                    loadPaymentPort,
                    paymentGatewayPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                )

            val payment = PaymentTestFixture.createPaymentRequest()
            val paymentId = payment.id

            val command =
                RequestPaymentCommand(
                    paymentId = paymentId,
                    paymentMethod = PaymentMethod.CARD,
                    totalAmount = BigDecimal("50000"),
                    paymentAmount = BigDecimal("50000"),
                    discountAmount = BigDecimal.ZERO,
                    deliveryFee = BigDecimal.ZERO,
                )

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }
            every { loadPaymentPort.loadById(paymentId) } returns payment
            every { paymentGatewayPort.requestPayment(paymentId, any(), any()) } returns
                PgRequestResult(
                    pgTransactionId = "PG-TXN-NEW",
                    paymentUrl = "https://pg.example.com/pay/$paymentId",
                    expiresAt = LocalDateTime.now().plusMinutes(10),
                )

            When("execute 호출") {
                Then("IllegalArgumentException이 발생해야 한다 (이미 PAYMENT_REQUEST 상태)") {
                    try {
                        service.execute(command)
                        throw AssertionError("Expected IllegalArgumentException")
                    } catch (e: IllegalArgumentException) {
                        e.message shouldBe "Only PAYMENT_WAIT payments can be requested"
                    }
                }
            }
        }

        Given("분산 락 획득 실패") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentGatewayPort = mockk<PaymentGatewayPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                RequestPaymentService(
                    loadPaymentPort,
                    paymentGatewayPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                )

            val paymentId = UUID.randomUUID()
            val command =
                RequestPaymentCommand(
                    paymentId = paymentId,
                    paymentMethod = PaymentMethod.CARD,
                    totalAmount = BigDecimal("50000"),
                    paymentAmount = BigDecimal("50000"),
                    discountAmount = BigDecimal.ZERO,
                    deliveryFee = BigDecimal.ZERO,
                )

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } throws
                IllegalStateException("Failed to acquire lock for payment: $paymentId")

            When("execute 호출") {
                Then("IllegalStateException이 발생해야 한다") {
                    try {
                        service.execute(command)
                        throw AssertionError("Expected IllegalStateException")
                    } catch (e: IllegalStateException) {
                        e.message shouldBe "Failed to acquire lock for payment: $paymentId"
                    }
                }
            }
        }
    })
