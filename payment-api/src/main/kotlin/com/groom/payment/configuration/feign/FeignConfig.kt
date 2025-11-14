package com.groom.payment.configuration.feign

import feign.Logger
import feign.codec.ErrorDecoder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/**
 * Feign Client 공통 설정
 *
 * - Timeout, URL 등의 설정은 application.yml의 feign.client.config에서 관리
 * - FeignClientProperties는 프로퍼티 구조화 및 IDE 자동완성을 위한 용도
 */
@Configuration
@EnableConfigurationProperties(FeignClientProperties::class)
class FeignConfig {
    /**
     * Feign Logger 레벨 설정
     *
     * NONE: 로깅 없음
     * BASIC: 요청 메서드, URL, 응답 상태 코드, 실행 시간만 로깅
     * HEADERS: BASIC + 요청/응답 헤더
     * FULL: HEADERS + 요청/응답 본문
     *
     * 환경별 설정은 application.yml의 feign.client.config.default.loggerLevel에서 관리
     */
    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.BASIC

    /**
     * Feign 에러 디코더
     *
     * Feign Client 호출 실패 시 커스텀 예외 처리
     */
    @Bean
    fun errorDecoder(): ErrorDecoder =
        ErrorDecoder { methodKey, response ->
            logger.error { "Feign client error: $methodKey, status: ${response.status()}, reason: ${response.reason()}" }

            when (response.status()) {
                400 -> FeignClientException.BadRequest("Bad Request: ${response.reason()}")
                404 -> FeignClientException.NotFound("Resource not found: ${response.reason()}")
                500 -> FeignClientException.InternalServerError("Internal server error: ${response.reason()}")
                503 -> FeignClientException.ServiceUnavailable("Service unavailable: ${response.reason()}")
                else -> FeignClientException.Unknown("Unknown error: ${response.reason()}, status: ${response.status()}")
            }
        }
}

/**
 * Feign Client 호출 시 발생하는 커스텀 예외
 */
sealed class FeignClientException(
    message: String,
) : RuntimeException(message) {
    class BadRequest(
        message: String,
    ) : FeignClientException(message)

    class NotFound(
        message: String,
    ) : FeignClientException(message)

    class InternalServerError(
        message: String,
    ) : FeignClientException(message)

    class ServiceUnavailable(
        message: String,
    ) : FeignClientException(message)

    class Unknown(
        message: String,
    ) : FeignClientException(message)
}
