package com.groom.payment.application.service

import com.groom.payment.application.dto.CreatePaymentWaitCommand
import com.groom.payment.common.annotation.UnitTest
import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.SavePaymentPort
import com.groom.payment.fixture.PaymentTestFixture
import com.groom.platform.saga.SagaTrackerClient
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.util.UUID

@UnitTest
class CreatePaymentWaitServiceTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        Given("신규 결제 대기 생성") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val savePaymentPort = mockk<SavePaymentPort>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CreatePaymentWaitService(
                    loadPaymentPort,
                    savePaymentPort,
                    sagaTrackerClient,
                )

            val orderId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val totalAmount = BigDecimal("50000")

            val command =
                CreatePaymentWaitCommand(
                    eventId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    userId = userId,
                    totalAmount = totalAmount,
                )

            every { loadPaymentPort.loadByOrderId(orderId) } returns null

            val paymentSlot = slot<Payment>()
            every { savePaymentPort.save(capture(paymentSlot)) } answers {
                firstArg<Payment>()
            }

            When("execute 호출") {
                val result = service.execute(command)

                Then("결과가 정상적으로 반환되어야 한다") {
                    result.orderId shouldBe orderId
                    result.userId shouldBe userId
                    result.status shouldBe PaymentStatus.PAYMENT_WAIT
                    result.paymentId shouldNotBe null
                    result.createdAt shouldNotBe null
                }

                Then("Payment가 PAYMENT_WAIT 상태로 저장되어야 한다") {
                    val savedPayment = paymentSlot.captured
                    savedPayment.orderId shouldBe orderId
                    savedPayment.userId shouldBe userId
                    savedPayment.totalAmount shouldBe totalAmount
                    savedPayment.status shouldBe PaymentStatus.PAYMENT_WAIT
                }

                Then("savePaymentPort.save가 호출되어야 한다") {
                    verify(exactly = 1) { savePaymentPort.save(any()) }
                }
            }
        }

        Given("멱등성 - 이미 존재하는 주문에 대한 결제 대기") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val savePaymentPort = mockk<SavePaymentPort>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CreatePaymentWaitService(
                    loadPaymentPort,
                    savePaymentPort,
                    sagaTrackerClient,
                )

            val orderId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val totalAmount = BigDecimal("50000")

            val existingPayment =
                PaymentTestFixture.createPaymentWait(
                    orderId = orderId,
                    userId = userId,
                    paymentAmount = totalAmount,
                )

            val command =
                CreatePaymentWaitCommand(
                    eventId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    userId = userId,
                    totalAmount = totalAmount,
                )

            every { loadPaymentPort.loadByOrderId(orderId) } returns existingPayment

            When("execute 호출") {
                val result = service.execute(command)

                Then("기존 Payment 정보가 반환되어야 한다") {
                    result.paymentId shouldBe existingPayment.id
                    result.orderId shouldBe orderId
                    result.userId shouldBe userId
                    result.status shouldBe PaymentStatus.PAYMENT_WAIT
                }

                Then("savePaymentPort.save가 호출되지 않아야 한다") {
                    verify(exactly = 0) { savePaymentPort.save(any()) }
                }
            }
        }

        Given("멱등성 - 이미 PAYMENT_REQUEST 상태인 결제") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val savePaymentPort = mockk<SavePaymentPort>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CreatePaymentWaitService(
                    loadPaymentPort,
                    savePaymentPort,
                    sagaTrackerClient,
                )

            val orderId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val totalAmount = BigDecimal("50000")

            val existingPayment =
                PaymentTestFixture.createPaymentRequest(
                    orderId = orderId,
                    userId = userId,
                    paymentAmount = totalAmount,
                )

            val command =
                CreatePaymentWaitCommand(
                    eventId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    userId = userId,
                    totalAmount = totalAmount,
                )

            every { loadPaymentPort.loadByOrderId(orderId) } returns existingPayment

            When("execute 호출") {
                val result = service.execute(command)

                Then("기존 Payment 정보가 반환되어야 한다 (현재 상태 그대로)") {
                    result.paymentId shouldBe existingPayment.id
                    result.orderId shouldBe orderId
                    result.status shouldBe PaymentStatus.PAYMENT_REQUEST
                }

                Then("새로운 Payment가 생성되지 않아야 한다") {
                    verify(exactly = 0) { savePaymentPort.save(any()) }
                }
            }
        }

        Given("다른 주문에 대한 결제 대기 생성") {
            val loadPaymentPort = mockk<LoadPaymentPort>()
            val savePaymentPort = mockk<SavePaymentPort>()
            val sagaTrackerClient = mockk<SagaTrackerClient>(relaxed = true)

            val service =
                CreatePaymentWaitService(
                    loadPaymentPort,
                    savePaymentPort,
                    sagaTrackerClient,
                )

            val orderId1 = UUID.randomUUID()
            val orderId2 = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val totalAmount = BigDecimal("50000")

            val existingPayment =
                PaymentTestFixture.createPaymentWait(
                    orderId = orderId1,
                    userId = userId,
                    paymentAmount = totalAmount,
                )

            // orderId2에 대한 조회는 null 반환
            every { loadPaymentPort.loadByOrderId(orderId2) } returns null

            val paymentSlot = slot<Payment>()
            every { savePaymentPort.save(capture(paymentSlot)) } answers {
                firstArg<Payment>()
            }

            val command =
                CreatePaymentWaitCommand(
                    eventId = UUID.randomUUID().toString(),
                    orderId = orderId2,
                    userId = userId,
                    totalAmount = totalAmount,
                )

            When("execute 호출") {
                val result = service.execute(command)

                Then("새로운 Payment가 생성되어야 한다") {
                    result.orderId shouldBe orderId2
                    result.paymentId shouldNotBe existingPayment.id
                    result.status shouldBe PaymentStatus.PAYMENT_WAIT
                }

                Then("savePaymentPort.save가 호출되어야 한다") {
                    verify(exactly = 1) { savePaymentPort.save(any()) }
                    paymentSlot.captured.orderId shouldBe orderId2
                }
            }
        }
    })
