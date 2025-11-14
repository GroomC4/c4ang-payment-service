package com.groom.payment.common.annotation

import com.groom.payment.common.extension.PaymentServiceContainerExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Payment Service 통합 테스트용 어노테이션
 *
 * Payment Service에 필요한 컨테이너 Extension과 설정을 포함합니다.
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
@ExtendWith(PaymentServiceContainerExtension::class)
annotation class IntegrationTest
