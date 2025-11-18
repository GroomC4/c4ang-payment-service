package com.groom.payment.common

import com.groom.payment.common.annotation.IntegrationTest
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.web.context.WebApplicationContext

/**
 * Spring Cloud Contract Test를 위한 Base 클래스
 *
 * Contract 파일(YAML)을 기반으로 자동 생성된 테스트가 이 클래스를 상속받습니다.
 * - Provider 측(payment-service)에서 Contract를 검증
 * - 실제 통합 테스트 환경 사용
 * - 각 테스트 전에 테스트 데이터를 로드하고, 후에 정리
 */
@IntegrationTest
@AutoConfigureMockMvc
@SqlGroup(
    Sql(
        scripts = ["/sql/integration/payment-test-data.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    ),
    Sql(
        scripts = ["/sql/integration/cleanup.sql"],
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    ),
)
abstract class ContractTestBase {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @BeforeEach
    fun setup() {
        // RestAssured MockMvc 설정 (전체 애플리케이션 컨텍스트 사용)
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext)
    }
}
