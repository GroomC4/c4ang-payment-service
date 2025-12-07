package com.groom.payment.application.service

import com.groom.payment.application.dto.CancelPaymentCommand
import com.groom.payment.common.annotation.UnitTest
import com.groom.payment.domain.event.PaymentCancelledEvent
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import com.groom.payment.fixture.PaymentTestFixture
import com.groom.platform.saga.SagaTrackerClient
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.util.UUID

@UnitTest
class CancelPaymentServiceTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        Given("PAYMENT_WAIT 상태의 결제 취소") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CancelPaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                    sagaTrackerClient,
                )

            val payment = PaymentTestFixture.createPaymentWait()
            val paymentId = payment.id
            val orderId = payment.orderId
            val reason = "사용자 취소 요청"

            val command =
                CancelPaymentCommand(
                    paymentId = paymentId,
                    reason = reason,
                )

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }
            every { loadPaymentPort.loadById(paymentId) } returns payment

            val eventSlot = slot<PaymentCancelledEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } just runs
            every { paymentEventFactory.createPaymentCancelledEvent(payment, any(), reason) } answers {
                PaymentCancelledEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    userId = payment.userId,
                    previousStatus = arg(1),
                    reason = reason,
                    occurredAt = LocalDateTime.now(),
                )
            }

            When("execute 호출") {
                val result = service.execute(command)

                Then("결과가 정상적으로 반환되어야 한다") {
                    result.paymentId shouldBe paymentId
                    result.orderId shouldBe orderId
                    result.previousStatus shouldBe PaymentStatus.PAYMENT_WAIT
                    result.currentStatus shouldBe PaymentStatus.PAYMENT_CANCELLED
                    result.reason shouldBe reason
                    result.cancelledAt shouldNotBe null
                }

                Then("Payment 상태가 PAYMENT_CANCELLED로 변경되어야 한다") {
                    payment.status shouldBe PaymentStatus.PAYMENT_CANCELLED
                    payment.cancelledAt shouldNotBe null
                }

                Then("PaymentCancelledEvent가 발행되어야 한다") {
                    val publishedEvent = eventSlot.captured
                    publishedEvent.paymentId shouldBe paymentId
                    publishedEvent.orderId shouldBe orderId
                    publishedEvent.previousStatus shouldBe PaymentStatus.PAYMENT_WAIT
                    publishedEvent.reason shouldBe reason
                }
            }
        }

        Given("PAYMENT_REQUEST 상태의 결제 취소") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CancelPaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                    sagaTrackerClient,
                )

            val payment = PaymentTestFixture.createPaymentRequest()
            val paymentId = payment.id
            val orderId = payment.orderId
            val reason = "타임아웃"

            val command =
                CancelPaymentCommand(
                    paymentId = paymentId,
                    reason = reason,
                )

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }
            every { loadPaymentPort.loadById(paymentId) } returns payment

            val eventSlot = slot<PaymentCancelledEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } just runs
            every { paymentEventFactory.createPaymentCancelledEvent(payment, any(), reason) } answers {
                PaymentCancelledEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    userId = payment.userId,
                    previousStatus = arg(1),
                    reason = reason,
                    occurredAt = LocalDateTime.now(),
                )
            }

            When("execute 호출") {
                val result = service.execute(command)

                Then("결과가 정상적으로 반환되어야 한다") {
                    result.paymentId shouldBe paymentId
                    result.orderId shouldBe orderId
                    result.previousStatus shouldBe PaymentStatus.PAYMENT_REQUEST
                    result.currentStatus shouldBe PaymentStatus.PAYMENT_CANCELLED
                }

                Then("Payment 상태가 PAYMENT_CANCELLED로 변경되어야 한다") {
                    payment.status shouldBe PaymentStatus.PAYMENT_CANCELLED
                }
            }
        }

        Given("존재하지 않는 Payment") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CancelPaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                    sagaTrackerClient,
                )

            val paymentId = UUID.randomUUID()
            val command =
                CancelPaymentCommand(
                    paymentId = paymentId,
                    reason = "취소 요청",
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

        Given("PAYMENT_COMPLETED 상태의 결제 취소 시도") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CancelPaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                    sagaTrackerClient,
                )

            val payment = PaymentTestFixture.createPaymentCompleted()
            val paymentId = payment.id
            val command =
                CancelPaymentCommand(
                    paymentId = paymentId,
                    reason = "취소 요청",
                )

            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }
            every { loadPaymentPort.loadById(paymentId) } returns payment

            When("execute 호출") {
                Then("IllegalArgumentException이 발생해야 한다") {
                    try {
                        service.execute(command)
                        throw AssertionError("Expected IllegalArgumentException")
                    } catch (e: IllegalArgumentException) {
                        e.message shouldBe "Cannot cancel payment in status: PAYMENT_COMPLETED"
                    }
                }
            }
        }

        Given("분산 락 획득 실패") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val paymentEventFactory = mockk<PaymentEventFactory>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CancelPaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    eventPublisher,
                    paymentEventFactory,
                    sagaTrackerClient,
                )

            val paymentId = UUID.randomUUID()
            val command =
                CancelPaymentCommand(
                    paymentId = paymentId,
                    reason = "취소 요청",
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
