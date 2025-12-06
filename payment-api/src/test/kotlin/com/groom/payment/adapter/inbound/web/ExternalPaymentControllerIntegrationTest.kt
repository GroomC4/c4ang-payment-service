package com.groom.payment.adapter.inbound.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.payment.common.annotation.IntegrationTest
import com.groom.payment.common.config.NoOpEventPublisherConfig
import com.groom.payment.domain.port.SavePaymentPort
import com.groom.payment.fixture.PaymentTestFixture
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ExternalPaymentController 통합 테스트
 *
 * PG 콜백 API의 응답 케이스 테스트
 */
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@Import(NoOpEventPublisherConfig::class)
@TestPropertySource(
    properties = [
        "feign.clients.order-service.url=http://localhost:\${wiremock.server.port}",
    ],
)
@Transactional
@DisplayName("ExternalPaymentController 통합 테스트")
class ExternalPaymentControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var savePaymentPort: SavePaymentPort

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("POST /external/pg/callback/payment/complete - 결제 완료 콜백")
    inner class CompletePayment {
        @Test
        @DisplayName("200 - 결제 완료 성공")
        fun shouldReturn200WhenPaymentCompleteSucceeds() {
            // given
            val payment = PaymentTestFixture.createPaymentRequest()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "pgApprovalNumber" to "APPROVAL-12345678",
                    "idempotencyKey" to "PG-TXN-${UUID.randomUUID()}",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/complete") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.orderId") { value(payment.orderId.toString()) }
                    jsonPath("$.status") { value("PAYMENT_COMPLETED") }
                    jsonPath("$.pgApprovalNumber") { value("APPROVAL-12345678") }
                    jsonPath("$.alreadyProcessed") { value(false) }
                    jsonPath("$.completedAt") { exists() }
                }
        }

        @Test
        @DisplayName("200 - 멱등성: 동일한 idempotencyKey로 중복 요청 시 alreadyProcessed=true")
        fun shouldReturn200WithAlreadyProcessedWhenDuplicateRequest() {
            // given - PAYMENT_REQUEST 상태의 Payment
            val payment = PaymentTestFixture.createPaymentRequest()
            savePaymentPort.save(payment)

            val idempotencyKey = "PG-TXN-DUPLICATE-${UUID.randomUUID()}"

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "pgApprovalNumber" to "APPROVAL-12345678",
                    "idempotencyKey" to idempotencyKey,
                )

            // 첫 번째 요청 - 결제 완료 처리
            mockMvc
                .post("/external/pg/callback/payment/complete") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.alreadyProcessed") { value(false) }
                    jsonPath("$.status") { value("PAYMENT_COMPLETED") }
                }

            // 두 번째 요청 - 동일한 idempotencyKey로 중복 요청
            mockMvc
                .post("/external/pg/callback/payment/complete") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.alreadyProcessed") { value(true) }
                }
        }

        @Test
        @DisplayName("400 - 존재하지 않는 Payment ID로 요청 시 실패")
        fun shouldReturn400WhenPaymentNotFound() {
            // given
            val nonExistentPaymentId = UUID.randomUUID()
            val request =
                mapOf(
                    "paymentId" to nonExistentPaymentId.toString(),
                    "pgApprovalNumber" to "APPROVAL-12345678",
                    "idempotencyKey" to "PG-TXN-${UUID.randomUUID()}",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/complete") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("400 - PAYMENT_WAIT 상태에서 완료 요청 시 실패")
        fun shouldReturn400WhenPaymentNotInRequestState() {
            // given - PAYMENT_WAIT 상태 (아직 PAYMENT_REQUEST가 아님)
            val payment = PaymentTestFixture.createPaymentWait()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "pgApprovalNumber" to "APPROVAL-12345678",
                    "idempotencyKey" to "PG-TXN-${UUID.randomUUID()}",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/complete") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // require → IllegalArgumentException → 400
                }
        }
    }

    @Nested
    @DisplayName("POST /external/pg/callback/payment/refund - 환불 완료 콜백")
    inner class CompleteRefund {
        @Test
        @DisplayName("200 - 환불 완료 성공")
        fun shouldReturn200WhenRefundCompleteSucceeds() {
            // given - REFUND_REQUESTED 상태
            val payment = PaymentTestFixture.createRefundRequested()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "pgApprovalNumber" to "REFUND-TXN-12345678",
                    "idempotencyKey" to "PG-REFUND-${UUID.randomUUID()}",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/refund") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.orderId") { value(payment.orderId.toString()) }
                    jsonPath("$.status") { value("REFUND_COMPLETED") }
                    jsonPath("$.pgApprovalNumber") { value("REFUND-TXN-12345678") }
                    jsonPath("$.alreadyProcessed") { value(false) }
                    jsonPath("$.completedAt") { exists() }
                }
        }

        @Test
        @DisplayName("400 - 존재하지 않는 Payment ID로 요청 시 실패")
        fun shouldReturn400WhenPaymentNotFound() {
            // given
            val nonExistentPaymentId = UUID.randomUUID()
            val request =
                mapOf(
                    "paymentId" to nonExistentPaymentId.toString(),
                    "pgApprovalNumber" to "REFUND-TXN-12345678",
                    "idempotencyKey" to "PG-REFUND-${UUID.randomUUID()}",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/refund") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("400 - PAYMENT_COMPLETED 상태에서 환불 완료 요청 시 실패")
        fun shouldReturn400WhenPaymentNotInRefundRequestedState() {
            // given - PAYMENT_COMPLETED 상태 (REFUND_REQUESTED가 아님)
            val payment = PaymentTestFixture.createPaymentCompleted()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "pgApprovalNumber" to "REFUND-TXN-12345678",
                    "idempotencyKey" to "PG-REFUND-${UUID.randomUUID()}",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/refund") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // require → IllegalArgumentException → 400
                }
        }
    }

    @Nested
    @DisplayName("POST /external/pg/callback/payment/fail - 결제 실패 콜백")
    inner class MarkFailed {
        @Test
        @DisplayName("200 - 결제 실패 처리 성공")
        fun shouldReturn200WhenPaymentFailSucceeds() {
            // given - PAYMENT_REQUEST 상태
            val payment = PaymentTestFixture.createPaymentRequest()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "reason" to "카드 한도 초과",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/fail") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.orderId") { value(payment.orderId.toString()) }
                    jsonPath("$.currentStatus") { value("PAYMENT_FAILED") }
                    jsonPath("$.reason") { value("카드 한도 초과") }
                }
        }

        @Test
        @DisplayName("400 - 존재하지 않는 Payment ID로 요청 시 실패")
        fun shouldReturn400WhenPaymentNotFound() {
            // given
            val nonExistentPaymentId = UUID.randomUUID()
            val request =
                mapOf(
                    "paymentId" to nonExistentPaymentId.toString(),
                    "reason" to "결제 실패",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/fail") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("400 - PAYMENT_WAIT 상태에서 실패 처리 요청 시 실패")
        fun shouldReturn400WhenPaymentNotInRequestState() {
            // given - PAYMENT_WAIT 상태 (아직 PAYMENT_REQUEST가 아님)
            val payment = PaymentTestFixture.createPaymentWait()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "reason" to "결제 실패",
                )

            // when & then
            mockMvc
                .post("/external/pg/callback/payment/fail") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // require → IllegalArgumentException → 400
                }
        }
    }
}
