-- Payment Command Controller Integration Test Cleanup Script
-- 테스트 데이터 정리

-- 테스트 Payment History 데이터 삭제 (Payment 자식 엔티티)
DELETE FROM p_payment_history
WHERE payment_id IN (
    'cccccccc-cccc-cccc-cccc-111111111111'::uuid,
    'cccccccc-cccc-cccc-cccc-222222222222'::uuid,
    'cccccccc-cccc-cccc-cccc-333333333333'::uuid,
    'cccccccc-cccc-cccc-cccc-444444444444'::uuid
);

-- 테스트 Payment 데이터 삭제
DELETE FROM p_payment
WHERE id IN (
    'cccccccc-cccc-cccc-cccc-111111111111'::uuid,
    'cccccccc-cccc-cccc-cccc-222222222222'::uuid,
    'cccccccc-cccc-cccc-cccc-333333333333'::uuid,
    'cccccccc-cccc-cccc-cccc-444444444444'::uuid
);

-- 테스트 Order Item 데이터 삭제
DELETE FROM p_order_item
WHERE order_id IN (
    'dddddddd-dddd-dddd-dddd-111111111111'::uuid,
    'dddddddd-dddd-dddd-dddd-222222222222'::uuid,
    'dddddddd-dddd-dddd-dddd-333333333333'::uuid,
    'dddddddd-dddd-dddd-dddd-444444444444'::uuid
);

-- 테스트 Order 데이터 삭제
DELETE FROM p_order
WHERE id IN (
    'dddddddd-dddd-dddd-dddd-111111111111'::uuid,
    'dddddddd-dddd-dddd-dddd-222222222222'::uuid,
    'dddddddd-dddd-dddd-dddd-333333333333'::uuid,
    'dddddddd-dddd-dddd-dddd-444444444444'::uuid
);

-- 테스트 Product 데이터 삭제
DELETE FROM p_product
WHERE id IN (
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid
);

-- 테스트 Product Category 데이터 삭제
DELETE FROM p_product_category
WHERE id = 'dddddddd-dddd-dddd-dddd-000000000001'::uuid;

-- 테스트 Store 데이터 삭제
DELETE FROM p_store
WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-000000000001'::uuid;

-- 테스트 User 데이터 삭제
DELETE FROM p_user
WHERE id IN (
    'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
    'cccccccc-cccc-cccc-cccc-000000000002'::uuid
);