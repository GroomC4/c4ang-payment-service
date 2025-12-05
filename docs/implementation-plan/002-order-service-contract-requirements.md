# Order Service HTTP API Contract 요구사항

## 개요

이 문서는 Payment Service가 Order Service에 요청하는 HTTP API에 대한 Consumer Driven Contract 요구사항입니다.

**작성일**: 2025-12-05
**Consumer**: Payment Service
**Provider**: Order Service
**통신 방식**: HTTP REST (Spring Cloud OpenFeign)

---

## 상태

| 상태 | 설명 |
|------|------|
| ⏳ 대기 | Order Service 팀에 Contract 작성 요청 필요 |

---

## 필요 API 목록

| # | API | 용도 | 우선순위 |
|---|-----|------|----------|
| 1 | 주문 조회 | 결제 요청 시 주문 정보 조회 | 높음 |
| 2 | 결제 대기 상태 변경 | Payment 생성 시 Order와 연결 | 높음 |
| 3 | 결제 존재 여부 확인 | 중복 결제 방지 | 중간 |

> **참고**: 주문 완료/취소는 Kafka 이벤트(`payment.completed`, `payment.failed`, `payment.cancelled`)를 통해 비동기로 처리되므로 HTTP API가 필요하지 않습니다.

---

## API 상세 명세

### 1. 주문 조회

**용도**: 결제 요청 시 주문 정보 조회 (금액 검증, 사용자 확인)

**Request**:
```http
GET /internal/v1/orders/{orderId}
```

**Expected Response** (200 OK):
```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "orderNumber": "ORD-2024-001",
  "status": "ORDER_CONFIRMED",
  "totalAmount": 50000,
  "items": [
    {
      "productId": "uuid",
      "productName": "상품명",
      "quantity": 2,
      "unitPrice": 25000
    }
  ]
}
```

**Error Cases**:
| Status | 조건 |
|--------|------|
| `404 Not Found` | 주문이 존재하지 않음 |
| `401 Unauthorized` | 인증 실패 |

---

### 2. 결제 대기 상태 변경

**용도**: Payment 생성 시점에 Order와 Payment를 연결

**Request**:
```http
POST /internal/v1/orders/{orderId}/payment-pending

{
  "paymentId": "uuid"
}
```

**Expected Response** (200 OK):
```json
{
  "orderId": "uuid",
  "status": "PAYMENT_PENDING",
  "paymentId": "uuid"
}
```

**Preconditions**:
- 주문 상태가 `ORDER_CONFIRMED`여야 함

**Error Cases**:
| Status | 조건 |
|--------|------|
| `404 Not Found` | 주문이 존재하지 않음 |
| `409 Conflict` | 주문 상태가 `ORDER_CONFIRMED`가 아님 |
| `409 Conflict` | 이미 결제가 연결되어 있음 |

---

### 3. 결제 존재 여부 확인

**용도**: 주문당 결제 1개 제한 (비즈니스 규칙)

**Request**:
```http
GET /internal/v1/orders/{orderId}/has-payment
```

**Expected Response** (200 OK):
```json
{
  "hasPayment": false
}
```

**Error Cases**:
| Status | 조건 |
|--------|------|
| `404 Not Found` | 주문이 존재하지 않음 |

---

## Contract 작성 요청 사항

Order Service 팀에서 다음 Contract를 작성해주시면 Payment Service에서 구현을 진행하겠습니다.

### 요청 Contract 목록

| Contract 파일명 | 설명 |
|----------------|------|
| `shouldGetOrder.groovy` | 주문 조회 성공 |
| `shouldReturn404WhenOrderNotFound.groovy` | 주문 조회 - 주문 없음 |
| `shouldMarkPaymentPending.groovy` | 결제 대기 상태 변경 성공 |
| `shouldReturn409WhenOrderNotConfirmed.groovy` | 결제 대기 - 주문 상태 불일치 |
| `shouldReturn409WhenPaymentAlreadyExists.groovy` | 결제 대기 - 이미 결제 존재 |
| `shouldCheckHasPayment.groovy` | 결제 존재 여부 확인 |

### Spring Cloud Contract (Groovy DSL) 예시

```groovy
// shouldGetOrder.groovy
Contract.make {
    description "Order Service가 주문 정보를 반환한다"

    request {
        method GET()
        url("/internal/v1/orders/550e8400-e29b-41d4-a716-446655440000")
        headers {
            accept(applicationJson())
            header("Authorization", "Bearer test-token")
        }
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            orderId: "550e8400-e29b-41d4-a716-446655440000",
            userId: "660e8400-e29b-41d4-a716-446655440000",
            orderNumber: "ORD-2024-001",
            status: "ORDER_CONFIRMED",
            totalAmount: 50000,
            items: [
                [
                    productId: "770e8400-e29b-41d4-a716-446655440000",
                    productName: "테스트 상품",
                    quantity: 2,
                    unitPrice: 25000
                ]
            ]
        ])
    }
}
```

```groovy
// shouldMarkPaymentPending.groovy
Contract.make {
    description "주문을 결제 대기 상태로 변경한다"

    request {
        method POST()
        url("/internal/v1/orders/550e8400-e29b-41d4-a716-446655440000/payment-pending")
        headers {
            contentType(applicationJson())
            header("Authorization", "Bearer test-token")
        }
        body([
            paymentId: "880e8400-e29b-41d4-a716-446655440000"
        ])
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            orderId: "550e8400-e29b-41d4-a716-446655440000",
            status: "PAYMENT_PENDING",
            paymentId: "880e8400-e29b-41d4-a716-446655440000"
        ])
    }
}
```

```groovy
// shouldCheckHasPayment.groovy
Contract.make {
    description "주문에 결제가 연결되어 있는지 확인한다"

    request {
        method GET()
        url("/internal/v1/orders/550e8400-e29b-41d4-a716-446655440000/has-payment")
        headers {
            accept(applicationJson())
            header("Authorization", "Bearer test-token")
        }
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            hasPayment: false
        ])
    }
}
```

---

## Payment Service 구현 계획 (Contract 완성 후)

### 1. Feign Client 인터페이스

**파일**: `adapter/outbound/client/OrderFeignClient.kt`

```kotlin
@FeignClient(
    name = "order-service",
    url = "\${order-service.url}",
    configuration = [FeignClientConfig::class],
)
interface OrderFeignClient {
    @GetMapping("/internal/v1/orders/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): OrderResponse

    @PostMapping("/internal/v1/orders/{orderId}/payment-pending")
    fun markPaymentPending(
        @PathVariable orderId: UUID,
        @RequestBody request: PaymentPendingRequest,
    ): PaymentPendingResponse

    @GetMapping("/internal/v1/orders/{orderId}/has-payment")
    fun hasPayment(@PathVariable orderId: UUID): HasPaymentResponse
}
```

### 2. OrderAdapter 구현

**파일**: `adapter/outbound/client/OrderAdapter.kt`

```kotlin
@Component
class OrderAdapter(
    private val orderFeignClient: OrderFeignClient,
) : OrderPort {

    override fun findById(orderId: UUID): OrderInfo? {
        return try {
            orderFeignClient.getOrder(orderId).toOrderInfo()
        } catch (e: FeignException.NotFound) {
            null
        }
    }

    override fun markOrderPaymentPending(orderId: UUID, paymentId: UUID) {
        orderFeignClient.markPaymentPending(
            orderId = orderId,
            request = PaymentPendingRequest(paymentId = paymentId),
        )
    }

    override fun hasPayment(orderId: UUID): Boolean {
        return orderFeignClient.hasPayment(orderId).hasPayment
    }
}
```

### 3. DTO 클래스

**파일**: `adapter/outbound/client/dto/OrderDtos.kt`

```kotlin
data class OrderResponse(
    val orderId: UUID,
    val userId: UUID,
    val orderNumber: String,
    val status: String,
    val totalAmount: BigDecimal,
    val items: List<OrderItemResponse>,
) {
    fun toOrderInfo(): OrderInfo = OrderInfo(
        orderId = orderId,
        userId = userId,
        orderNumber = orderNumber,
        status = status,
        totalAmount = totalAmount,
        items = items.map { it.toOrderItemInfo() },
    )
}

data class OrderItemResponse(
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
) {
    fun toOrderItemInfo(): OrderItemInfo = OrderItemInfo(
        productId = productId,
        productName = productName,
        quantity = quantity,
        unitPrice = unitPrice,
    )
}

data class PaymentPendingRequest(
    val paymentId: UUID,
)

data class PaymentPendingResponse(
    val orderId: UUID,
    val status: String,
    val paymentId: UUID,
)

data class HasPaymentResponse(
    val hasPayment: Boolean,
)
```

### 4. Feign 설정

**파일**: `configuration/FeignClientConfig.kt`

```kotlin
@Configuration
class FeignClientConfig {

    @Bean
    fun feignErrorDecoder(): ErrorDecoder {
        return CustomErrorDecoder()
    }

    @Bean
    fun feignRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            // 인증 토큰 추가 등
        }
    }
}

class CustomErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        return when (response.status()) {
            404 -> OrderNotFoundException()
            409 -> OrderStateConflictException()
            else -> FeignException.errorStatus(methodKey, response)
        }
    }
}
```

---

## 의존성

Payment Service에 추가 필요한 의존성:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
}
```

---

## 다음 단계

1. [ ] Order Service 팀에 Contract 작성 요청
2. [ ] Contract 완성 후 Stub 다운로드
3. [ ] Feign Client 구현
4. [ ] OrderAdapter 구현
5. [ ] 통합 테스트 작성

---

**문의**: Order Service 팀
