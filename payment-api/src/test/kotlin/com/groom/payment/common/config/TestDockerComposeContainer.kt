package com.groom.payment.common.config

import com.groom.payment.common.extension.PaymentServiceContainerExtension
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * TestDockerComposeContainer는 PaymentServiceContainerExtension의 Wrapper 역할을 합니다.
 * 실제 컨테이너 생명주기 관리는 PaymentServiceContainerExtension이 담당하고,
 * 이 클래스는 기존 코드와의 호환성을 위해 유지됩니다.
 */
@Profile("test")
@Configuration("TestDockerComposeContainer")
class TestDockerComposeContainer {
    companion object {
        const val POSTGRESQL_USERNAME = "test"
        const val POSTGRESQL_PASSWORD = "test"

        /**
         * Master(Primary) 데이터베이스 JDBC URL을 반환합니다.
         * PaymentServiceContainerExtension에 위임합니다.
         */
        fun getMasterJdbcUrl(): String = PaymentServiceContainerExtension.getPrimaryJdbcUrl()

        /**
         * Replica 데이터베이스 JDBC URL을 반환합니다.
         * PaymentServiceContainerExtension에 위임합니다.
         */
        fun getReplicaJdbcUrl(): String = PaymentServiceContainerExtension.getReplicaJdbcUrl()

        /**
         * Redis 호스트를 반환합니다.
         * PaymentServiceContainerExtension에 위임합니다.
         */
        fun getRedisHost(): String = PaymentServiceContainerExtension.getRedisHost()

        /**
         * Redis 포트를 반환합니다.
         * PaymentServiceContainerExtension에 위임합니다.
         */
        fun getRedisMappedPort(): Int = PaymentServiceContainerExtension.getRedisPort()
    }
}
