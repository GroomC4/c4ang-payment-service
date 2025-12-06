package com.groom.payment.adapter.inbound.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.payment.common.annotation.IntegrationTest
import com.groom.payment.common.config.NoOpEventPublisherConfig
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.SavePaymentPort
import com.groom.payment.fixture.PaymentTestFixture
import org.junit.jupiter.api.BeforeEach
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
import java.math.BigDecimal
import java.util.UUID

/**
 * PaymentCommandController 통합 테스트
 *
 * 결제 요청, 취소, 환불 요청 API의 응답 케이스 테스트
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
@DisplayName("PaymentCommandController 통합 테스트")
class PaymentCommandControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var savePaymentPort: SavePaymentPort

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("POST /api/v1/payments/request - 결제 요청")
    inner class RequestPayment {
        @Test
        @DisplayName("201 - PAYMENT_WAIT 상태에서 결제 요청 성공")
        fun shouldReturn201WhenPaymentRequestSucceeds() {
            // given
            val payment = PaymentTestFixture.createPaymentWait()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "paymentMethod" to "CARD",
                    "totalAmount" to 50000,
                    "paymentAmount" to 48000,
                    "discountAmount" to 2000,
                    "deliveryFee" to 3000,
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.orderId") { value(payment.orderId.toString()) }
                    jsonPath("$.status") { value("PAYMENT_REQUEST") }
                    jsonPath("$.pgTransactionId") { exists() }
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
                    "paymentMethod" to "CARD",
                    "totalAmount" to 50000,
                    "paymentAmount" to 50000,
                    "discountAmount" to 0,
                    "deliveryFee" to 0,
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("400 - PAYMENT_REQUEST 상태에서 다시 요청 시 실패")
        fun shouldReturn400WhenPaymentAlreadyRequested() {
            // given - 이미 PAYMENT_REQUEST 상태인 Payment
            val payment = PaymentTestFixture.createPaymentRequest()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "paymentMethod" to "CARD",
                    "totalAmount" to 50000,
                    "paymentAmount" to 50000,
                    "discountAmount" to 0,
                    "deliveryFee" to 0,
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // require → IllegalArgumentException → 400
                }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{paymentId}/cancel - 결제 취소")
    inner class CancelPayment {
        @Test
        @DisplayName("200 - PAYMENT_WAIT 상태에서 취소 성공")
        fun shouldReturn200WhenCancelFromPaymentWait() {
            // given
            val payment = PaymentTestFixture.createPaymentWait()
            savePaymentPort.save(payment)

            val request = mapOf("paymentId" to payment.id.toString(), "reason" to "사용자 취소 요청")

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/cancel") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.previousStatus") { value("PAYMENT_WAIT") }
                    jsonPath("$.currentStatus") { value("PAYMENT_CANCELLED") }
                    jsonPath("$.reason") { value("사용자 취소 요청") }
                }
        }

        @Test
        @DisplayName("200 - PAYMENT_REQUEST 상태에서 취소 성공")
        fun shouldReturn200WhenCancelFromPaymentRequest() {
            // given
            val payment = PaymentTestFixture.createPaymentRequest()
            savePaymentPort.save(payment)

            val request = mapOf("paymentId" to payment.id.toString(), "reason" to "타임아웃")

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/cancel") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.previousStatus") { value("PAYMENT_REQUEST") }
                    jsonPath("$.currentStatus") { value("PAYMENT_CANCELLED") }
                }
        }

        @Test
        @DisplayName("400 - 존재하지 않는 Payment ID로 취소 시 실패")
        fun shouldReturn400WhenPaymentNotFound() {
            // given
            val nonExistentPaymentId = UUID.randomUUID()
            val request = mapOf("paymentId" to nonExistentPaymentId.toString(), "reason" to "취소 요청")

            // when & then
            mockMvc
                .post("/api/v1/payments/$nonExistentPaymentId/cancel") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("400 - PAYMENT_COMPLETED 상태에서 취소 시 실패")
        fun shouldReturn400WhenCancelFromPaymentCompleted() {
            // given - 이미 완료된 Payment
            val payment = PaymentTestFixture.createPaymentCompleted()
            savePaymentPort.save(payment)

            val request = mapOf("paymentId" to payment.id.toString(), "reason" to "취소 요청")

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/cancel") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // require → IllegalArgumentException → 400
                }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{paymentId}/refund/request - 환불 요청")
    inner class RequestRefund {
        @Test
        @DisplayName("202 - PAYMENT_COMPLETED 상태에서 환불 요청 성공")
        fun shouldReturn202WhenRefundRequestSucceeds() {
            // given
            val payment = PaymentTestFixture.createPaymentCompleted(paymentAmount = BigDecimal("50000"))
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "refundAmount" to 50000,
                    "reason" to "상품 불량",
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/refund/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isAccepted() }
                    jsonPath("$.paymentId") { value(payment.id.toString()) }
                    jsonPath("$.status") { value("REFUND_REQUESTED") }
                    jsonPath("$.refundAmount") { value(50000) }
                    jsonPath("$.reason") { value("상품 불량") }
                }
        }

        @Test
        @DisplayName("202 - 부분 환불 요청 성공")
        fun shouldReturn202WhenPartialRefundRequestSucceeds() {
            // given
            val payment = PaymentTestFixture.createPaymentCompleted(paymentAmount = BigDecimal("50000"))
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "refundAmount" to 30000,
                    "reason" to "부분 반품",
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/refund/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isAccepted() }
                    jsonPath("$.refundAmount") { value(30000) }
                }
        }

        @Test
        @DisplayName("400 - 존재하지 않는 Payment ID로 환불 요청 시 실패")
        fun shouldReturn400WhenPaymentNotFound() {
            // given
            val nonExistentPaymentId = UUID.randomUUID()
            val request =
                mapOf(
                    "paymentId" to nonExistentPaymentId.toString(),
                    "refundAmount" to 50000,
                    "reason" to "환불 요청",
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/$nonExistentPaymentId/refund/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("400 - PAYMENT_WAIT 상태에서 환불 요청 시 실패")
        fun shouldReturn400WhenRefundFromPaymentWait() {
            // given - 아직 결제 완료되지 않은 Payment
            val payment = PaymentTestFixture.createPaymentWait()
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "refundAmount" to 50000,
                    "reason" to "환불 요청",
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/refund/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // require → IllegalArgumentException → 400
                }
        }

        @Test
        @DisplayName("400 - 결제 금액보다 큰 환불 요청 시 실패")
        fun shouldReturn400WhenRefundAmountExceedsPaymentAmount() {
            // given
            val payment = PaymentTestFixture.createPaymentCompleted(paymentAmount = BigDecimal("50000"))
            savePaymentPort.save(payment)

            val request =
                mapOf(
                    "paymentId" to payment.id.toString(),
                    "refundAmount" to 100000, // 결제 금액보다 큼
                    "reason" to "환불 요청",
                )

            // when & then
            mockMvc
                .post("/api/v1/payments/${payment.id}/refund/request") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() } // IllegalArgumentException → 400
                }
        }
    }
}
