package com.groom.payment.common.exception

import java.util.UUID

/**
 * 도메인 예외의 최상위 sealed class
 *
 * 모든 비즈니스 예외는 이 클래스를 상속받아 정의됩니다.
 * sealed class를 사용하여 컴파일 타임에 모든 예외 타입을 exhaustive하게 처리할 수 있습니다.
 */
sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * 인증 관련 예외
 */
sealed class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 이메일로 사용자를 찾을 수 없는 경우
     * @param email 찾을 수 없는 사용자의 이메일
     */
    data class UserNotFoundByEmail(
        val email: String,
    ) : AuthenticationException("일치하는 이메일의 사용자가 없습니다: $email")

    /**
     * 비밀번호가 일치하지 않는 경우
     * @param email 인증을 시도한 사용자의 이메일
     */
    data class InvalidPassword(
        val email: String,
    ) : AuthenticationException("비밀번호가 일치하지 않습니다")

    /**
     * 인증 정보가 올바르지 않은 경우
     * @param clue 디버깅을 위한 추가 정보
     */
    data class InvalidCredentials(
        val clue: Map<String, Any> = emptyMap(),
    ) : AuthenticationException("인증 정보가 올바르지 않습니다")
}

/**
 * 토큰 관련 예외
 */
sealed class TokenException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 토큰이 만료된 경우
     */
    class TokenExpired : TokenException("토큰이 만료되었습니다. 다시 로그인해주세요.")

    /**
     * 토큰 서명이 올바르지 않은 경우
     * @param cause 원인이 되는 예외
     */
    data class InvalidTokenSignature(
        override val cause: Throwable?,
    ) : TokenException("인증에 실패하였습니다.", cause)

    /**
     * 토큰 형식이 올바르지 않은 경우
     * @param cause 원인이 되는 예외
     */
    data class InvalidTokenFormat(
        override val cause: Throwable?,
    ) : TokenException("토큰 형식이 올바르지 않습니다.", cause)

    /**
     * 토큰 알고리즘이 올바르지 않은 경우
     * @param cause 원인이 되는 예외
     */
    data class InvalidTokenAlgorithm(
        override val cause: Throwable?,
    ) : TokenException("토큰 알고리즘이 올바르지 않습니다.", cause)

    /**
     * 토큰 발급자가 올바르지 않은 경우
     * @param expected 기대되는 발급자
     * @param actual 실제 발급자
     */
    data class InvalidTokenIssuer(
        val expected: String,
        val actual: String,
    ) : TokenException("토큰 발급자가 올바르지 않습니다. 기대값: $expected, 실제값: $actual")

    /**
     * 필수 토큰 클레임이 없는 경우
     * @param claimName 없는 클레임의 이름
     */
    data class MissingTokenClaim(
        val claimName: String,
    ) : TokenException("필수 토큰 클레임이 없습니다: $claimName")

    /**
     * 인증 토큰이 제공되지 않은 경우
     */
    class MissingToken : TokenException("인증 토큰이 없습니다.")
}

/**
 * 리프레시 토큰 관련 예외
 */
sealed class RefreshTokenException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 리프레시 토큰을 찾을 수 없는 경우
     * @param tokenValue 찾을 수 없는 토큰 값 (로깅용)
     */
    data class RefreshTokenNotFound(
        val tokenValue: String,
    ) : RefreshTokenException("리프레시 토큰을 찾을 수 없습니다")

    /**
     * 리프레시 토큰이 만료된 경우
     */
    class RefreshTokenExpired : RefreshTokenException("리프레시 토큰이 만료되었습니다")

    /**
     * 리프레시 토큰이 사용자와 일치하지 않는 경우
     * @param userId 사용자 ID
     */
    data class RefreshTokenMismatch(
        val userId: UUID,
    ) : RefreshTokenException("리프레시 토큰이 일치하지 않습니다")
}

/**
 * 사용자 관련 예외
 */
sealed class UserException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 이메일이 중복된 경우
     * @param email 중복된 이메일
     */
    data class DuplicateEmail(
        val email: String,
    ) : UserException("이미 존재하는 이메일입니다: $email")

    /**
     * 사용자를 찾을 수 없는 경우
     * @param userId 찾을 수 없는 사용자 ID
     */
    data class UserNotFound(
        val userId: UUID,
    ) : UserException("사용자를 찾을 수 없습니다: $userId")

    /**
     * 사용자가 이미 존재하는 경우
     * @param identifier 중복된 식별자
     */
    data class UserAlreadyExists(
        val identifier: String,
    ) : UserException("사용자가 이미 존재합니다: $identifier")

    /**
     * 작업을 수행할 권한이 부족한 경우
     * @param userId 사용자 ID
     * @param requiredRole 필요한 역할
     * @param currentRole 현재 역할
     */
    data class InsufficientPermission(
        val userId: UUID,
        val requiredRole: String,
        val currentRole: String,
    ) : UserException("이 작업을 수행할 권한이 없습니다.")
}

/**
 * 권한 관련 예외
 */
sealed class PermissionException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 접근이 거부된 경우
     * @param resource 접근하려는 리소스
     * @param userId 사용자 ID
     */
    data class AccessDenied(
        val resource: String,
        val userId: UUID,
    ) : PermissionException("접근이 거부되었습니다")

    /**
     * 권한이 부족한 경우
     * @param required 필요한 권한
     * @param userId 사용자 ID
     */
    data class InsufficientPermissions(
        val required: String,
        val userId: UUID,
    ) : PermissionException("권한이 부족합니다")
}

/**
 * 리소스 관련 예외
 */
sealed class ResourceException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 리소스를 찾을 수 없는 경우
     * @param resourceType 리소스 타입
     * @param identifier 리소스 식별자
     * @param clue 디버깅을 위한 추가 정보
     */
    data class ResourceNotFound(
        val resourceType: String,
        val identifier: String,
        val clue: Map<String, Any> = emptyMap(),
    ) : ResourceException("$resourceType 를 찾을 수 없습니다: $identifier")

    /**
     * 리소스가 이미 존재하는 경우
     * @param resourceType 리소스 타입
     * @param identifier 리소스 식별자
     * @param clue 디버깅을 위한 추가 정보
     */
    data class ResourceAlreadyExists(
        val resourceType: String,
        val identifier: String,
        val clue: Map<String, Any> = emptyMap(),
    ) : ResourceException("$resourceType 가 이미 존재합니다: $identifier")

    /**
     * 리소스 충돌이 발생한 경우
     * @param resourceType 리소스 타입
     * @param reason 충돌 이유
     * @param clue 디버깅을 위한 추가 정보
     */
    data class ResourceConflict(
        val resourceType: String,
        val reason: String,
        val clue: Map<String, Any> = emptyMap(),
    ) : ResourceException("$resourceType 충돌: $reason")
}
