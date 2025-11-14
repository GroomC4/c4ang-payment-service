package com.groom.payment.common.exception.handler

import com.groom.payment.common.exception.AuthenticationException
import com.groom.payment.common.exception.ErrorCode
import com.groom.payment.common.exception.PermissionException
import com.groom.payment.common.exception.RefreshTokenException
import com.groom.payment.common.exception.ResourceException
import com.groom.payment.common.exception.UserException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code = ErrorCode.INVALID_REQUEST_PARAMETER,
                message = e.message ?: "잘못된 요청입니다.",
            ),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code = ErrorCode.INVALID_REQUEST_STATE,
                message = e.message ?: "요청을 처리하는데 실패하였습니다.",
            ),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )

    /**
     * 인증 관련 예외 처리
     *
     * 발생 위치:
     * - CustomerAuthenticationService.login(): UserNotFoundByEmail, InvalidPassword
     * - OwnerAuthenticationService.login(): UserNotFoundByEmail, InvalidPassword
     *
     * 참고: TokenException은 필터 레이어에서만 발생하므로 CustomAuthenticationEntryPoint에서 처리
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(e: AuthenticationException): ResponseEntity<ErrorResponse> {
        val errorCode =
            when (e) {
                is AuthenticationException.UserNotFoundByEmail -> {
                    logger.warn(e) { "User not found by email: ${e.email}" }
                    ErrorCode.USER_NOT_FOUND_BY_EMAIL
                }
                is AuthenticationException.InvalidPassword -> {
                    logger.warn(e) { "Invalid password for email: ${e.email}" }
                    ErrorCode.INVALID_PASSWORD
                }
                is AuthenticationException.InvalidCredentials -> {
                    logger.warn(e) { "Invalid credentials: clue=${e.clue}" }
                    ErrorCode.INVALID_CREDENTIALS
                }
            }

        return ResponseEntity(
            ErrorResponse(
                code = errorCode,
                message = e.message ?: "인증에 실패하였습니다.",
            ),
            HttpStatus.UNAUTHORIZED,
        )
    }

    /**
     * 리프레시 토큰 관련 예외 처리
     *
     * 발생 위치:
     * - RefreshTokenService.refresh() → Authenticator.refreshCredentials()
     *   - RefreshTokenNotFound: DB에 리프레시 토큰이 없는 경우
     *   - RefreshTokenExpired: 리프레시 토큰 만료
     */
    @ExceptionHandler(RefreshTokenException::class)
    fun handleRefreshTokenException(e: RefreshTokenException): ResponseEntity<ErrorResponse> {
        val errorCode =
            when (e) {
                is RefreshTokenException.RefreshTokenNotFound -> {
                    logger.warn(e) { "Refresh token not found" }
                    ErrorCode.REFRESH_TOKEN_NOT_FOUND
                }
                is RefreshTokenException.RefreshTokenExpired -> {
                    logger.warn(e) { "Refresh token expired" }
                    ErrorCode.REFRESH_TOKEN_EXPIRED
                }
                is RefreshTokenException.RefreshTokenMismatch -> {
                    logger.warn(e) { "Refresh token mismatch for userId: ${e.userId}" }
                    ErrorCode.REFRESH_TOKEN_MISMATCH
                }
            }

        return ResponseEntity(
            ErrorResponse(
                code = errorCode,
                message = e.message ?: "리프레시 토큰 검증에 실패하였습니다.",
            ),
            HttpStatus.UNAUTHORIZED,
        )
    }

    /**
     * 사용자 관련 예외 처리
     *
     * 발생 위치:
     * - UserPolicy.checkAlreadyRegister(): DuplicateEmail
     * - CustomerAuthenticationService.logout(): UserNotFound
     * - OwnerAuthenticationService.logout(): UserNotFound
     * - StoreController.registerStore(): InsufficientPermission
     */
    @ExceptionHandler(UserException::class)
    fun handleUserException(e: UserException): ResponseEntity<ErrorResponse> {
        val (errorCode, httpStatus) =
            when (e) {
                is UserException.DuplicateEmail -> {
                    logger.warn(e) { "Duplicate email: ${e.email}" }
                    ErrorCode.DUPLICATE_EMAIL to HttpStatus.CONFLICT
                }
                is UserException.UserNotFound -> {
                    logger.warn(e) { "User not found: ${e.userId}" }
                    ErrorCode.USER_NOT_FOUND to HttpStatus.NOT_FOUND
                }
                is UserException.UserAlreadyExists -> {
                    logger.warn(e) { "User already exists: ${e.identifier}" }
                    ErrorCode.USER_ALREADY_EXISTS to HttpStatus.CONFLICT
                }
                is UserException.InsufficientPermission -> {
                    logger.warn(e) { "Insufficient permission: userId=${e.userId}, required=${e.requiredRole}, current=${e.currentRole}" }
                    ErrorCode.INSUFFICIENT_PERMISSION to HttpStatus.FORBIDDEN
                }
            }

        return ResponseEntity(
            ErrorResponse(
                code = errorCode,
                message = e.message ?: "사용자 처리 중 오류가 발생하였습니다.",
            ),
            httpStatus,
        )
    }

    @ExceptionHandler(PermissionException::class)
    fun handlePermissionException(e: PermissionException): ResponseEntity<ErrorResponse> {
        val errorCode =
            when (e) {
                is PermissionException.AccessDenied -> {
                    logger.warn(e) { "Access denied: userId=${e.userId}, resource=${e.resource}" }
                    ErrorCode.ACCESS_DENIED
                }
                is PermissionException.InsufficientPermissions -> {
                    logger.warn(e) { "Insufficient permissions: userId=${e.userId}, required=${e.required}" }
                    ErrorCode.INSUFFICIENT_PERMISSIONS
                }
            }

        return ResponseEntity(
            ErrorResponse(
                code = errorCode,
                message = e.message ?: "권한이 부족합니다.",
            ),
            HttpStatus.FORBIDDEN,
        )
    }

    @ExceptionHandler(ResourceException::class)
    fun handleResourceException(e: ResourceException): ResponseEntity<ErrorResponse> {
        val (errorCode, httpStatus) =
            when (e) {
                is ResourceException.ResourceNotFound -> {
                    logger.warn(e) { "Resource not found: type=${e.resourceType}, id=${e.identifier}, clue=${e.clue}" }
                    ErrorCode.RESOURCE_NOT_FOUND to HttpStatus.NOT_FOUND
                }
                is ResourceException.ResourceAlreadyExists -> {
                    logger.warn(e) { "Resource already exists: type=${e.resourceType}, id=${e.identifier}, clue=${e.clue}" }
                    ErrorCode.RESOURCE_ALREADY_EXISTS to HttpStatus.CONFLICT
                }
                is ResourceException.ResourceConflict -> {
                    logger.warn(e) { "Resource conflict: type=${e.resourceType}, reason=${e.reason}, clue=${e.clue}" }
                    ErrorCode.RESOURCE_CONFLICT to HttpStatus.CONFLICT
                }
            }

        return ResponseEntity(
            ErrorResponse(
                code = errorCode,
                message = e.message ?: "리소스 처리 중 오류가 발생하였습니다.",
            ),
            httpStatus,
        )
    }
}
