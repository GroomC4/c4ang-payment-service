# Payment Service

결제 요청, 완료, 취소, 환불 등 결제 생명주기 전반을 관리하는 마이크로서비스입니다.

## 기술 스택

- **Language**: Kotlin 1.9+
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL (Master-Replica 구성)
- **Cache**: Redis (Redisson)
- **Messaging**: Apache Kafka (Avro + Schema Registry)
- **Build**: Gradle (Kotlin DSL)

## 아키텍처

헥사고날 아키텍처(Ports & Adapters)를 기반으로 설계되었습니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Adapter Layer                                   │
│  ┌─────────────────────────┐              ┌─────────────────────────────┐   │
│  │     Inbound Adapters    │              │     Outbound Adapters       │   │
│  │  - REST Controllers     │              │  - JPA Repositories         │   │
│  │  - PG Callback API      │              │  - Feign Clients            │   │
│  │                         │              │  - Kafka Publisher          │   │
│  │                         │              │  - Redis (Lock/Idempotency) │   │
│  └───────────┬─────────────┘              └─────────────┬───────────────┘   │
└──────────────┼──────────────────────────────────────────┼───────────────────┘
               │                                          │
               ▼                                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Application Layer                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Use Cases (Services)           │  Event Handlers                   │    │
│  │  - RequestPaymentService        │  - OrderCreatedEventListener      │    │
│  │  - CompletePaymentService       │  - PaymentEventPublisher          │    │
│  │  - CancelPaymentService         │                                   │    │
│  │  - RequestPaymentRefundService  │                                   │    │
│  │  - CompletePaymentRefundService │                                   │    │
│  │  - GetPaymentService            │                                   │    │
│  │  - ListPaymentsService          │                                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
               │                                          ▲
               ▼                                          │
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Domain Layer                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐  │
│  │  Aggregates     │  │  Domain Services│  │  Ports (Interfaces)         │  │
│  │  - Payment      │  │  - PaymentLock  │  │  - LoadPaymentPort          │  │
│  │  - PaymentHist  │  │    Manager      │  │  - SavePaymentPort          │  │
│  │  - PaymentGW    │  │                 │  │  - PaymentGatewayPort       │  │
│  │    Log          │  │                 │  │  - IdempotencyPort          │  │
│  │                 │  │                 │  │  - OrderPort                │  │
│  │  Domain Events  │  │                 │  │  - PaymentEventPublisher    │  │
│  │  - PaymentReq   │  │                 │  │                             │  │
│  │  - PaymentComp  │  │                 │  │                             │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 패키지 구조

```
com.groom.payment
├── adapter/                          # 외부 시스템과의 연결 어댑터
│   ├── inbound/                      # 들어오는 요청 처리
│   │   └── web/                      # REST API 컨트롤러
│   │       ├── PaymentCommandController.kt   # 결제 요청/취소/환불 API
│   │       ├── PaymentQueryController.kt     # 결제 조회 API
│   │       ├── ExternalPaymentController.kt  # PG 콜백 API
│   │       └── dto/                          # Request/Response DTO
│   │
│   └── outbound/                     # 외부 시스템 호출
│       ├── client/                   # 외부 서비스 호출
│       │   ├── OrderAdapter.kt           # Order Service 호출
│       │   ├── PgGatewayAdapter.kt       # PG사 API 호출
│       │   └── RedisIdempotencyAdapter.kt # 멱등성 검증
│       │
│       ├── persistence/              # 데이터베이스 접근
│       │   ├── PaymentJpaRepository.kt       # Payment JPA Repository
│       │   ├── PaymentPersistenceAdapter.kt  # Port 구현체
│       │   ├── PaymentHistoryJpaRepository.kt
│       │   ├── PaymentHistoryPersistenceAdapter.kt
│       │   ├── PaymentGatewayLogJpaRepository.kt
│       │   └── PaymentGatewayLogPersistenceAdapter.kt
│       │
│       └── lock/                     # 분산 락 관리
│           └── RedisPaymentLockManager.kt   # Redisson 기반 분산 락
│
├── application/                      # 유스케이스 및 비즈니스 오케스트레이션
│   ├── dto/                          # Command/Query/Result DTO
│   │   ├── RequestPaymentCommand.kt
│   │   ├── CompletePaymentCommand.kt
│   │   ├── CancelPaymentCommand.kt
│   │   └── ...
│   │
│   ├── service/                      # 유스케이스 구현
│   │   ├── RequestPaymentService.kt      # 결제 요청
│   │   ├── CompletePaymentService.kt     # 결제 완료 (PG 콜백)
│   │   ├── CancelPaymentService.kt       # 결제 취소
│   │   ├── MarkPaymentFailedService.kt   # 결제 실패 처리
│   │   ├── RequestPaymentRefundService.kt  # 환불 요청
│   │   ├── CompletePaymentRefundService.kt # 환불 완료 (PG 콜백)
│   │   ├── GetPaymentService.kt          # 결제 상세 조회
│   │   └── ListPaymentsService.kt        # 결제 목록 조회
│   │
│   └── event/                        # 이벤트 핸들러
│       ├── OrderEventListener.kt         # Order 이벤트 수신 (Kafka)
│       └── PaymentEventPublisher.kt      # Payment 이벤트 발행 (Kafka)
│
├── domain/                           # 핵심 비즈니스 로직
│   ├── model/                        # 도메인 엔티티 및 값 객체
│   │   ├── Payment.kt                    # 결제 애그리게이트 루트
│   │   ├── PaymentHistory.kt             # 결제 이력
│   │   ├── PaymentGatewayLog.kt          # PG 통신 로그
│   │   ├── PaymentStatus.kt              # 결제 상태 enum
│   │   ├── PaymentMethod.kt              # 결제 수단 enum
│   │   └── ...
│   │
│   ├── event/                        # 도메인 이벤트
│   │   ├── PaymentRequestedEvent.kt
│   │   ├── PaymentCompletedEvent.kt
│   │   ├── PaymentCancelledEvent.kt
│   │   ├── PaymentFailedEvent.kt
│   │   ├── RefundRequestedEvent.kt
│   │   └── RefundCompletedEvent.kt
│   │
│   ├── service/                      # 도메인 서비스
│   │   └── PaymentLockManager.kt         # 분산 락 인터페이스
│   │
│   └── port/                         # 포트 인터페이스 (추상화)
│       ├── LoadPaymentPort.kt            # 결제 조회
│       ├── SavePaymentPort.kt            # 결제 저장
│       ├── PaymentGatewayPort.kt         # PG사 통신
│       ├── IdempotencyPort.kt            # 멱등성 검증
│       ├── OrderPort.kt                  # Order Service 호출
│       └── PaymentEventPublisher.kt      # 외부 이벤트 발행
│
├── configuration/                    # 설정 클래스
│   ├── jpa/                          # JPA/DataSource 설정
│   ├── kafka/                        # Kafka Producer/Consumer 설정
│   ├── feign/                        # Feign Client 설정
│   ├── event/                        # 도메인 이벤트 발행 설정
│   └── swagger/                      # OpenAPI 문서 설정
│
└── common/                           # 공통 유틸리티
    ├── exception/                    # 예외 처리
    │   └── handler/                  # Global Exception Handler
    ├── domain/                       # 공통 도메인 인터페이스
    ├── idempotency/                  # 멱등성 유틸리티
    └── util/                         # 유틸리티 클래스
```

## 주요 기능

### 결제 생명주기

```
PAYMENT_WAIT → PAYMENT_REQUEST → PAYMENT_COMPLETED
                    ↓                    ↓
             PAYMENT_FAILED      REFUND_REQUESTED → REFUND_COMPLETED
                    ↓
             PAYMENT_CANCELLED
```

### API 엔드포인트

#### 내부 API (사용자/시스템)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/payments/request` | 결제 요청 |
| POST | `/api/v1/payments/{paymentId}/cancel` | 결제 취소 |
| POST | `/api/v1/payments/{paymentId}/refund/request` | 환불 요청 |
| GET | `/api/v1/payments/{paymentId}` | 결제 상세 조회 |
| GET | `/api/v1/payments` | 결제 목록 조회 |

#### 외부 API (PG 콜백)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/external/pg/callback/payment/complete` | 결제 완료 콜백 |
| POST | `/external/pg/callback/payment/refund` | 환불 완료 콜백 |
| POST | `/external/pg/callback/payment/fail` | 결제 실패 콜백 |

### 구독 이벤트 (Kafka)

| Topic | 설명 | 처리 |
|-------|------|------|
| `order.created` | 주문 생성됨 | Payment 레코드 생성 (PAYMENT_WAIT) |

### 발행 이벤트 (Kafka)

| Topic | 설명 | 구독자 |
|-------|------|--------|
| `payment.completed` | 결제 완료됨 | Order Service (주문 확정) |
| `payment.failed` | 결제 실패됨 | Order Service (주문 취소) |
| `payment.cancelled` | 결제 취소됨 | Order Service (주문 취소) |
| `refund.completed` | 환불 완료됨 | Order Service (환불 처리) |

## 레이어별 책임

### Domain Layer
- **비즈니스 규칙**: 결제 상태 전이, 금액 검증
- **외부 의존성 없음**: 순수 Kotlin 코드, 프레임워크 독립적
- **Port 정의**: 외부 시스템과의 계약(인터페이스) 정의

### Application Layer
- **유스케이스 조율**: 도메인 서비스, Port 호출 조합
- **트랜잭션 관리**: `@Transactional` 경계 설정
- **멱등성 보장**: 중복 PG 콜백 처리
- **분산 락**: 동시성 제어

### Adapter Layer
- **기술 구현**: JPA, Feign, Kafka, Redis 등
- **Port 구현**: 도메인 Port 인터페이스의 구체적 구현
- **외부 시스템 연동**: PG사 API, 메시지 발행, DB 접근

## 실행 방법

```bash
# 개발 환경 실행
./gradlew :payment-api:bootRun --args='--spring.profiles.active=local'

# 테스트 실행
./gradlew :payment-api:test

# 빌드
./gradlew :payment-api:build
```

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_MASTER_URL` | PostgreSQL Master URL | `jdbc:postgresql://payment-db:5432/payment_db` |
| `DB_REPLICA_URL` | PostgreSQL Replica URL | `jdbc:postgresql://payment-db:5432/payment_db` |
| `DB_USERNAME` | 데이터베이스 사용자 | `application` |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | `application` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 | `kafka:9092` |
| `SCHEMA_REGISTRY_URL` | Schema Registry URL | `http://schema-registry:8081` |
| `REDIS_HOST` | Redis 호스트 | `cache-redis` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `ORDER_SERVICE_URL` | Order Service URL | `http://order-api:8082` |

## 프로필 설정

| 프로필 | 용도 | 특징 |
|--------|------|------|
| `local` | 로컬 개발 | Docker Compose 기반, 상세 로깅 |
| `dev` | k3d 개발환경 | Kubernetes 환경, 내부 서비스 URL |
| `prod` | 운영환경 | 최소 로깅, 환경변수 기반 설정 |

## 의존 서비스

- **Order Service**: 주문 정보 조회, 주문 상태 연동
- **PG Gateway**: 결제 처리 (현재 Stub 구현)
