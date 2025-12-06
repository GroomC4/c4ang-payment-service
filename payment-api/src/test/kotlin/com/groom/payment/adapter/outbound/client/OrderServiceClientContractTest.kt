package com.groom.payment.adapter.outbound.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.groom.payment.adapter.outbound.client.dto.OrderServiceMarkPaymentPendingRequest
import feign.Feign
import feign.FeignException
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.contract.stubrunner.StubFinder
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties
import org.springframework.cloud.openfeign.support.SpringMvcContract
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.math.BigDecimal
import java.util.UUID

/**
 * Order Service Client Consumer Contract Test
 *
 * Producer(Order Service)가 배포한 Stub을 사용하여
 * Consumer(Payment Service)의 OrderServiceClient가 Contract를 준수하는지 검증합니다.
 *
 * ## Contract 파일 위치 (order-service)
 * ```
 * order-api/src/test/resources/contracts/internal/payment-service/
 * ├── shouldGetOrder.yml                           - 주문 조회 성공
 * ├── shouldReturn404WhenOrderNotFound.yml         - 주문 조회 실패 (404)
 * ├── shouldCheckHasPayment.yml                    - 결제 존재 여부 확인
 * ├── shouldMarkPaymentPending.yml                 - 결제 대기 상태 변경 성공
 * ├── shouldReturn409WhenOrderNotConfirmed.yml     - 주문 상태 불일치 (409)
 * └── shouldReturn409WhenPaymentAlreadyExists.yml  - 이미 결제 존재 (409)
 * ```
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*OrderServiceClientContractTest"
 * ```
 *
 * ## 사전 조건
 * - Order Service가 Stub JAR를 GitHub Packages에 배포해야 함
 * - GITHUB_ACTOR, GITHUB_TOKEN 환경변수 설정 필요
 *
 * @see OrderServiceClient
 */
@Tag("contract-test")
@SpringJUnitConfig
@ActiveProfiles("test")
@AutoConfigureStubRunner(
    ids = ["com.groom:order-service-contract-stubs:+:stubs"],
    stubsMode = StubRunnerProperties.StubsMode.REMOTE,
    repositoryRoot = "https://maven.pkg.github.com/GroomC4/c4ang-packages-hub",
)
@DisplayName("Order Service Client Consumer Contract Test")
class OrderServiceClientContractTest {
    @Autowired
    private lateinit var stubFinder: StubFinder

    private lateinit var orderServiceClient: OrderServiceClient

    companion object {
        // Contract에 정의된 테스트 데이터
        // order-api/src/test/resources/contracts/internal/payment-service/ 참조
        private val EXISTING_ORDER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        private val NON_EXISTING_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private val ORDER_NOT_CONFIRMED_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001")
        private val ORDER_WITH_PAYMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002")
        private val ORDER_FOR_PAYMENT_PENDING_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003")
        private val PAYMENT_ID = UUID.fromString("880e8400-e29b-41d4-a716-446655440000")
    }

    @BeforeEach
    fun setup() {
        val objectMapper = ObjectMapper().registerKotlinModule()

        // StubFinder를 통해 동적으로 할당된 포트 조회
        val stubUrl = stubFinder.findStubUrl("order-service-contract-stubs")

        // Feign Client를 Stub Runner가 실행한 WireMock 서버에 연결
        orderServiceClient =
            Feign
                .builder()
                .contract(SpringMvcContract())
                .encoder(JacksonEncoder(objectMapper))
                .decoder(JacksonDecoder(objectMapper))
                .requestInterceptor { template ->
                    template.header("Content-Type", "application/json")
                    template.header("Accept", "application/json")
                }.target(OrderServiceClient::class.java, stubUrl.toString())
    }

    /**
     * Contract: shouldGetOrder.yml
     * Description: Order Service가 주문 정보를 반환한다
     */
    @Nested
    @DisplayName("주문 조회 API")
    inner class GetOrder {
        @Test
        @DisplayName("주문이 존재하면 주문 정보를 반환한다 - Contract: shouldGetOrder")
        fun shouldGetOrder() {
            // given - Contract에 정의된 orderId 사용

            // when
            val response = orderServiceClient.getOrder(EXISTING_ORDER_ID)

            // then - Contract에 정의된 응답값 검증
            response.shouldNotBeNull()
            response.orderId shouldBe EXISTING_ORDER_ID
            response.userId shouldBe UUID.fromString("660e8400-e29b-41d4-a716-446655440000")
            response.orderNumber shouldBe "ORD-2024-001"
            response.status shouldBe "ORDER_CONFIRMED"
            response.totalAmount shouldBe BigDecimal(50000)
            response.items.size shouldBe 1
            response.items[0].productId shouldBe UUID.fromString("770e8400-e29b-41d4-a716-446655440000")
            response.items[0].productName shouldBe "테스트 상품"
            response.items[0].quantity shouldBe 2
            response.items[0].unitPrice shouldBe BigDecimal(25000)
        }

        /**
         * Contract: shouldReturn404WhenOrderNotFound.yml
         * Description: 존재하지 않는 주문 조회 시 404를 반환한다
         */
        @Test
        @DisplayName("주문이 존재하지 않으면 404를 반환한다 - Contract: shouldReturn404WhenOrderNotFound")
        fun shouldReturn404WhenOrderNotFound() {
            // given - Contract에 정의된 존재하지 않는 orderId 사용

            // when & then - 404 응답 검증
            shouldThrow<FeignException.NotFound> {
                orderServiceClient.getOrder(NON_EXISTING_ORDER_ID)
            }
        }
    }

    /**
     * Contract: shouldCheckHasPayment.yml
     * Description: 주문에 결제가 연결되어 있는지 확인한다
     */
    @Nested
    @DisplayName("결제 존재 여부 확인 API")
    inner class CheckHasPayment {
        @Test
        @DisplayName("결제가 연결되어 있지 않으면 false를 반환한다 - Contract: shouldCheckHasPayment")
        fun shouldCheckHasPayment() {
            // given - Contract에 정의된 orderId 사용

            // when
            val response = orderServiceClient.hasPayment(EXISTING_ORDER_ID)

            // then - Contract에 정의된 응답값 검증
            response.shouldNotBeNull()
            response.hasPayment shouldBe false
        }
    }

    /**
     * Contract: shouldMarkPaymentPending.yml, shouldReturn409WhenOrderNotConfirmed.yml, shouldReturn409WhenPaymentAlreadyExists.yml
     */
    @Nested
    @DisplayName("결제 대기 상태 변경 API")
    inner class MarkPaymentPending {
        /**
         * Contract: shouldMarkPaymentPending.yml
         * Description: 주문을 결제 대기 상태로 변경한다
         */
        @Test
        @DisplayName("주문 상태가 ORDER_CONFIRMED이면 PAYMENT_PENDING으로 변경한다 - Contract: shouldMarkPaymentPending")
        fun shouldMarkPaymentPending() {
            // given - Contract에 정의된 값 사용
            val request = OrderServiceMarkPaymentPendingRequest(paymentId = PAYMENT_ID)

            // when
            val response = orderServiceClient.markPaymentPending(ORDER_FOR_PAYMENT_PENDING_ID, request)

            // then - Contract에 정의된 응답값 검증
            response.shouldNotBeNull()
            response.orderId shouldBe ORDER_FOR_PAYMENT_PENDING_ID
            response.status shouldBe "PAYMENT_PENDING"
            response.paymentId shouldBe PAYMENT_ID
        }

        /**
         * Contract: shouldReturn409WhenOrderNotConfirmed.yml
         * Description: 주문 상태가 ORDER_CONFIRMED가 아닌 경우 409를 반환한다
         */
        @Test
        @DisplayName("주문 상태가 ORDER_CONFIRMED가 아니면 409를 반환한다 - Contract: shouldReturn409WhenOrderNotConfirmed")
        fun shouldReturn409WhenOrderNotConfirmed() {
            // given - Contract에 정의된 값 사용
            val request = OrderServiceMarkPaymentPendingRequest(paymentId = PAYMENT_ID)

            // when & then - 409 응답 검증
            shouldThrow<FeignException.Conflict> {
                orderServiceClient.markPaymentPending(ORDER_NOT_CONFIRMED_ID, request)
            }
        }

        /**
         * Contract: shouldReturn409WhenPaymentAlreadyExists.yml
         * Description: 이미 결제가 연결된 주문에 결제 대기 요청 시 409를 반환한다
         */
        @Test
        @DisplayName("이미 결제가 연결되어 있으면 409를 반환한다 - Contract: shouldReturn409WhenPaymentAlreadyExists")
        fun shouldReturn409WhenPaymentAlreadyExists() {
            // given - Contract에 정의된 값 사용
            val request = OrderServiceMarkPaymentPendingRequest(paymentId = PAYMENT_ID)

            // when & then - 409 응답 검증
            shouldThrow<FeignException.Conflict> {
                orderServiceClient.markPaymentPending(ORDER_WITH_PAYMENT_ID, request)
            }
        }
    }
}
