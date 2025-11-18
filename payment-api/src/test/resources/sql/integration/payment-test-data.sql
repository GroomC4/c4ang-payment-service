-- Payment 테스트 데이터
-- Contract Test 및 Integration Test에서 사용

-- 결제 완료 테스트 데이터
INSERT INTO payments (payment_id, order_id, user_id, total_amount, payment_method, payment_status, pg_approval_number, created_at, updated_at)
VALUES ('PAY-12345', 'ORD-12345', 'USER-001', 50000, 'CARD', 'COMPLETED', 'APPROVE-12345', NOW(), NOW());

INSERT INTO payment_histories (history_id, payment_id, status, changed_at, reason)
VALUES ('HIST-001', 'PAY-12345', 'COMPLETED', NOW(), '결제 완료');

-- 결제 실패 테스트 데이터
INSERT INTO payments (payment_id, order_id, user_id, total_amount, payment_method, payment_status, created_at, updated_at)
VALUES ('PAY-12346', 'ORD-12346', 'USER-002', 30000, 'CARD', 'FAILED', NOW(), NOW());

INSERT INTO payment_histories (history_id, payment_id, status, changed_at, reason)
VALUES ('HIST-002', 'PAY-12346', 'FAILED', NOW(), '카드 승인 거부');

-- 결제 취소 테스트 데이터
INSERT INTO payments (payment_id, order_id, user_id, total_amount, payment_method, payment_status, pg_approval_number, created_at, updated_at)
VALUES ('PAY-12347', 'ORD-12347', 'USER-003', 70000, 'CARD', 'CANCELLED', 'APPROVE-12347', NOW(), NOW());

INSERT INTO payment_histories (history_id, payment_id, status, changed_at, reason)
VALUES ('HIST-003', 'PAY-12347', 'CANCELLED', NOW(), '재고 부족으로 취소');
