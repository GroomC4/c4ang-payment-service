# Payment Service 도메인 연동 문서 (v2)

## 개요

이 문서는 Payment Service가 다른 도메인 서비스와 연동해야 하는 부분과 필요한 기능들을 정리합니다.

**버전**: 2.0
**작성일**: 2025-12-05
**참고 문서**: `c4ang-contract-hub/docs/interface/kafka-event-sequence.md`, `kafka-event-specifications.md`

---

## 1. Order Service 연동

### 1.1 연동 인터페이스

**파일**: `domain/port/OrderPort.kt`
**어댑터**: `adapter/outbound/client/OrderAdapter.kt`
**통신 방식**: HTTP (Spring Cloud OpenFeign)

### 1.2 필요 기능

| 메서드 | 설명 | 현재 상태 | 우선순위 |
|--------|------|-----------|----------|
| `findById(orderId)` | 주문 정보 조회 (상품 정보 포함) | ❌ TODO | 높음 |
| `markOrderPaymentPending(orderId, paymentId)` | 주문을 결제 대기 상태로 변경 | ❌ TODO | 높음 |
| `hasPayment(orderId)` | 주문에 결제 정보가 있는지 확인 | ❌ TODO | 중간 |
| `confirmStockReservation(orderId)` | 재고 예약 확정 (Redis → DB) | ❌ TODO | 높음 |

### 1.3 기능 상세

#### 1.3.1 findById
```kotlin
fun findById(orderId: UUID): OrderInfo?
```
- **용도**: 결제 요청 시 주문 정보 조회
- **반환값**: 주문 ID, 사용자 ID, 총 금액, 상품 목록 등
- **HTTP 예시**: `GET /api/v1/orders/{orderId}`

#### 1.3.2 markOrderPaymentPending
```kotlin
fun markOrderPaymentPending(orderId: UUID, paymentId: UUID)
```
- **용도**: Payment 생성 시점에 Order와 Payment를 연결
- **상태 전이**: `ORDER_CONFIRMED` → `PAYMENT_PENDING`
- **HTTP 예시**: `POST /api/v1/orders/{orderId}/payment-pending`
- **요청 본문**: `{ "paymentId": "uuid" }`

#### 1.3.3 hasPayment
```kotlin
fun hasPayment(orderId: UUID): Boolean
```
- **용도**: 주문당 결제 1개 제한 (비즈니스 규칙)
- **HTTP 예시**: `GET /api/v1/orders/{orderId}/has-payment`

#### 1.3.4 confirmStockReservation
```kotlin
fun confirmStockReservation(orderId: UUID)
```
- **용도**: 결제 완료 후 재고 예약 확정
- **처리**: Redis에 예약된 재고를 DB로 확정
- **HTTP 예시**: `POST /api/v1/orders/{orderId}/confirm-stock`

---

## 2. PG Gateway 연동

### 2.1 연동 인터페이스

**파일**: `domain/port/PaymentGatewayPort.kt`
**어댑터**: `adapter/outbound/client/PgGatewayAdapter.kt`

### 2.2 현재 상태

현재 **Stub 구현**으로 항상 성공 응답을 반환합니다.

### 2.3 필요 기능

| 메서드 | 설명 | 현재 상태 | 우선순위 |
|--------|------|-----------|----------|
| `requestPayment(paymentId, amount, orderNumber)` | PG사 결제 요청 | ✅ Stub | 높음 |
| `cancelPayment(pgTransactionId)` | PG사 결제 취소 | ❌ 미구현 | 중간 |
| `requestRefund(pgTransactionId, amount)` | PG사 환불 요청 | ❌ 미구현 | 중간 |

### 2.4 향후 연동 대상

- Toss Payments API
- Kakao Pay API
- NHN KCP API
- Naver Pay API

---

## 3. Kafka 이벤트 연동

### 3.1 구독 이벤트 (Consumer)

**파일**: `adapter/inbound/kafka/OrderEventKafkaConsumer.kt`

Payment Service가 수신하는 이벤트:

| Topic | Avro 스키마 | 처리 | 현재 상태 |
|-------|-------------|------|-----------|
| `order.confirmed` | `OrderConfirmed` | Payment 대기 생성 (PAYMENT_WAIT) | ✅ 구현됨 |
| `saga.stock-confirmation.failed` | `StockConfirmationFailed` | 결제 취소 보상 트랜잭션 | ❌ TODO |

> **참고**: `payment.completed` 이벤트는 **Product Service**가 소비합니다 (Payment Service 아님).
> Product Service가 재고 확정 후 `stock.confirmed`를 발행하면 Order Service가 소비합니다.

### 3.2 발행 이벤트 (Producer)

**파일**: `adapter/outbound/kafka/PaymentEventKafkaProducer.kt`
**Port**: `domain/port/PaymentEventPublishPort.kt`

Payment Service가 발행하는 이벤트:

| Topic | Avro 스키마 | 발행 시점 | 현재 상태 |
|-------|-------------|-----------|-----------|
| `payment.completed` | `PaymentCompleted` | PG 결제 승인 완료 | ✅ 구현됨 |
| `payment.failed` | `PaymentFailed` | 결제 실패 | ✅ 구현됨 |
| `payment.cancelled` | `PaymentCancelled` | 사용자/시스템 결제 취소 | ✅ 구현됨 |
| `saga.payment-initialization.failed` | `PaymentInitializationFailed` | 결제 대기 생성 실패 | ❌ TODO |
| `saga.payment-completion.compensate` | `PaymentCompletionCompensate` | 결제 완료 보상 (취소) | ❌ TODO |

### 3.3 SAGA 플로우

#### 3.3.1 Order Creation SAGA (정상 흐름)

```
[Order Service]                    [Product Service]                  [Payment Service]
      │                                   │                                  │
      │── order.created ─────────────────>│                                  │
      │                            (재고 예약)                                 │
      │<─────────── stock.reserved ───────│                                  │
      │                                   │                                  │
      │── order.confirmed ───────────────────────────────────────────────────>│
      │                                   │                           (Payment 대기 생성)
      │                                   │                                  │
```

#### 3.3.2 Payment SAGA (정상 흐름)

```
[Payment Service]                  [Product Service]                  [Order Service]
      │                                   │                                  │
      │<─── (사용자 결제 요청) ────────     │                                  │
      │                                   │                                  │
      │── (PG 결제 완료) ─>                │                                  │
      │                                   │                                  │
      │── payment.completed ─────────────>│                                  │
      │                            (재고 확정: Redis → DB)                     │
      │                                   │                                  │
      │                                   │── stock.confirmed ──────────────>│
      │                                   │                           (주문 완료)
      │                                   │                                  │
```

> **중요**: `payment.completed`는 **Product Service**가 소비합니다.
> Product Service가 재고 확정 후 `stock.confirmed`를 발행하고, Order Service가 이를 소비하여 주문을 완료합니다.

#### 3.3.3 Payment SAGA (재고 확정 실패 - 보상 트랜잭션)

```
[Payment Service]                  [Product Service]                  [Order Service]
      │                                   │                                  │
      │── payment.completed ─────────────>│                                  │
      │                            (재고 확정 시도 → 실패)                      │
      │                                   │                                  │
      │<── saga.stock-confirmation.failed─│                                  │
      │                                   │                                  │
      │── (PG 결제 취소) ─>                │                                  │
      │                                   │                                  │
      │── payment.cancelled ─────────────────────────────────────────────────>│
      │                                   │                           (주문 취소)
      │                                   │                                  │
```

> **중요**: `saga.stock-confirmation.failed`는 **Product Service**가 발행합니다.
> Payment Service는 이를 수신하여 PG 결제를 취소하고 `payment.cancelled`를 발행합니다.
> Order Service가 `payment.cancelled`를 수신하여 주문을 취소합니다.

#### 3.3.4 Payment SAGA (결제 실패 - 보상 트랜잭션)

```
[Payment Service]                  [Order Service]                    [Product Service]
      │                                   │                                  │
      │<─── (PG 결제 실패) ────────────    │                                  │
      │                                   │                                  │
      │── payment.failed ────────────────>│                                  │
      │                            (주문 취소)                                 │
      │                                   │                                  │
      │                                   │── saga.order-confirmation.compensate ─>│
      │                                   │                           (재고 복원)
```

---

## 4. 도메인 이벤트 핸들러 연동

### 4.1 내부 도메인 이벤트 → 외부 서비스 호출

트랜잭션 커밋 후 외부 서비스와 연동이 필요한 이벤트 핸들러:

| 이벤트 핸들러 | 도메인 이벤트 | 필요 동작 | 현재 상태 |
|---------------|---------------|-----------|-----------|
| `PaymentCompletedEventHandler` | `PaymentCompletedEvent` | Kafka `payment.completed` 발행 | ❌ TODO |
| `PaymentFailedEventHandler` | `PaymentFailedEvent` | Kafka `payment.failed` 발행 | ❌ TODO |
| `PaymentCancelledEventHandler` | `PaymentCancelledEvent` | Kafka `payment.cancelled` 발행 + PG 취소 | ❌ TODO |
| `PaymentRefundRequestedEventHandler` | `PaymentRefundRequestedEvent` | PG 환불 요청 | ❌ TODO |
| `PaymentRefundCompletedEventHandler` | `PaymentRefundCompletedEvent` | Kafka `payment.refunded` 발행 | ❌ TODO |

### 4.2 이벤트 핸들러 트랜잭션 전략

```kotlin
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun handle(event: PaymentCompletedEvent) {
    // Kafka 이벤트 발행 (독립 트랜잭션)
    paymentEventKafkaProducer.publishPaymentCompleted(...)
}
```

- **AFTER_COMMIT**: Payment 트랜잭션 커밋 후 실행
- **REQUIRES_NEW**: Kafka 발행은 독립 트랜잭션으로 분리

### 4.3 불필요한 핸들러 (제거 대상)

| 핸들러 | 이유 |
|--------|------|
| `OrderStockReservedEventHandler` | `stock.reserved` 이벤트는 Order Service가 수신. Payment Service는 `order.confirmed` 이벤트에서 처리 |

---

## 5. 구현 우선순위

### Phase 1: 핵심 결제 플로우 (높음)

1. ~~**OrderEventKafkaConsumer.handleOrderConfirmed** - Payment 대기 생성~~ ✅ 완료
2. **PaymentCompletedEventHandler** - Kafka `payment.completed` 이벤트 발행
3. **PaymentFailedEventHandler** - Kafka `payment.failed` 이벤트 발행

### Phase 2: SAGA 보상 트랜잭션 (중간)

1. **saga.stock-confirmation.failed Consumer** - 결제 취소 보상 처리
2. **PaymentCancelledEventHandler** - Kafka `payment.cancelled` 발행 + PG 취소
3. **saga.payment-completion.compensate Producer** - 보상 이벤트 발행
4. **saga.payment-initialization.failed Producer** - 결제 대기 생성 실패 시 발행

### Phase 3: Order Service HTTP 연동 (중간)

1. **OrderPort.findById** - 주문 정보 조회
2. **OrderPort.markOrderPaymentPending** - 결제 대기 연결
3. **OrderPort.hasPayment** - 중복 결제 방지
4. **OrderPort.confirmStockReservation** - 재고 확정

### Phase 4: 환불 처리 (낮음)

1. **PaymentRefundRequestedEventHandler** - PG 환불 요청
2. **PaymentRefundCompletedEventHandler** - 환불 완료 처리
3. **PgGatewayPort.requestRefund** 구현

### Phase 5: PG 실제 연동

1. **Toss Payments API** 연동
2. **기타 PG사 API** 연동

---

## 6. Order Service 요구 API 명세

Payment Service가 호출해야 하는 Order Service API:

### 6.1 주문 조회

```http
GET /api/v1/orders/{orderId}
```

**Response**:
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

### 6.2 결제 대기 상태 변경

```http
POST /api/v1/orders/{orderId}/payment-pending

{
  "paymentId": "uuid"
}
```

### 6.3 결제 존재 여부 확인

```http
GET /api/v1/orders/{orderId}/has-payment
```

**Response**:
```json
{
  "hasPayment": false
}
```

### 6.4 재고 예약 확정

```http
POST /api/v1/orders/{orderId}/confirm-stock
```

### 6.5 주문 상태 변경 (결제 완료)

```http
POST /api/v1/orders/{orderId}/payment-completed

{
  "paymentId": "uuid",
  "pgApprovalNumber": "PG-APPROVAL-001"
}
```

### 6.6 주문 취소

```http
POST /api/v1/orders/{orderId}/cancel

{
  "reason": "PAYMENT_FAILED",
  "paymentId": "uuid"
}
```

---

## 7. 기술 스택

### 연동 방식

| 연동 대상 | 방식 | 라이브러리 |
|-----------|------|------------|
| Order Service | HTTP REST | Spring Cloud OpenFeign |
| PG Gateway | HTTP REST | WebClient / RestTemplate |
| Kafka | 이벤트 스트리밍 | Spring Kafka + Avro |
| Redis | 캐시/락/멱등성 | Redisson |

### Feign Client 설정 예시

```kotlin
@FeignClient(
    name = "order-service",
    url = "\${order-service.url}"
)
interface OrderFeignClient {
    @GetMapping("/api/v1/orders/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): OrderResponse

    @PostMapping("/api/v1/orders/{orderId}/payment-pending")
    fun markPaymentPending(
        @PathVariable orderId: UUID,
        @RequestBody request: PaymentPendingRequest
    )
}
```

---

## 8. TODO 요약

### 어댑터 구현

- [ ] `OrderAdapter.findById` 구현
- [ ] `OrderAdapter.markOrderPaymentPending` 구현
- [ ] `OrderAdapter.hasPayment` 구현
- [ ] `OrderAdapter.confirmStockReservation` 구현
- [ ] `PgGatewayAdapter.cancelPayment` 구현
- [ ] `PgGatewayAdapter.requestRefund` 구현
- [ ] `PgGatewayAdapter` 실제 PG 연동

### Kafka 이벤트 핸들러 구현

- [x] `OrderEventKafkaConsumer.handleOrderConfirmed` 결제 대기 생성 로직
- [ ] `saga.stock-confirmation.failed` Consumer 구현 (결제 취소 보상) - Product Service가 발행
- [ ] `saga.payment-initialization.failed` Producer 구현

### 도메인 이벤트 핸들러 구현

- [ ] `PaymentCompletedEventHandler` → Kafka `payment.completed` 발행
- [ ] `PaymentFailedEventHandler` → Kafka `payment.failed` 발행
- [ ] `PaymentCancelledEventHandler` → Kafka `payment.cancelled` 발행 + PG 취소
- [ ] `PaymentRefundRequestedEventHandler` → PG 환불 요청
- [ ] `PaymentRefundCompletedEventHandler` → Kafka `payment.refunded` 발행
- [x] ~~`OrderStockReservedEventHandler`~~ (제거 대상)

### 기타

- [ ] 멱등성 체크 로직 (이벤트 ID 기반 중복 처리 방지)
- [ ] Feign Client 설정 및 에러 핸들링
- [ ] Circuit Breaker 패턴 적용 (Resilience4j)
- [ ] Saga Tracker 기록 로직 추가

---

## 9. v1 → v2 변경 사항

| 항목 | v1 | v2 | 변경 사유 |
|------|----|----|----------|
| `payment.completed` Consumer | Order Service | **Product Service** | Contract Hub 문서와 일치 |
| `stock.confirmed` Producer | Order Service | **Product Service** | Contract Hub 문서와 일치 |
| SAGA 보상 이벤트 | 없음 | `saga.*` 토픽 추가 | SAGA 패턴 명시적 구현 |
| `OrderStockReservedEventHandler` | 필요 | 제거 대상 | `stock.reserved`는 Order Service가 수신 |
| 도메인 이벤트 핸들러 역할 | Order Service 호출 | Kafka 이벤트 발행 | 비동기 이벤트 기반 아키텍처 |

### 9.1 핵심 플로우 변경

**v1 (잘못된 이해)**:
```
Payment → payment.completed → Order (재고 확정) → order.stock.confirmed → Payment
```

**v2 (올바른 플로우)**:
```
Payment → payment.completed → Product (재고 확정) → stock.confirmed → Order (주문 완료)
```

> **참고**: 상세 플로우는 `docs/contract-hub-index.md` 문서를 참조하세요.

---

**참고 문서**:
- `c4ang-contract-hub/docs/interface/kafka-event-sequence.md`
- `c4ang-contract-hub/docs/interface/kafka-event-specifications.md`
