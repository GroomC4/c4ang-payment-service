package com.groom.payment.adapter.inbound.web

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.groom.payment.common.annotation.IntegrationTest
import com.groom.payment.common.config.NoOpEventPublisherConfig
import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentMethod
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * PaymentQueryController 통합 테스트
 *
 * Order Service 연동 시나리오를 포함한 결제 조회 API 테스트
 *
 * WireMock을 사용하여 Order Service 응답을 시뮬레이션합니다.
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
@DisplayName("PaymentQueryController 통합 테스트")
class PaymentQueryControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var savePaymentPort: SavePaymentPort

    private lateinit var testPayment: Payment
    private lateinit var testOrderId: UUID
    private lateinit var testUserId: UUID

    @BeforeEach
    fun setup() {
        // 테스트 데이터 준비
        testOrderId = UUID.randomUUID()
        testUserId = UUID.randomUUID()

        // Payment 생성 및 저장 (PAYMENT_WAIT 상태)
        testPayment =
            PaymentTestFixture.createPaymentWait(
                orderId = testOrderId,
                userId = testUserId,
            )
        savePaymentPort.save(testPayment)
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{paymentId}")
    inner class GetPayment {
        @Test
        @DisplayName("주문 정보가 있는 경우 - 결제 상세와 주문 정보를 함께 반환한다")
        fun shouldReturnPaymentWithOrderInfo() {
            // given - WireMock으로 Order Service 응답 설정
            stubFor(
                get(urlPathEqualTo("/internal/v1/orders/$testOrderId"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                """
                                {
                                    "orderId": "$testOrderId",
                                    "userId": "$testUserId",
                                    "orderNumber": "ORD-2024-001",
                                    "status": "ORDER_CONFIRMED",
                                    "totalAmount": 50000,
                                    "items": [
                                        {
                                            "productId": "${UUID.randomUUID()}",
                                            "productName": "테스트 상품",
                                            "quantity": 1,
                                            "unitPrice": 50000
                                        }
                                    ]
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            // when & then
            mockMvc
                .get("/api/v1/payments/${testPayment.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(testPayment.id.toString()) }
                    jsonPath("$.orderId") { value(testOrderId.toString()) }
                    jsonPath("$.status") { value("PAYMENT_WAIT") }
                    // 주문 정보 검증
                    jsonPath("$.orderInfo.orderId") { value(testOrderId.toString()) }
                    jsonPath("$.orderInfo.orderNumber") { value("ORD-2024-001") }
                    jsonPath("$.orderInfo.items") { isArray() }
                }
        }

        @Test
        @DisplayName("주문 정보가 없는 경우 - 결제 상세만 반환하고 orderInfo는 null이다")
        fun shouldReturnPaymentWithoutOrderInfoWhenOrderNotFound() {
            // given - Order Service에서 404 반환
            stubFor(
                get(urlPathEqualTo("/internal/v1/orders/$testOrderId"))
                    .willReturn(
                        aResponse()
                            .withStatus(404)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                """
                                {
                                    "code": "ORDER_NOT_FOUND",
                                    "message": "Order not found: $testOrderId"
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            // when & then
            mockMvc
                .get("/api/v1/payments/${testPayment.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(testPayment.id.toString()) }
                    jsonPath("$.orderId") { value(testOrderId.toString()) }
                    jsonPath("$.status") { value("PAYMENT_WAIT") }
                    // 주문 정보가 null
                    jsonPath("$.orderInfo") { doesNotExist() }
                }
        }

        @Test
        @DisplayName("Order Service 장애 시 - 503 에러를 반환한다")
        fun shouldReturn503WhenOrderServiceUnavailable() {
            // given - Order Service 장애 시뮬레이션 (500 에러)
            stubFor(
                get(urlPathEqualTo("/internal/v1/orders/$testOrderId"))
                    .willReturn(
                        aResponse()
                            .withStatus(500)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                """
                                {
                                    "code": "INTERNAL_SERVER_ERROR",
                                    "message": "Order Service is temporarily unavailable"
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            // when & then
            mockMvc
                .get("/api/v1/payments/${testPayment.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isServiceUnavailable() }
                    jsonPath("$.code") { value("ORDER_SERVICE_CALL_FAILED") }
                }
        }

        @Test
        @DisplayName("결제가 존재하지 않는 경우 - 400 에러를 반환한다")
        fun shouldReturn400WhenPaymentNotFound() {
            // given
            val nonExistentPaymentId = UUID.randomUUID()

            // when & then
            mockMvc
                .get("/api/v1/payments/$nonExistentPaymentId") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        @DisplayName("이력 포함 조회 - 주문 정보와 결제 이력을 함께 반환한다")
        fun shouldReturnPaymentWithHistoryAndOrderInfo() {
            // given - WireMock으로 Order Service 응답 설정
            stubFor(
                get(urlPathEqualTo("/internal/v1/orders/$testOrderId"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                """
                                {
                                    "orderId": "$testOrderId",
                                    "userId": "$testUserId",
                                    "orderNumber": "ORD-2024-002",
                                    "status": "ORDER_CONFIRMED",
                                    "totalAmount": 50000,
                                    "items": []
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            // when & then
            mockMvc
                .get("/api/v1/payments/${testPayment.id}?includeHistory=true") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.paymentId") { value(testPayment.id.toString()) }
                    jsonPath("$.orderInfo.orderNumber") { value("ORD-2024-002") }
                    // history는 빈 배열일 수 있음 (아직 이력이 없는 경우)
                    jsonPath("$.history") { isArray() }
                }
        }
    }

    @Nested
    @DisplayName("결제 상태별 주문 정보 조회")
    inner class PaymentStatusWithOrderInfo {
        @Test
        @DisplayName("PAYMENT_REQUEST 상태 - 주문 정보와 함께 결제 금액 정보도 반환한다")
        fun shouldReturnPaymentRequestWithOrderInfo() {
            // given - Payment 상태를 PAYMENT_REQUEST로 변경
            testPayment.requestPayment(
                pgTransactionId = "PG-TXN-001",
                totalAmount = BigDecimal("50000"),
                paymentAmount = BigDecimal("48000"),
                discountAmount = BigDecimal("2000"),
                deliveryFee = BigDecimal("3000"),
                method = PaymentMethod.CARD,
            )
            savePaymentPort.save(testPayment)

            // WireMock으로 Order Service 응답 설정
            stubFor(
                get(urlPathEqualTo("/internal/v1/orders/$testOrderId"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                """
                                {
                                    "orderId": "$testOrderId",
                                    "userId": "$testUserId",
                                    "orderNumber": "ORD-2024-003",
                                    "status": "PAYMENT_PENDING",
                                    "totalAmount": 50000,
                                    "items": []
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            // when & then
            mockMvc
                .get("/api/v1/payments/${testPayment.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("PAYMENT_REQUEST") }
                    jsonPath("$.totalAmount") { value(50000) }
                    jsonPath("$.paymentAmount") { value(48000) }
                    jsonPath("$.discountAmount") { value(2000) }
                    jsonPath("$.deliveryFee") { value(3000) }
                    jsonPath("$.method") { value("CARD") }
                    jsonPath("$.orderInfo.orderNumber") { value("ORD-2024-003") }
                }
        }

        @Test
        @DisplayName("PAYMENT_COMPLETED 상태 - 완료 시각과 주문 정보를 함께 반환한다")
        fun shouldReturnCompletedPaymentWithOrderInfo() {
            // given - Payment 상태를 PAYMENT_COMPLETED로 변경
            testPayment.requestPayment(
                pgTransactionId = "PG-TXN-002",
                totalAmount = BigDecimal("50000"),
                paymentAmount = BigDecimal("50000"),
                discountAmount = BigDecimal.ZERO,
                deliveryFee = BigDecimal.ZERO,
                method = PaymentMethod.CARD,
            )
            testPayment.complete("PG-APPROVAL-001")
            savePaymentPort.save(testPayment)

            // WireMock으로 Order Service 응답 설정
            stubFor(
                get(urlPathEqualTo("/internal/v1/orders/$testOrderId"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                """
                                {
                                    "orderId": "$testOrderId",
                                    "userId": "$testUserId",
                                    "orderNumber": "ORD-2024-004",
                                    "status": "PAYMENT_COMPLETED",
                                    "totalAmount": 50000,
                                    "items": []
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            // when & then
            mockMvc
                .get("/api/v1/payments/${testPayment.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("PAYMENT_COMPLETED") }
                    jsonPath("$.pgApprovalNumber") { value("PG-APPROVAL-001") }
                    jsonPath("$.completedAt") { exists() }
                    jsonPath("$.orderInfo.orderNumber") { value("ORD-2024-004") }
                }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments - 결제 목록 조회")
    inner class ListPayments {
        @Test
        @DisplayName("200 - 사용자별 결제 목록 조회 성공")
        fun shouldReturnPaymentListForUser() {
            // given - 추가 Payment 생성
            val payment2 =
                PaymentTestFixture.createPaymentCompleted(
                    orderId = UUID.randomUUID(),
                    userId = testUserId,
                )
            savePaymentPort.save(payment2)

            // when & then
            mockMvc
                .get("/api/v1/payments") {
                    param("userId", testUserId.toString())
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.payments") { isArray() }
                    jsonPath("$.payments.length()") { value(2) }
                    jsonPath("$.pagination.page") { value(1) }
                    jsonPath("$.pagination.total") { value(2) }
                }
        }

        @Test
        @DisplayName("200 - 상태 필터링 적용")
        fun shouldReturnFilteredPaymentsByStatus() {
            // given - PAYMENT_COMPLETED 상태의 Payment 추가
            val completedPayment =
                PaymentTestFixture.createPaymentCompleted(
                    orderId = UUID.randomUUID(),
                    userId = testUserId,
                )
            savePaymentPort.save(completedPayment)

            // when & then - PAYMENT_COMPLETED 상태만 조회
            mockMvc
                .get("/api/v1/payments") {
                    param("userId", testUserId.toString())
                    param("status", "PAYMENT_COMPLETED")
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.payments") { isArray() }
                    jsonPath("$.payments.length()") { value(1) }
                    jsonPath("$.payments[0].status") { value("PAYMENT_COMPLETED") }
                }
        }

        @Test
        @DisplayName("200 - 페이지네이션 적용")
        fun shouldReturnPaginatedPayments() {
            // given - 추가 Payment 3개 생성 (총 4개)
            for (i in 1..3) {
                val payment =
                    PaymentTestFixture.createPaymentWait(
                        orderId = UUID.randomUUID(),
                        userId = testUserId,
                    )
                savePaymentPort.save(payment)
            }

            // when & then - 페이지 사이즈 2로 첫 페이지 조회
            mockMvc
                .get("/api/v1/payments") {
                    param("userId", testUserId.toString())
                    param("page", "1")
                    param("limit", "2")
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.payments") { isArray() }
                    jsonPath("$.payments.length()") { value(2) }
                    jsonPath("$.pagination.page") { value(1) }
                    jsonPath("$.pagination.limit") { value(2) }
                    jsonPath("$.pagination.total") { value(4) }
                }
        }

        @Test
        @DisplayName("200 - 데이터가 없는 경우 빈 목록 반환")
        fun shouldReturnEmptyListWhenNoPayments() {
            // given - 다른 사용자 ID로 조회
            val anotherUserId = UUID.randomUUID()

            // when & then
            mockMvc
                .get("/api/v1/payments") {
                    param("userId", anotherUserId.toString())
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.payments") { isArray() }
                    jsonPath("$.payments.length()") { value(0) }
                    jsonPath("$.pagination.total") { value(0) }
                }
        }

        @Test
        @DisplayName("200 - 두 번째 페이지 조회")
        fun shouldReturnSecondPage() {
            // given - 추가 Payment 3개 생성 (총 4개)
            for (i in 1..3) {
                val payment =
                    PaymentTestFixture.createPaymentWait(
                        orderId = UUID.randomUUID(),
                        userId = testUserId,
                    )
                savePaymentPort.save(payment)
            }

            // when & then - 두 번째 페이지 조회
            mockMvc
                .get("/api/v1/payments") {
                    param("userId", testUserId.toString())
                    param("page", "2")
                    param("limit", "2")
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.payments") { isArray() }
                    jsonPath("$.payments.length()") { value(2) }
                    jsonPath("$.pagination.page") { value(2) }
                }
        }
    }
}
