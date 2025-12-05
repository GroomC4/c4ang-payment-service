# Contract Hub 문서 인덱스

이 문서는 `c4ang-contract-hub` 저장소의 문서들을 인덱싱하여 Payment Service 개발 시 참고할 수 있도록 정리한 것입니다.

**작성일**: 2025-12-05
**원본 위치**: `/Users/castle/Workspace/c4ang-contract-hub/`

---

## 1. docs/ 디렉토리 구조

### 1.1 interface/ - 인터페이스 명세서

| 파일 | 역할 | 주요 내용 |
|------|------|-----------|
| `kafka-event-specifications.md` | **Kafka 토픽 및 이벤트 명세** (권위 있는 문서) | 토픽 생성 스크립트, Avro 스키마, Consumer Group, SAGA 보상 트랜잭션 |
| `kafka-event-sequence.md` | **서비스 간 통신 시퀀스** | Mermaid 다이어그램, 시나리오별 흐름, 에러 처리 |
| `internal-api-specifications.md` | **내부 HTTP API 명세** | 서비스 간 동기 HTTP 통신 |

### 1.2 generated/ - 자동 생성 문서

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `event-specifications.md` | Avro 스키마에서 자동 생성된 이벤트 문서 | ⚠️ `AvroDocGenerator.kt`가 `c4ang.` 프리픽스를 자동 추가하므로 **실제 토픽명과 다름** |

### 1.3 publishing/ - 아티팩트 배포 가이드

| 파일 | 역할 |
|------|------|
| `avro-artifact-publishing.md` | Avro 아티팩트 배포 방법 |
| `github-packages-guide.md` | GitHub Packages 사용 가이드 |

### 1.4 guide/ - 개발 가이드

| 파일 | 역할 |
|------|------|
| `contract 테스트 가이드.md` | Spring Cloud Contract 테스트 작성 가이드 |
| `contract(명세 요구사항) 생성 가이드.md` | Contract 명세 작성 가이드 |
| `microservice-refactoring-guide.toml` | MSA 리팩토링 가이드 |

### 1.5 todo/ - 의사결정 기록

| 파일 | 역할 |
|------|------|
| `ADR-001-avro-rest-api-strategy.md` | Avro vs REST API 전략 결정 |

### 1.6 기타

| 파일 | 역할 |
|------|------|
| `명세-유지-자동화-전략.md` | 명세 문서 자동화 전략 |

---

## 2. event-flows/ 디렉토리 구조

기능 단위로 이벤트 흐름을 정리한 문서들입니다.

### 2.1 order-creation/ - 주문 생성 SAGA

| 파일 | 역할 | 관련 이벤트 |
|------|------|-------------|
| `README.md` | 주문 생성 SAGA 개요 | - |
| `success.md` | **정상 플로우**: 주문 → 재고 예약 → 주문 확정 → 결제 대기 | `order.created`, `stock.reserved`, `order.confirmed` |
| `stock-reservation-failed.md` | **실패 플로우**: 재고 부족으로 주문 취소 | `saga.stock-reservation.failed` |

### 2.2 payment-processing/ - 결제 처리 SAGA ⭐ (Payment Service 핵심)

| 파일 | 역할 | 관련 이벤트 |
|------|------|-------------|
| `README.md` | 결제 처리 SAGA 개요 | - |
| `payment-success.md` | **정상 플로우**: 결제 완료 → 재고 확정 → 주문 완료 | `payment.completed`, `stock.confirmed` |
| `payment-failed.md` | **실패 플로우**: PG 결제 거부 → 주문 취소 → 재고 복원 | `payment.failed`, `order.cancelled` |
| `stock-confirmation-failed.md` | **실패 플로우**: 재고 확정 실패 → 결제 환불 → 주문 취소 | `stock.confirmation.failed`, `payment.cancelled` |

### 2.3 store-management/ - 매장 관리

| 파일 | 역할 | 관련 이벤트 |
|------|------|-------------|
| `README.md` | 매장 관리 개요 | - |
| `create-store.md` | 매장 생성 (동기 HTTP) | - |
| `delete-store.md` | 매장 삭제 (Soft Delete) | `store.deleted` |
| `update-store-info.md` | 매장 정보 수정 | - |

### 2.4 scheduled-jobs/ - 스케줄 작업

| 파일 | 역할 | 관련 이벤트 |
|------|------|-------------|
| `README.md` | 스케줄 작업 개요 | - |
| `order-expiration.md` | 주문 만료 처리 (5분 초과) | `order.cancelled`, `order.expiration.notification` |
| `stock-sync.md` | 재고 동기화 (Redis ↔ DB) | `stock.sync.alert` |
| `daily-statistics.md` | 일일 통계 집계 | `analytics.daily.statistics` |

---

## 3. Payment Service 관련 핵심 토픽 정리

### 3.1 Payment Service가 발행하는 토픽

| 토픽명 | Avro 스키마 | 발행 시점 | Consumer |
|--------|-------------|-----------|----------|
| `payment.completed` | `PaymentCompleted` | PG 결제 승인 완료 | **Product Service** |
| `payment.failed` | `PaymentFailed` | 결제 실패 | Order Service |
| `payment.cancelled` | `PaymentCancelled` | 결제 취소 (환불) | Order Service |
| `saga.payment-initialization.failed` | `PaymentInitializationFailed` | 결제 대기 생성 실패 | Order Service |
| `saga.payment-completion.compensate` | (SAGA 보상) | 결제 완료 보상 (취소) | Product Service |

### 3.2 Payment Service가 소비하는 토픽

| 토픽명 | Avro 스키마 | 발행자 | 처리 내용 |
|--------|-------------|--------|-----------|
| `order.confirmed` | `OrderConfirmed` | Order Service | Payment 대기 생성 (PAYMENT_WAIT) |
| `saga.stock-confirmation.failed` | `StockConfirmationFailed` | Product Service | 결제 취소 보상 트랜잭션 |

### 3.3 Payment Service와 무관한 토픽 (참고용)

| 토픽명 | Producer | Consumer | 비고 |
|--------|----------|----------|------|
| `stock.confirmed` | **Product Service** | Order Service | Payment → Product → Order 흐름 |
| `order.created` | Order Service | Product Service | 재고 예약 시작 |
| `stock.reserved` | Product Service | Order Service | 재고 예약 완료 |

---

## 4. Payment SAGA 플로우 (정정된 버전)

### 4.1 정상 플로우: 결제 성공

**출처**: `event-flows/payment-processing/payment-success.md`

```
[Client]                [Payment Service]           [Product Service]           [Order Service]
   │                          │                            │                          │
   │── POST /payments ───────>│                            │                          │
   │                          │── PG 결제 요청 ──>          │                          │
   │                          │<── PG 승인 완료 ───         │                          │
   │<── 200 OK ───────────────│                            │                          │
   │                          │                            │                          │
   │                          │── payment.completed ──────>│                          │
   │                          │                     (재고 확정: Redis → DB)            │
   │                          │                            │                          │
   │                          │                            │── stock.confirmed ──────>│
   │                          │                            │                   (주문 완료)
   │                          │                            │                          │
```

**핵심 포인트**:
- `payment.completed`는 **Product Service**가 소비 (Order Service 아님!)
- **Product Service**가 재고 확정 후 `stock.confirmed` 발행
- **Order Service**가 `stock.confirmed`를 소비하여 주문 완료 처리

### 4.2 실패 플로우: 재고 확정 실패

**출처**: `event-flows/payment-processing/stock-confirmation-failed.md`

```
[Payment Service]           [Product Service]           [Order Service]
      │                            │                          │
      │── payment.completed ──────>│                          │
      │                     (재고 확정 시도 → 실패)             │
      │                            │                          │
      │<── stock.confirmation.failed ─│                       │
      │                            │                          │
      │── PG 결제 취소 ──>          │                          │
      │                            │                          │
      │── payment.cancelled ──────────────────────────────────>│
      │                            │                    (주문 취소)
```

**핵심 포인트**:
- `stock.confirmation.failed`는 **Product Service**가 발행
- Payment Service는 이를 수신하여 PG 결제 취소 후 `payment.cancelled` 발행
- Order Service가 `payment.cancelled`를 수신하여 주문 취소

---

## 5. SAGA 토픽 네이밍 규칙 (v2.0)

**출처**: `docs/interface/kafka-event-specifications.md` Section 2.8

### 5.1 비즈니스 이벤트 vs SAGA 이벤트

| 구분 | 네이밍 패턴 | 예시 |
|------|-------------|------|
| **비즈니스 Forward 이벤트** | `{domain}.{action}` | `payment.completed`, `order.created` |
| **SAGA 실패 이벤트** | `saga.{transaction}.failed` | `saga.stock-confirmation.failed` |
| **SAGA 보상 이벤트** | `saga.{transaction}.compensate` | `saga.payment-completion.compensate` |
| **SAGA Tracker** | `saga.tracker` | (단일 토픽) |

### 5.2 SAGA 관련 전체 토픽 목록

**실패 이벤트**:
- `saga.stock-reservation.failed`
- `saga.payment-initialization.failed`
- `saga.stock-confirmation.failed`

**보상 이벤트**:
- `saga.order-creation.compensate`
- `saga.order-confirmation.compensate`
- `saga.payment-completion.compensate`
- `saga.stock-reservation.compensate`
- `saga.stock-confirmation.compensate`

**Tracker**:
- `saga.tracker`

---

## 6. Consumer Group 정리

### 6.1 Payment Service Consumer Groups

| Consumer Group ID | 구독 토픽 | 목적 |
|------------------|-----------|------|
| `payment-service-saga-forward` | `order.confirmed` | Order Creation Saga: 결제 대기 생성 |
| `payment-service-saga-compensation` | `saga.stock-confirmation.failed` | Payment Saga: 결제 취소 처리 |

### 6.2 관련 서비스 Consumer Groups (참고)

| Consumer Group ID | 서비스 | 구독 토픽 |
|------------------|--------|-----------|
| `product-service-saga-forward` | Product Service | `order.created` |
| `order-service-saga-payment` | Order Service | `payment.completed`, `payment.failed` |
| `product-service-saga-compensation` | Product Service | `saga.order-confirmation.compensate`, `saga.payment-completion.compensate` |

---

## 7. 주의사항

### 7.1 자동 생성 문서의 토픽명 불일치

`docs/generated/event-specifications.md`는 `AvroDocGenerator.kt`에 의해 자동 생성되며, 토픽명에 `c4ang.` 프리픽스가 자동으로 붙습니다.

**실제 토픽명은 `docs/interface/kafka-event-specifications.md`를 참조하세요.**

| 자동 생성 문서 | 실제 토픽명 |
|----------------|-------------|
| `c4ang.payment.completed` | `payment.completed` |
| `c4ang.order.confirmed` | `order.confirmed` |

### 7.2 서비스 책임 혼동 주의

`payment.completed` 이벤트의 흐름:

```
❌ 잘못된 이해:
Payment Service → payment.completed → Order Service (재고 확정)

✅ 올바른 흐름:
Payment Service → payment.completed → Product Service (재고 확정)
Product Service → stock.confirmed → Order Service (주문 완료)
```

---

## 8. 관련 Avro 스키마 위치

```
c4ang-contract-hub/src/main/avro/
├── order/
│   ├── OrderCreated.avsc
│   ├── OrderConfirmed.avsc
│   ├── OrderCancelled.avsc
│   └── StockConfirmed.avsc
├── payment/
│   ├── PaymentCompleted.avsc
│   ├── PaymentFailed.avsc
│   └── PaymentCancelled.avsc
├── product/
│   ├── StockReserved.avsc
│   └── StockReservationFailed.avsc
├── saga/
│   ├── StockConfirmationFailed.avsc
│   ├── PaymentInitializationFailed.avsc
│   └── (보상 이벤트 스키마들)
├── analytics/
│   └── DailyStatistics.avsc
└── monitoring/
    └── StockSyncAlert.avsc
```

---

## 9. 참고 문서 링크

- **Kafka 토픽 명세**: `c4ang-contract-hub/docs/interface/kafka-event-specifications.md`
- **서비스 간 시퀀스**: `c4ang-contract-hub/docs/interface/kafka-event-sequence.md`
- **결제 성공 플로우**: `c4ang-contract-hub/event-flows/payment-processing/payment-success.md`
- **재고 확정 실패**: `c4ang-contract-hub/event-flows/payment-processing/stock-confirmation-failed.md`
- **결제 실패 플로우**: `c4ang-contract-hub/event-flows/payment-processing/payment-failed.md`

---

**마지막 업데이트**: 2025-12-05
