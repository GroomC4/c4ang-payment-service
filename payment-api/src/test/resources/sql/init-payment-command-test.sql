-- Payment Command Controller Integration Test Init Script
-- 테스트 데이터 초기화

-- 테스트 User 데이터 생성 (인증이 필요한 경우)
INSERT INTO p_user (id, username, email, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('cccccccc-cccc-cccc-cccc-000000000001'::uuid, 'testuser1', 'testuser1@example.com',
     '$2a$10$encrypted', 'CUSTOMER', true, NOW(), NOW()),
    ('cccccccc-cccc-cccc-cccc-000000000002'::uuid, 'testuser2', 'testuser2@example.com',
     '$2a$10$encrypted', 'CUSTOMER', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 테스트 Store 데이터 생성
INSERT INTO p_store (id, owner_user_id, name, description, status, created_at, updated_at)
VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid, 'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
     'Test Electronics Store', 'Payment 테스트용 상점', 'REGISTERED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 테스트 Product Category 데이터 생성
INSERT INTO p_product_category (id, name, depth, created_at, updated_at)
VALUES
    ('dddddddd-dddd-dddd-dddd-000000000001'::uuid, '전자제품', 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 테스트 Product 데이터 생성
INSERT INTO p_product (id, store_id, store_name, category_id, product_name, description, price, status, stock_quantity, created_at, updated_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid, 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid,
     'Test Electronics Store', 'dddddddd-dddd-dddd-dddd-000000000001'::uuid,
     '무선 마우스', 'Payment 테스트용 마우스', 50000, 'ON_SALE', 100, NOW(), NOW()),
    ('aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid, 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid,
     'Test Electronics Store', 'dddddddd-dddd-dddd-dddd-000000000001'::uuid,
     '기계식 키보드', 'Payment 테스트용 키보드', 120000, 'ON_SALE', 50, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 테스트 Order 데이터 생성
INSERT INTO p_order (id, user_id, store_id, order_number, status, payment_summary, timeline,
                     payment_id, reservation_id, expires_at, created_at, updated_at)
VALUES
    -- ORDER_PAYMENT_WAIT (PAYMENT_PENDING, 결제 요청 전)
    ('dddddddd-dddd-dddd-dddd-111111111111'::uuid, 'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
     'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid, 'ORD-TEST-WAIT-001', 'PAYMENT_PENDING',
     '{}', '[]', 'cccccccc-cccc-cccc-cccc-111111111111'::uuid, 'RES-WAIT-001',
     NOW() + INTERVAL '10 minutes', NOW(), NOW()),

    -- ORDER_PAYMENT_REQUEST (PAYMENT_PENDING, PG 요청 완료)
    ('dddddddd-dddd-dddd-dddd-222222222222'::uuid, 'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
     'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid, 'ORD-TEST-REQ-002', 'PAYMENT_PENDING',
     '{}', '[]', 'cccccccc-cccc-cccc-cccc-222222222222'::uuid, 'RES-REQ-002',
     NOW() + INTERVAL '10 minutes', NOW(), NOW()),

    -- ORDER_PAYMENT_COMPLETED (PAYMENT_COMPLETED, 결제 완료)
    ('dddddddd-dddd-dddd-dddd-333333333333'::uuid, 'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
     'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid, 'ORD-TEST-COMP-003', 'PAYMENT_COMPLETED',
     '{}', '[]', 'cccccccc-cccc-cccc-cccc-333333333333'::uuid, NULL, NULL, NOW(), NOW()),

    -- ORDER_DELIVERED (DELIVERED, 환불 가능)
    ('dddddddd-dddd-dddd-dddd-444444444444'::uuid, 'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
     'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid, 'ORD-TEST-DEL-004', 'DELIVERED',
     '{}', '[]', 'cccccccc-cccc-cccc-cccc-444444444444'::uuid, NULL, NULL, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 테스트 Order Item 데이터 생성
INSERT INTO p_order_item (id, order_id, product_id, product_name, unit_price, quantity, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-111111111111'::uuid,
     'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid, '무선 마우스', 50000, 2, NOW(), NOW()),
    (gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-222222222222'::uuid,
     'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid, '무선 마우스', 50000, 2, NOW(), NOW()),
    (gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-333333333333'::uuid,
     'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid, '기계식 키보드', 120000, 1, NOW(), NOW()),
    (gen_random_uuid(), 'dddddddd-dddd-dddd-dddd-444444444444'::uuid,
     'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid, '무선 마우스', 50000, 2, NOW(), NOW());

-- 테스트 Payment 데이터 생성 (Order 생성 후)
INSERT INTO p_payment (id, version, order_id, total_amount, payment_amount, discount_amount, delivery_fee,
                      method, status, timeline, requested_at, pg_transaction_id, pg_approval_number,
                      completed_at, created_at, updated_at)
VALUES
    -- PAYMENT_WAIT (PAYMENT_WAIT, 결제 요청 전)
    ('cccccccc-cccc-cccc-cccc-111111111111'::uuid, 0, 'dddddddd-dddd-dddd-dddd-111111111111'::uuid,
     170000, 170000, 0, 0, 'CARD', 'PAYMENT_WAIT', '[]'::json, NOW(), NULL, NULL, NULL, NOW(), NOW()),

    -- PAYMENT_REQUEST (PAYMENT_REQUEST, PG 요청 완료)
    ('cccccccc-cccc-cccc-cccc-222222222222'::uuid, 0, 'dddddddd-dddd-dddd-dddd-222222222222'::uuid,
     50000, 50000, 0, 0, 'CARD', 'PAYMENT_REQUEST', '[]'::json, NOW(), 'PG-TX-002', NULL, NULL, NOW(), NOW()),

    -- PAYMENT_COMPLETED (PAYMENT_COMPLETED, 결제 완료)
    ('cccccccc-cccc-cccc-cccc-333333333333'::uuid, 0, 'dddddddd-dddd-dddd-dddd-333333333333'::uuid,
     120000, 120000, 0, 0, 'CARD', 'PAYMENT_COMPLETED', '[]'::json, NOW(), 'PG-TX-003', 'PG-APPROVAL-003', NOW(), NOW(), NOW()),

    -- PAYMENT_FOR_REFUND (PAYMENT_COMPLETED, 환불 가능)
    ('cccccccc-cccc-cccc-cccc-444444444444'::uuid, 0, 'dddddddd-dddd-dddd-dddd-444444444444'::uuid,
     100000, 100000, 0, 0, 'CARD', 'PAYMENT_COMPLETED', '[]'::json, NOW(), 'PG-TX-004', 'PG-APPROVAL-004', NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;