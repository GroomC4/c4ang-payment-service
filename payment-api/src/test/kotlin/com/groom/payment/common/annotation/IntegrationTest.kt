package com.groom.payment.common.annotation

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Payment Service 통합 테스트용 어노테이션
 *
 * Platform Core의 TestContainers가 자동으로 PostgreSQL과 Redis를 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @IntegrationTest
 * @AutoConfigureMockMvc
 * class PaymentControllerIntegrationTest {
 *     @Test
 *     fun `통합 테스트`() {
 *         // 테스트 로직
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
annotation class IntegrationTest
