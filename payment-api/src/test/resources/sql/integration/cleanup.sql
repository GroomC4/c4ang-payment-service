-- 테스트 데이터 정리
-- 각 테스트 후 실행하여 데이터베이스 상태 초기화

DELETE FROM payment_histories;
DELETE FROM payment_gateway_logs;
DELETE FROM payments;

-- 시퀀스 초기화 (필요한 경우)
-- ALTER SEQUENCE payments_seq RESTART WITH 1;
