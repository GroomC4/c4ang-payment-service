package com.groom.payment.domain.service

import java.util.UUID

/**
 * Payment 분산 락 관리 도메인 서비스
 *
 * Payment 엔티티에 대한 동시성 제어를 담당합니다.
 *
 * 구현체:
 * - RedisPaymentLockManager: Redisson 분산 락 기반 구현
 *
 * 사용처:
 * - 모든 Command Application 서비스 (결제 상태 변경 작업)
 */
interface PaymentLockManager {
    /**
     * Payment에 대한 분산 락을 획득하고 작업 실행
     *
     * @param paymentId Payment ID
     * @param action 락 내에서 실행할 작업
     * @return 작업 결과
     * @throws IllegalStateException 락 획득 실패 시
     */
    fun <T> executeWithLock(
        paymentId: UUID,
        action: () -> T,
    ): T
}
