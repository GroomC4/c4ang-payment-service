# Payment Service 연동 기능 구현 계획서

## 개요

이 문서는 `INTEGRATION-v2.md`에 정의된 미구현 항목들을 단계별로 구현하기 위한 계획서입니다.

**작성일**: 2025-12-05
**참고 문서**:
- `docs/INTEGRATION-v2.md`
- `docs/contract-hub-index.md` (Contract Hub 문서 인덱스)
- `c4ang-contract-hub/docs/interface/kafka-event-specifications.md` (권위 있는 명세서)
- `c4ang-contract-hub/event-flows/payment-processing/` (Payment 플로우 상세)

---

## 이벤트 흐름 핵심 포인트

> **중요**: `payment.completed` 이벤트의 Consumer는 **Product Service**입니다 (Order Service 아님).

**올바른 플로우**:
```
Payment Service → payment.completed → Product Service (재고 확정)
Product Service → stock.confirmed → Order Service (주문 완료)
```

**재고 확정 실패 시**:
```
Product Service → saga.stock-confirmation.failed → Payment Service
Payment Service → payment.cancelled → Order Service (주문 취소)
```

---

## 작업 목록

### Phase 0: 기반 작업 (선행 필수)

| # | 작업명 | 우선순위 | 예상 복잡도 |
|---|--------|----------|-------------|
| 0.1 | SAGA 이벤트 Topic 상수 추가 | 높음 | 낮음 |
| 0.2 | PaymentCompletedEvent 필드 추가 (userId, totalAmount, paymentMethod) | 높음 | 중간 |

### Phase 1: 핵심 결제 플로우

| # | 작업명 | 우선순위 | 예상 복잡도 |
|---|--------|----------|-------------|
| 1.1 | PaymentCompletedEventHandler - Kafka 이벤트 발행 | 높음 | 낮음 |
| 1.2 | PaymentFailedEventHandler - Kafka 이벤트 발행 | 높음 | 낮음 |

### Phase 2: SAGA 보상 트랜잭션

| # | 작업명 | 우선순위 | 예상 복잡도 |
|---|--------|----------|-------------|
| 2.1 | saga.stock-confirmation.failed Consumer 구현 | 중간 | 높음 |
| 2.2 | PaymentCancelledEventHandler - Kafka `payment.cancelled` 발행 + PG 취소 | 중간 | 중간 |
| 2.3 | saga.payment-initialization.failed Producer 구현 | 중간 | 낮음 |

### Phase 3: 환불 처리

| # | 작업명 | 우선순위 | 예상 복잡도 |
|---|--------|----------|-------------|
| 3.1 | PgGatewayPort.cancelPayment 인터페이스 추가 | 낮음 | 낮음 |
| 3.2 | PgGatewayPort.requestRefund 인터페이스 추가 | 낮음 | 낮음 |
| 3.3 | PaymentRefundRequestedEventHandler 구현 | 낮음 | 중간 |
| 3.4 | PaymentRefundCompletedEventHandler 구현 | 낮음 | 중간 |

### Phase 4: 기타

| # | 작업명 | 우선순위 | 예상 복잡도 |
|---|--------|----------|-------------|
| 4.1 | 멱등성 체크 로직 (eventId 기반) | 중간 | 중간 |
| 4.2 | OrderStockReservedEventHandler 제거 | 낮음 | 낮음 |

---

## 별도 문서로 분리된 항목

### Order Service HTTP 연동 (Consumer Driven Contract)

Order Service와의 동기 HTTP 통신은 Consumer Driven Contract 방식으로 진행합니다.

**상세 문서**: `docs/implementation-plan/002-order-service-contract-requirements.md`

Order Service 팀에서 Contract 작성이 완료되면 구현을 진행합니다.

---

## 상세 구현 계획

### 0.1 SAGA 이벤트 Topic 상수 추가

**파일**: `configuration/kafka/KafkaTopics.kt`

**변경 내용**:
```kotlin
object KafkaTopics {
    // Payment Service가 발행하는 이벤트
    const val PAYMENT_COMPLETED = "payment.completed"
    const val PAYMENT_FAILED = "payment.failed"
    const val PAYMENT_CANCELLED = "payment.cancelled"

    // Payment Service가 구독하는 이벤트
    const val ORDER_CONFIRMED = "order.confirmed"

    // SAGA 이벤트
    const val SAGA_STOCK_CONFIRMATION_FAILED = "saga.stock-confirmation.failed"  // 소비
    const val SAGA_PAYMENT_INITIALIZATION_FAILED = "saga.payment-initialization.failed"  // 발행
}
```

---

### 0.2 PaymentCompletedEvent 필드 추가

**파일**: `domain/event/PaymentCompletedEvent.kt`

Kafka Avro 스키마와 일치시키기 위해 필드를 추가해야 합니다.

**변경 후**:
```kotlin
data class PaymentCompletedEvent(
    val paymentId: UUID,
    val orderId: UUID,
    val userId: UUID,                    // 추가
    val totalAmount: BigDecimal,         // 추가
    val paymentMethod: String,           // 추가 (예: "CARD", "KAKAO_PAY")
    val pgApprovalNumber: String,
    val completedAt: LocalDateTime,
    val occurredAt: LocalDateTime,
)
```

**영향 범위**:
- Payment 도메인 엔티티에서 이벤트 발행 시 새 필드 전달
- `PaymentCompletedEventHandler` 수정

---

### 1.1 PaymentCompletedEventHandler - Kafka 이벤트 발행

**파일**: `application/event/PaymentCompletedEventHandler.kt`

**구현 내용**:
```kotlin
@Component("paymentDomainPaymentCompletedEventHandler")
class PaymentCompletedEventHandler(
    private val paymentEventKafkaProducer: PaymentEventKafkaProducer,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentCompletedEvent) {
        logger.info { "PaymentCompletedEvent received: orderId=${event.orderId}" }

        paymentEventKafkaProducer.publishPaymentCompleted(
            paymentId = event.paymentId.toString(),
            orderId = event.orderId.toString(),
            userId = event.userId.toString(),
            totalAmount = event.totalAmount,
            paymentMethod = event.paymentMethod,
            pgApprovalNumber = event.pgApprovalNumber,
        )
    }
}
```

**의존성**: `PaymentEventKafkaProducer` (이미 구현됨)

---

### 1.2 PaymentFailedEventHandler - Kafka 이벤트 발행

**파일**: `application/event/PaymentFailedEventHandler.kt`

**구현 내용**:
```kotlin
@Component
class PaymentFailedEventHandler(
    private val paymentEventKafkaProducer: PaymentEventKafkaProducer,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentFailedEvent) {
        logger.info { "PaymentFailedEvent received: orderId=${event.orderId}" }

        paymentEventKafkaProducer.publishPaymentFailed(
            paymentId = event.paymentId.toString(),
            orderId = event.orderId.toString(),
            userId = event.userId.toString(),
            failureReason = event.reason,
        )
    }
}
```

---

### 2.1 saga.stock-confirmation.failed Consumer 구현

**신규 파일**: `adapter/inbound/kafka/SagaCompensationKafkaConsumer.kt`

**구현 내용**:
1. `saga.stock-confirmation.failed` 토픽 구독 (Product Service가 발행)
2. 결제 취소 처리 (Payment 상태 변경)
3. PG 취소 API 호출
4. `payment.cancelled` 이벤트 발행 (Order Service가 주문 취소)

**예상 코드**:
```kotlin
@Component
class SagaCompensationKafkaConsumer(
    private val cancelPaymentService: CancelPaymentService,
    private val pgGatewayAdapter: PgGatewayAdapter,
    private val paymentEventKafkaProducer: PaymentEventKafkaProducer,
) {
    @KafkaListener(
        topics = [KafkaTopics.SAGA_STOCK_CONFIRMATION_FAILED],
        groupId = "payment-service-saga-compensation",
    )
    fun handleStockConfirmationFailed(
        event: StockConfirmationFailed,
        acknowledgment: Acknowledgment,
    ) {
        logger.info { "StockConfirmationFailed 수신: orderId=${event.orderId}, paymentId=${event.paymentId}" }

        // 1. Payment 조회 및 취소
        // 2. PG 취소 API 호출
        // 3. payment.cancelled 발행 (Order Service가 주문 취소)

        acknowledgment.acknowledge()
    }
}
```

**의존성**:
- `StockConfirmationFailed` Avro 스키마 필요 (contract-hub 확인)
- `PgGatewayAdapter.cancelPayment()` 구현 필요

---

### 2.2 PaymentCancelledEventHandler - Kafka 발행 + PG 취소

**파일**: `application/event/PaymentCancelledEventHandler.kt`

**구현 내용**:
1. Kafka `payment.cancelled` 이벤트 발행
2. PG 취소 API 호출 (결제 완료 상태에서 취소하는 경우)

**주의사항**:
- 결제 완료 후 취소하는 경우: PG 취소 필요
- 결제 대기 상태에서 취소하는 경우: PG 호출 불필요

---

### 2.3 saga.payment-initialization.failed Producer 구현

**파일**: `adapter/outbound/kafka/PaymentEventKafkaProducer.kt`

**구현 내용**:
- `order.confirmed` 이벤트 처리 중 결제 대기 생성에 실패하면 발행
- Order Service가 이를 수신하여 주문 취소 처리

```kotlin
fun publishPaymentInitializationFailed(
    orderId: String,
    failureReason: String,
) {
    // saga.payment-initialization.failed 토픽에 발행
}
```

---

### 3.1 PgGatewayPort.cancelPayment 인터페이스 추가

**파일**: `domain/port/PaymentGatewayPort.kt`

**구현 내용**:
- 결제 취소 API 인터페이스 정의
- 2.1 (saga.stock-confirmation.failed Consumer)에서 사용

```kotlin
interface PaymentGatewayPort {
    // 기존 메서드
    fun requestPayment(request: PaymentRequest): PaymentResponse

    // 추가
    fun cancelPayment(pgTransactionId: String): CancelPaymentResponse
}
```

**Stub 구현** (`adapter/outbound/client/PgGatewayAdapter.kt`):
```kotlin
override fun cancelPayment(pgTransactionId: String): CancelPaymentResponse {
    logger.info { "PG 결제 취소 요청: pgTransactionId=$pgTransactionId" }
    // Stub: 항상 성공 반환
    return CancelPaymentResponse(
        success = true,
        cancelledAt = LocalDateTime.now(),
    )
}
```

---

### 3.2 PgGatewayPort.requestRefund 인터페이스 추가

**파일**: `domain/port/PaymentGatewayPort.kt`

**구현 내용**:
- 환불 요청 API 인터페이스 정의
- 부분 환불 지원

```kotlin
interface PaymentGatewayPort {
    // 기존 메서드
    fun requestPayment(request: PaymentRequest): PaymentResponse
    fun cancelPayment(pgTransactionId: String): CancelPaymentResponse

    // 추가
    fun requestRefund(pgTransactionId: String, amount: BigDecimal): RefundResponse
}
```

**Stub 구현**:
```kotlin
override fun requestRefund(pgTransactionId: String, amount: BigDecimal): RefundResponse {
    logger.info { "PG 환불 요청: pgTransactionId=$pgTransactionId, amount=$amount" }
    // Stub: 항상 성공 반환
    return RefundResponse(
        success = true,
        refundId = "REFUND-${UUID.randomUUID()}",
        refundedAmount = amount,
        refundedAt = LocalDateTime.now(),
    )
}
```

---

### 3.3 PaymentRefundRequestedEventHandler 구현

**파일**: `application/event/PaymentRefundRequestedEventHandler.kt`

**트리거**: 도메인에서 `PaymentRefundRequestedEvent` 발행 시

**구현 내용**:
1. PG 환불 API 호출 (`PgGatewayPort.requestRefund`)
2. 환불 결과에 따라 Payment 상태 업데이트
3. 환불 완료 시 `PaymentRefundCompletedEvent` 발행

```kotlin
@Component
class PaymentRefundRequestedEventHandler(
    private val pgGatewayPort: PaymentGatewayPort,
    private val paymentRepository: PaymentRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentRefundRequestedEvent) {
        logger.info { "환불 요청 처리: paymentId=${event.paymentId}, amount=${event.amount}" }

        val payment = paymentRepository.findById(event.paymentId)
            ?: throw PaymentNotFoundException(event.paymentId)

        // PG 환불 요청
        val refundResponse = pgGatewayPort.requestRefund(
            pgTransactionId = payment.pgTransactionId,
            amount = event.amount,
        )

        if (refundResponse.success) {
            // 환불 완료 이벤트 발행
            applicationEventPublisher.publishEvent(
                PaymentRefundCompletedEvent(
                    paymentId = event.paymentId,
                    refundId = refundResponse.refundId,
                    refundedAmount = refundResponse.refundedAmount,
                    refundedAt = refundResponse.refundedAt,
                )
            )
        } else {
            logger.error { "환불 실패: paymentId=${event.paymentId}" }
            // 환불 실패 처리 로직
        }
    }
}
```

**의존성**:
- `PaymentRefundRequestedEvent` 도메인 이벤트 정의 필요
- `PaymentRefundCompletedEvent` 도메인 이벤트 정의 필요

---

### 3.4 PaymentRefundCompletedEventHandler 구현

**파일**: `application/event/PaymentRefundCompletedEventHandler.kt`

**트리거**: `PaymentRefundRequestedEventHandler`에서 환불 완료 시 발행

**구현 내용**:
1. Payment 상태를 `REFUNDED`로 업데이트
2. Kafka `payment.refunded` 이벤트 발행 (필요 시)

```kotlin
@Component
class PaymentRefundCompletedEventHandler(
    private val paymentRepository: PaymentRepository,
    private val paymentEventKafkaProducer: PaymentEventKafkaProducer,
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: PaymentRefundCompletedEvent) {
        logger.info { "환불 완료 처리: paymentId=${event.paymentId}, refundId=${event.refundId}" }

        // Payment 상태 업데이트
        val payment = paymentRepository.findById(event.paymentId)
            ?: throw PaymentNotFoundException(event.paymentId)

        payment.markAsRefunded(event.refundId, event.refundedAmount)
        paymentRepository.save(payment)

        // Kafka 이벤트 발행 (필요 시)
        // paymentEventKafkaProducer.publishPaymentRefunded(...)
    }
}
```

**의존성**:
- Payment 엔티티에 `markAsRefunded()` 메서드 추가 필요
- Payment 상태에 `REFUNDED` 추가 필요

---

### 4.1 멱등성 체크 로직 (eventId 기반)

**파일**: `adapter/inbound/kafka/OrderEventKafkaConsumer.kt` 및 기타 Consumer

**구현 내용**:
```kotlin
fun handleOrderConfirmed(event: OrderConfirmed, acknowledgment: Acknowledgment) {
    // 멱등성 체크
    if (idempotencyService.isDuplicate("order-confirmed:${event.eventId}")) {
        logger.warn { "중복 이벤트 무시: eventId=${event.eventId}" }
        acknowledgment.acknowledge()
        return
    }

    // 비즈니스 로직
    // ...

    // 처리 완료 기록
    idempotencyService.markProcessed("order-confirmed:${event.eventId}")
    acknowledgment.acknowledge()
}
```

**의존성**: `IdempotencyService` (이미 구현됨)

---

### 4.2 OrderStockReservedEventHandler 제거

**파일**: `application/event/OrderStockReservedEventHandler.kt`

**작업 내용**:
1. 파일 삭제
2. 관련 테스트 파일 삭제

**이유**:
- `stock.reserved` 이벤트는 Order Service가 수신
- Payment Service는 `order.confirmed` 이벤트에서 결제 대기를 생성

---

## 의존성 확인 필요 사항

| 항목 | 확인 필요 내용 | 상태 |
|------|---------------|------|
| Avro 스키마 | `StockConfirmationFailed` 스키마 존재 여부 | contract-hub/src/main/avro/saga/ 확인 필요 |
| Consumer Group | `payment-service-saga-compensation` 그룹 ID | kafka-event-specifications.md에서 확인됨 ✅ |

---

## 확인 완료 사항

| 항목 | 결과 |
|------|------|
| `payment.completed` Consumer | **Product Service** (Order Service 아님) |
| `stock.confirmed` Producer | **Product Service** (Order Service 아님) |
| `stock.confirmed` Consumer | **Order Service** (Payment Service 아님) |
| Payment Service → `stock.confirmed` 처리 | **불필요** (제거됨) |

---

## 다음 단계

1. Phase 0 작업 착수 (0.1, 0.2)
2. Phase 1 작업 착수 (1.1, 1.2)
3. 테스트 작성 및 검증
4. Phase 2 작업 진행
5. Order Service Contract 요청 후 HTTP 연동 구현
