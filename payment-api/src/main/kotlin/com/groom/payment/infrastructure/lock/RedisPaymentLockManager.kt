package com.groom.payment.infrastructure.lock

import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Redisson 기반 Payment 분산 락 관리자
 *
 * PaymentLockManager의 구현체
 *
 * 기술 스택:
 * - Redisson: Redis 기반 분산 락
 *
 * 동작 방식:
 * 1. lock:payment:{paymentId} 키로 락 획득 시도
 * 2. 대기 시간: 5초 (다른 트랜잭션 완료 대기)
 * 3. 락 보유 시간: 30초 (장시간 트랜잭션 방지)
 * 4. 작업 완료 후 자동 락 해제 (finally)
 */
@Component
class RedisPaymentLockManager(
    private val redissonClient: RedissonClient,
) : PaymentLockManager {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val LOCK_WAIT_TIME = 5L // 락 대기 시간 (초)
        private const val LOCK_LEASE_TIME = 30L // 락 보유 시간 (초)
    }

    override fun <T> executeWithLock(
        paymentId: UUID,
        action: () -> T,
    ): T {
        val lockKey = "lock:payment:$paymentId"
        val lock = redissonClient.getLock(lockKey)

        return try {
            // 분산 락 획득 시도
            val acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)

            if (!acquired) {
                logger.error { "Failed to acquire lock for payment: $paymentId after ${LOCK_WAIT_TIME}s" }
                throw IllegalStateException("Failed to acquire lock for payment: $paymentId")
            }

            logger.debug { "Lock acquired for payment: $paymentId" }

            // 락 내에서 작업 실행
            action()
        } finally {
            // 락 해제 (현재 스레드가 소유한 경우만)
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                logger.debug { "Lock released for payment: $paymentId" }
            }
        }
    }
}
