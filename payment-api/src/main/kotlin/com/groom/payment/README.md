# Payment 도메인 패키지 구조

## DDL 기반 테이블 구조
```sql
- p_payment: 주문 결제 내역
  - id (UUID PK)
  - order_id (UUID)
  - total_amount (NUMERIC(12,2))
  - payment_amount (NUMERIC(12,2))
  - discount_amount (NUMERIC(12,2))
  - delivery_fee (NUMERIC(12,2))
  - method (CARD, TOSS_PAY)
  - status (PAYMENT_WAIT, PAYMENT_REQUEST, PAYMENT_COMPLETED, PAYMENT_FAILED)
  - timeline (JSON)
  - requested_at (TIMESTAMPTZ)
  - completed_at (TIMESTAMPTZ)

- p_payment_gateway_log: PG사 통신 이력
  - id (UUID PK)
  - payment_id (UUID)
  - pg_code (TEXT)
  - status (REQUEST, APPROVED, FAILED)
  - external_payment_data (TEXT)

- p_payment_history: 결제 및 환불 이력
  - id (UUID PK)
  - payment_id (UUID)
  - event_type (PAYMENT, CANCEL)
  - metadata (JSONB)
```

## 패키지 구조 (User 패키지 참고)

### common/enums/
- **PaymentMethod.kt**: CARD, TOSS_PAY
- **PaymentStatus.kt**: PAYMENT_WAIT, PAYMENT_REQUEST, PAYMENT_COMPLETED, PAYMENT_FAILED
- **PaymentGatewayStatus.kt**: REQUEST, APPROVED, FAILED

### domain/model/
- **Payment.kt**: 애그리게이트 루트 (UUID PK)
- **PaymentGatewayLog.kt**: PG사 통신 로그 엔티티
- **PaymentHistory.kt**: 결제 이력 엔티티
- **Money.kt**: Value Object (금액)
- **PaymentTimeline.kt**: Value Object (결제 타임라인 JSON)

### domain/service/
- **PaymentFactory.kt**: Payment 애그리게이트 생성
- **PaymentPolicy.kt**: 결제 금액 검증, 중복 결제 방지
- **PaymentReader.kt**: 결제 조회 서비스

### infrastructure/repository/
- **PaymentRepositoryImpl.kt**: JpaRepository<Payment, UUID>
- **PaymentGatewayLogRepositoryImpl.kt**: JpaRepository<PaymentGatewayLog, UUID>

### infrastructure/adapter/
- **PaymentGatewayAdapter.kt**: 외부 PG사 연동 (Toss Payments, etc.)

### application/dto/
- **ProcessPaymentCommand.kt**: 결제 처리 커맨드
- **ProcessPaymentResult.kt**: 결제 처리 결과

### application/service/
- **ProcessPaymentService.kt**: 결제 처리 유스케이스

### presentation/web/dto/
- **ProcessPaymentRequest.kt**: API 요청 DTO
- **ProcessPaymentResponse.kt**: API 응답 DTO

### presentation/web/
- **PaymentController.kt**: POST /api/v1/payments
