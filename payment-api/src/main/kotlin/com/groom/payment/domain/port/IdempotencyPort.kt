package com.groom.payment.domain.port

import java.time.Duration

/**
 * 멱등성 검증 Port (Hexagonal Architecture)
 *
 * PG 콜백 중복 처리 방지를 위한 멱등성 키 관리
 *
 * 구현체:
 * - RedisIdempotencyAdapter: Redis SETNX 기반 구현
 *
 * 사용처:
 * - CompletePaymentService: 결제 완료 콜백 중복 방지
 * - CompletePaymentRefundService: 환불 완료 콜백 중복 방지
 */
interface IdempotencyPort {
    /**
     * 멱등성 키를 확인하고 등록
     *
     * @param key 멱등성 키 (예: PG-CALLBACK-20251016-001)
     * @param ttl 키 유효 시간
     * @return true: 신규 요청 (처리 진행), false: 중복 요청 (처리 중단)
     */
    fun ensureIdempotency(
        key: String,
        ttl: Duration,
    ): Boolean
}
