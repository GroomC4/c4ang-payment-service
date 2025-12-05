# Payment Service 도메인 연동 문서

## 개요

이 문서는 Payment Service가 다른 도메인 서비스와 연동해야 하는 부분과 필요한 기능들을 정리합니다.

---

## 1. Order Service 연동

### 1.1 연동 인터페이스

**파일**: `domain/port/OrderPort.kt`
**어댑터**: `adapter/outbound/client/OrderAdapter.kt`

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
- **용도**: Payment 생성 시 Order와 Payment 연결
- **상태 전이**: `STOCK_RESERVED` → `PAYMENT_PENDING`
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
| `order.created` | `OrderCreated` | 현재 미사용 (향후 확장) | ⚠️ 로그만 |
| `order.confirmed` | `OrderConfirmed` | Payment 대기 생성 | ✅ 구현됨 |
| `stock.confirmed` | `StockConfirmed` | 결제 최종 완료 처리 | ⚠️ 로그만 (TODO) |

### 3.2 발행 이벤트 (Producer)

**파일**: `adapter/outbound/kafka/PaymentEventKafkaProducer.kt`
**Port**: `domain/port/PaymentEventPublishPort.kt`

Payment Service가 발행하는 이벤트:

| Topic | Avro 스키마 | 발행 시점 | 현재 상태 |
|-------|-------------|-----------|-----------|
| `payment.completed` | `PaymentCompleted` | PG 결제 승인 완료 | ✅ 구현됨 |
| `payment.failed` | `PaymentFailed` | 결제 실패 | ✅ 구현됨 |
| `payment.cancelled` | `PaymentCancelled` | 결제 취소 | ✅ 구현됨 |

### 3.3 SAGA 플로우

```
[Order Service]                    [Payment Service]                  [Product Service]
      │                                   │                                  │
      │── order.created ─────────────────>│                                  │
      │                                   │                                  │
      │── order.confirmed ───────────────>│                                  │
      │                           (Payment 대기 생성)                          │
      │                                   │                                  │
      │                                   │<─── (사용자 결제 요청) ────────      │
      │                                   │                                  │
      │                                   │── (PG 결제 완료) ─>                │
      │                                   │                                  │
      │<─────────── payment.completed ────│                                  │
      │                                   │                                  │
      │── stock.confirmed ───────────────>│                                  │
      │                           (결제 최종 완료)                               │
```

---

## 4. 도메인 이벤트 핸들러 연동

### 4.1 내부 도메인 이벤트 → Order 서비스 호출

트랜잭션 커밋 후 Order 서비스와 연동이 필요한 이벤트 핸들러:

| 이벤트 핸들러 | 도메인 이벤트 | 필요 동작 | 현재 상태 |
|---------------|---------------|-----------|-----------|
| `PaymentCompletedEventHandler` | `PaymentCompletedEvent` | Order 상태를 `PAYMENT_COMPLETED`로 변경 | ❌ TODO |
| `PaymentFailedEventHandler` | `PaymentFailedEvent` | Order 취소, 재고 복구 | ❌ TODO |
| `PaymentCancelledEventHandler` | `PaymentCancelledEvent` | Order 취소, 재고 복구 | ❌ TODO |
| `PaymentRefundRequestedEventHandler` | `PaymentRefundRequestedEvent` | Order 상태를 `REFUND_PROCESSING`으로 변경 | ❌ TODO |
| `PaymentRefundCompletedEventHandler` | `PaymentRefundCompletedEvent` | Order 상태를 `REFUND_COMPLETED`로 변경, 재고 복구 | ❌ TODO |
| `OrderStockReservedEventHandler` | `StockReservedEvent` | Payment 엔티티 생성, Order-Payment 연결 | ❌ TODO |

### 4.2 이벤트 핸들러 트랜잭션 전략

```kotlin
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun handle(event: PaymentCompletedEvent) {
    // Order 서비스 호출 (독립 트랜잭션)
}
```

- **AFTER_COMMIT**: Payment 트랜잭션 커밋 후 실행
- **REQUIRES_NEW**: Order 호출은 독립 트랜잭션으로 분리

---

## 5. 구현 우선순위

### Phase 1: 핵심 결제 플로우 (높음)

1. **OrderPort.findById** - 주문 정보 조회
2. **OrderPort.markOrderPaymentPending** - 결제 대기 연결
3. **OrderPort.confirmStockReservation** - 재고 확정
4. ~~**OrderEventKafkaConsumer.handleOrderConfirmed** - Payment 대기 생성~~ ✅ 완료
5. **PaymentCompletedEventHandler** - 결제 완료 후 Order 상태 변경

### Phase 2: 실패/취소 처리 (중간)

1. **PaymentFailedEventHandler** - 결제 실패 시 Order 취소
2. **PaymentCancelledEventHandler** - 결제 취소 시 Order 취소
3. **OrderPort.hasPayment** - 중복 결제 방지

### Phase 3: 환불 처리 (낮음)

1. **PaymentRefundRequestedEventHandler** - 환불 요청 처리
2. **PaymentRefundCompletedEventHandler** - 환불 완료 처리
3. **PG 환불 API 연동**

### Phase 4: PG 실제 연동

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
  "status": "STOCK_RESERVED",
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
- [ ] `PgGatewayAdapter` 실제 PG 연동

### 이벤트 핸들러 구현

- [x] `OrderEventKafkaConsumer.handleOrderConfirmed` 결제 대기 생성 로직
- [ ] `OrderEventKafkaConsumer.handleStockConfirmed` 결제 최종 완료 로직
- [ ] `PaymentCompletedEventHandler` Order 상태 변경 호출
- [ ] `PaymentFailedEventHandler` Order 취소 호출
- [ ] `PaymentCancelledEventHandler` Order 취소 호출
- [ ] `PaymentRefundRequestedEventHandler` Order 상태 변경 호출
- [ ] `PaymentRefundCompletedEventHandler` Order 상태 변경 호출
- [ ] `OrderStockReservedEventHandler` Payment 생성 로직

### 기타

- [ ] 멱등성 체크 로직 (이벤트 ID 기반 중복 처리 방지)
- [ ] Feign Client 설정 및 에러 핸들링
- [ ] Circuit Breaker 패턴 적용 (Resilience4j)
