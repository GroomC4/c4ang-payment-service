package com.groom.payment.common.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Istio API Gateway가 주입한 인증 헤더를 추출하는 유틸리티
 *
 * Istio는 JWT 검증 후 다음 헤더를 주입합니다:
 * - X-User-Id: 사용자 UUID
 * - X-User-Role: 사용자 역할 (CUSTOMER, OWNER 등)
 *
 * 이 서비스는 반드시 Istio API Gateway를 통해서만 접근되어야 하며,
 * 헤더가 없는 경우 인증 실패로 간주됩니다.
 */
@Component
class IstioHeaderExtractor {
    companion object {
        const val USER_ID_HEADER = "X-User-Id"
        const val USER_ROLE_HEADER = "X-User-Role"
    }

    /**
     * Istio가 JWT 검증 후 주입한 사용자 ID를 추출합니다.
     *
     * @param request HTTP 요청
     * @return 사용자 UUID
     * @throws IllegalStateException 헤더가 없거나 형식이 잘못된 경우
     */
    fun extractUserId(request: HttpServletRequest): UUID {
        val userId =
            request.getHeader(USER_ID_HEADER)
                ?: throw IllegalStateException(
                    "$USER_ID_HEADER header not found. " +
                        "Request must pass through Istio API Gateway with valid JWT authentication.",
                )

        return try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid user ID format in $USER_ID_HEADER: $userId", e)
        }
    }

    /**
     * Istio가 JWT 검증 후 주입한 사용자 역할을 추출합니다.
     *
     * @param request HTTP 요청
     * @return 사용자 역할 (예: "CUSTOMER", "OWNER")
     * @throws IllegalStateException 헤더가 없는 경우
     */
    fun extractUserRole(request: HttpServletRequest): String =
        request.getHeader(USER_ROLE_HEADER)
            ?: throw IllegalStateException(
                "$USER_ROLE_HEADER header not found. " +
                    "Request must pass through Istio API Gateway with valid JWT authentication.",
            )

    /**
     * 사용자 ID와 역할을 함께 추출합니다.
     *
     * @param request HTTP 요청
     * @return Pair<UUID, String> (userId, userRole)
     */
    fun extractUserInfo(request: HttpServletRequest): Pair<UUID, String> {
        val userId = extractUserId(request)
        val userRole = extractUserRole(request)
        return Pair(userId, userRole)
    }
}
