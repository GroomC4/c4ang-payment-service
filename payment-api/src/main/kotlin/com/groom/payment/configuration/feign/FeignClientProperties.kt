package com.groom.payment.configuration.feign

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Feign Client 설정을 관리하는 Properties
 *
 * application.yml에서 다음과 같이 설정:
 * ```yaml
 * feign:
 *   clients:
 *     store-service:
 *       url: http://localhost:8081
 *       connect-timeout: 5000
 *       read-timeout: 5000
 * ```
 */
@ConfigurationProperties(prefix = "feign.clients")
data class FeignClientProperties(
    val storeService: ServiceConfig = ServiceConfig(),
) {
    data class ServiceConfig(
        val url: String = "http://localhost:8081",
        val connectTimeout: Int = 5000,
        val readTimeout: Int = 5000,
        val loggerLevel: String = "basic",
    )
}
