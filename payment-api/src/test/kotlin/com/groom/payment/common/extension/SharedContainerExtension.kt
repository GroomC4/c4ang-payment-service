package com.groom.payment.common.extension

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

/**
 * Payment Service용 통합 테스트 컨테이너 Extension
 *
 * Testcontainers를 사용하여 PostgreSQL과 Redis를 실행합니다.
 */
class PaymentServiceContainerExtension : BeforeAllCallback {
    companion object {
        private var container: DockerComposeContainer<*>? = null
        private var initialized = false

        private const val POSTGRES_SERVICE = "postgres-primary"
        private const val POSTGRES_REPLICA_SERVICE = "postgres-replica"
        private const val REDIS_SERVICE = "redis"
        private const val POSTGRES_PORT = 5432
        private const val REDIS_PORT = 6379

        fun getPrimaryJdbcUrl(): String {
            val host = container?.getServiceHost(POSTGRES_SERVICE, POSTGRES_PORT) ?: "localhost"
            val port = container?.getServicePort(POSTGRES_SERVICE, POSTGRES_PORT) ?: 5432
            return "jdbc:postgresql://$host:$port/payment_test"
        }

        fun getReplicaJdbcUrl(): String {
            val host = container?.getServiceHost(POSTGRES_REPLICA_SERVICE, POSTGRES_PORT) ?: "localhost"
            val port = container?.getServicePort(POSTGRES_REPLICA_SERVICE, POSTGRES_PORT) ?: 5433
            return "jdbc:postgresql://$host:$port/payment_test"
        }

        fun getRedisHost(): String = container?.getServiceHost(REDIS_SERVICE, REDIS_PORT) ?: "localhost"

        fun getRedisPort(): Int = container?.getServicePort(REDIS_SERVICE, REDIS_PORT) ?: 6379
    }

    override fun beforeAll(context: ExtensionContext) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    startContainer()
                    initialized = true
                }
            }
        }
    }

    private fun startContainer() {
        val composeFile = findComposeFile()
        if (composeFile != null && composeFile.exists()) {
            container =
                DockerComposeContainer(composeFile)
                    .withExposedService(
                        POSTGRES_SERVICE,
                        POSTGRES_PORT,
                        Wait
                            .forListeningPort()
                            .withStartupTimeout(Duration.ofSeconds(60)),
                    ).withExposedService(
                        POSTGRES_REPLICA_SERVICE,
                        POSTGRES_PORT,
                        Wait
                            .forListeningPort()
                            .withStartupTimeout(Duration.ofSeconds(60)),
                    ).withExposedService(
                        REDIS_SERVICE,
                        REDIS_PORT,
                        Wait
                            .forListeningPort()
                            .withStartupTimeout(Duration.ofSeconds(30)),
                    ).apply {
                        start()
                    }
        }
    }

    private fun findComposeFile(): File? {
        // 프로젝트 루트에서 docker-compose 파일 찾기
        val possiblePaths =
            listOf(
                "docker-compose.test.yml",
                "docker-compose-test.yml",
                "test/docker-compose.yml",
                "../docker-compose.test.yml",
            )

        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                return file
            }
        }
        return null
    }
}

/**
 * 하위 호환성을 위한 별칭
 * 기존 테스트 코드에서 SharedContainerExtension을 사용하는 경우를 대비
 */
@Deprecated(
    message = "Use PaymentServiceContainerExtension instead",
    replaceWith = ReplaceWith("PaymentServiceContainerExtension"),
)
typealias SharedContainerExtension = PaymentServiceContainerExtension
