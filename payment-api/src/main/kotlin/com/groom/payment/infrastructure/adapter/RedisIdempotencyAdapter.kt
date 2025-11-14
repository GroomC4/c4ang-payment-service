package com.groom.payment.infrastructure.adapter

import com.groom.payment.domain.port.IdempotencyPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis 기반 멱등성 검증 Adapter
 *
 * IdempotencyPort의 구현체
 *
 * 기술 스택:
 * - Redis SETNX (SET if Not eXists)
 * - TTL 기반 자동 만료
 *
 * 동작 방식:
 * 1. idempotency:${key} 키로 Redis에 SETNX 시도
 * 2. 성공 (키 없음) → true 반환 (신규 요청, 처리 진행)
 * 3. 실패 (키 존재) → false 반환 (중복 요청, 처리 중단)
 */
@Component
class RedisIdempotencyAdapter(
    private val redisTemplate: StringRedisTemplate,
) : IdempotencyPort {
    private val logger = KotlinLogging.logger {}

    override fun ensureIdempotency(
        key: String,
        ttl: Duration,
    ): Boolean {
        val idempotencyKey = "idempotency:$key"

        return try {
            // SET NX EX: key가 없을 때만 설정 (원자적 연산)
            val result =
                redisTemplate.opsForValue().setIfAbsent(
                    idempotencyKey,
                    "1",
                    ttl,
                )

            if (result == false) {
                logger.info { "Duplicate request detected (Payment domain): $key" }
            }

            result ?: false
        } catch (e: Exception) {
            logger.error(e) { "Failed to check idempotency (Payment domain): $key" }
            // 예외 발생 시 안전하게 true 반환 (요청 허용)
            true
        }
    }
}
