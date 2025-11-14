package com.groom.payment.application.service

import com.groom.payment.application.dto.CompletePaymentCommand
import com.groom.payment.common.annotation.UnitTest
import com.groom.payment.domain.event.PaymentCompletedEvent
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.IdempotencyPort
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.OrderPort
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
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.UUID

@UnitTest
class CompletePaymentServiceTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        Given("정상적인 결제 완료 요청") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val idempotencyPort = mockk<IdempotencyPort>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val orderPort = mockk<OrderPort>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                CompletePaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    idempotencyPort,
                    eventPublisher,
                    orderPort,
                    paymentEventFactory,
                )

            val pgApprovalNumber = "PG-APPROVAL-001"
            val idempotencyKey = "IDEMPOTENCY-001"

            // PaymentTestFixture로 PAYMENT_REQUEST 상태의 Payment 생성
            val payment = PaymentTestFixture.createPaymentRequest()
            val paymentId = payment.id
            val orderId = payment.orderId

            val command =
                CompletePaymentCommand(
                    paymentId = paymentId,
                    pgApprovalNumber = pgApprovalNumber,
                    idempotencyKey = idempotencyKey,
                )

            // PaymentLockManager.executeWithLock()이 람다를 실행하도록 설정
            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }

            every { idempotencyPort.ensureIdempotency(idempotencyKey, any()) } returns true
            every { loadPaymentPort.loadById(paymentId) } returns payment
            every { orderPort.confirmStockReservation(orderId) } just runs

            val publishedEventSlot = slot<PaymentCompletedEvent>()
            every { eventPublisher.publishEvent(capture(publishedEventSlot)) } just runs
            every { paymentEventFactory.createPaymentCompletedEvent(payment) } answers {
                PaymentCompletedEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    pgApprovalNumber = pgApprovalNumber,
                    completedAt = payment.completedAt!!,
                    occurredAt = payment.completedAt!!,
                )
            }

            When("execute 호출") {
                val result = service.execute(command)

                Then("결과가 정상적으로 반환되어야 한다") {
                    result.paymentId shouldBe paymentId
                    result.orderId shouldBe orderId
                    result.status shouldBe PaymentStatus.PAYMENT_COMPLETED
                    result.pgApprovalNumber shouldBe pgApprovalNumber
                    result.alreadyProcessed shouldBe false
                    result.completedAt shouldNotBe null
                }

                Then("Payment 상태가 PAYMENT_COMPLETED로 변경되어야 한다") {
                    payment.status shouldBe PaymentStatus.PAYMENT_COMPLETED
                    payment.pgApprovalNumber shouldBe pgApprovalNumber
                    payment.completedAt shouldNotBe null
                }

                Then("PaymentCompletedEvent가 올바른 내용으로 발행되어야 한다") {
                    val publishedEvent = publishedEventSlot.captured
                    publishedEvent.paymentId shouldBe paymentId
                    publishedEvent.orderId shouldBe orderId
                    publishedEvent.pgApprovalNumber shouldBe pgApprovalNumber
                    publishedEvent.occurredAt shouldNotBe null
                }
            }
        }

        Given("중복 결제 완료 요청 (멱등성)") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val idempotencyPort = mockk<IdempotencyPort>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val orderPort = mockk<OrderPort>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                CompletePaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    idempotencyPort,
                    eventPublisher,
                    orderPort,
                    paymentEventFactory,
                )

            val pgApprovalNumber = "PG-APPROVAL-002"
            val idempotencyKey = "IDEMPOTENCY-002"

            // PaymentTestFixture로 PAYMENT_COMPLETED 상태의 Payment 생성 (이미 완료됨)
            val completedPayment = PaymentTestFixture.createPaymentCompleted(pgApprovalNumber = pgApprovalNumber)
            val paymentId = completedPayment.id
            val orderId = completedPayment.orderId
            val originalCompletedAt = completedPayment.completedAt

            val command =
                CompletePaymentCommand(
                    paymentId = paymentId,
                    pgApprovalNumber = pgApprovalNumber,
                    idempotencyKey = idempotencyKey,
                )

            // PaymentLockManager.executeWithLock()이 람다를 실행하도록 설정
            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }

            every { idempotencyPort.ensureIdempotency(idempotencyKey, any()) } returns false // 중복 요청
            every { loadPaymentPort.loadById(paymentId) } returns completedPayment

            When("execute 호출") {
                val result = service.execute(command)

                Then("이미 처리된 결과를 반환해야 한다") {
                    result.paymentId shouldBe paymentId
                    result.orderId shouldBe orderId
                    result.status shouldBe PaymentStatus.PAYMENT_COMPLETED
                    result.alreadyProcessed shouldBe true
                }

                Then("Payment 상태가 변경되지 않아야 한다") {
                    completedPayment.status shouldBe PaymentStatus.PAYMENT_COMPLETED
                    completedPayment.completedAt shouldBe originalCompletedAt
                }
            }
        }

        Given("재고 예약 ID가 없는 주문의 결제 완료") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val idempotencyPort = mockk<IdempotencyPort>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val orderPort = mockk<OrderPort>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                CompletePaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    idempotencyPort,
                    eventPublisher,
                    orderPort,
                    paymentEventFactory,
                )

            val pgApprovalNumber = "PG-APPROVAL-003"
            val idempotencyKey = "IDEMPOTENCY-003"

            // PaymentTestFixture로 PAYMENT_REQUEST 상태의 Payment 생성
            val payment = PaymentTestFixture.createPaymentRequest(paymentAmount = BigDecimal("20000"))
            val paymentId = payment.id
            val orderId = payment.orderId

            val command =
                CompletePaymentCommand(
                    paymentId = paymentId,
                    pgApprovalNumber = pgApprovalNumber,
                    idempotencyKey = idempotencyKey,
                )

            // PaymentLockManager.executeWithLock()이 람다를 실행하도록 설정
            every { paymentLockManager.executeWithLock<Any>(any(), any()) } answers {
                secondArg<() -> Any>().invoke()
            }

            every { idempotencyPort.ensureIdempotency(idempotencyKey, any()) } returns true
            every { loadPaymentPort.loadById(paymentId) } returns payment
            every { orderPort.confirmStockReservation(orderId) } just runs

            val publishedEventSlot = slot<PaymentCompletedEvent>()
            every { eventPublisher.publishEvent(capture(publishedEventSlot)) } just runs
            every { paymentEventFactory.createPaymentCompletedEvent(payment) } answers {
                PaymentCompletedEvent(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    pgApprovalNumber = pgApprovalNumber,
                    completedAt = payment.completedAt!!,
                    occurredAt = payment.completedAt!!,
                )
            }

            When("execute 호출") {
                val result = service.execute(command)

                Then("결제는 정상적으로 완료되어야 한다") {
                    result.paymentId shouldBe paymentId
                    result.status shouldBe PaymentStatus.PAYMENT_COMPLETED
                    payment.status shouldBe PaymentStatus.PAYMENT_COMPLETED
                    payment.pgApprovalNumber shouldBe pgApprovalNumber
                }

                Then("PaymentCompletedEvent가 정상적으로 발행되어야 한다") {
                    val publishedEvent = publishedEventSlot.captured
                    publishedEvent.paymentId shouldBe paymentId
                    publishedEvent.orderId shouldBe orderId
                }
            }
        }

        Given("분산 락 획득 실패") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val paymentLockManager = mockk<PaymentLockManager>()
            val idempotencyPort = mockk<IdempotencyPort>()
            val eventPublisher = mockk<ApplicationEventPublisher>()
            val orderPort = mockk<OrderPort>()
            val paymentEventFactory = mockk<PaymentEventFactory>()

            val service =
                CompletePaymentService(
                    loadPaymentPort,
                    paymentLockManager,
                    idempotencyPort,
                    eventPublisher,
                    orderPort,
                    paymentEventFactory,
                )

            val paymentId = UUID.randomUUID()
            val command =
                CompletePaymentCommand(
                    paymentId = paymentId,
                    pgApprovalNumber = "PG-APPROVAL-004",
                    idempotencyKey = "IDEMPOTENCY-004",
                )

            // PaymentLockManager에서 락 획득 실패 시 예외 발생
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
