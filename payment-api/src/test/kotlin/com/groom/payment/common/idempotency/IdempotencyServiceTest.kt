package com.groom.payment.common.idempotency

import com.groom.payment.common.annotation.UnitTest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@UnitTest
class IdempotencyServiceTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        Given("처음 요청하는 경우") {
            val redisTemplate = mockk<StringRedisTemplate>()
            val valueOps = mockk<ValueOperations<String, String>>()
            val service = IdempotencyService(redisTemplate)

            every { redisTemplate.opsForValue() } returns valueOps
            every { valueOps.setIfAbsent(any(), any(), any<Duration>()) } returns true

            When("멱등성을 확인하면") {
                val result = service.ensureIdempotency("test-key", Duration.ofMinutes(5))

                Then("처리 가능을 반환한다") {
                    result shouldBe true
                    verify(exactly = 1) { valueOps.setIfAbsent(any(), any(), any<Duration>()) }
                }
            }
        }

        Given("중복 요청인 경우") {
            val redisTemplate = mockk<StringRedisTemplate>()
            val valueOps = mockk<ValueOperations<String, String>>()
            val service = IdempotencyService(redisTemplate)

            every { redisTemplate.opsForValue() } returns valueOps
            every { valueOps.setIfAbsent(any(), any(), any<Duration>()) } returns false

            When("멱등성을 확인하면") {
                val result = service.ensureIdempotency("test-key")

                Then("중복 요청임을 반환한다") {
                    result shouldBe false
                    verify(exactly = 1) { valueOps.setIfAbsent(any(), any(), any<Duration>()) }
                }
            }
        }

        Given("idempotency key가 존재하는 경우") {
            val redisTemplate = mockk<StringRedisTemplate>()
            val service = IdempotencyService(redisTemplate)

            every { redisTemplate.hasKey(any()) } returns true

            When("존재 여부를 확인하면") {
                val result = service.exists("test-key")

                Then("true를 반환한다") {
                    result shouldBe true
                }
            }
        }
    })
