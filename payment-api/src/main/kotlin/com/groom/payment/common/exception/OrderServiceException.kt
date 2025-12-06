package com.groom.payment.common.exception

import java.util.UUID

/**
 * Order Service 연동 관련 예외
 *
 * Payment Service에서 Order Service API 호출 시 발생하는 예외입니다.
 */
sealed class OrderServiceException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    /**
     * 주문을 찾을 수 없는 경우
     *
     * @param orderId 찾을 수 없는 주문 ID
     */
    data class OrderNotFound(
        val orderId: UUID,
    ) : OrderServiceException("주문을 찾을 수 없습니다: $orderId")

    /**
     * 주문에 이미 결제가 연결되어 있는 경우
     *
     * @param orderId 주문 ID
     */
    data class PaymentAlreadyExists(
        val orderId: UUID,
    ) : OrderServiceException("주문에 이미 결제가 연결되어 있습니다: $orderId")

    /**
     * 주문 상태가 결제를 진행할 수 없는 상태인 경우
     *
     * @param orderId 주문 ID
     * @param reason 상태 전이 불가 사유
     */
    data class InvalidOrderStatus(
        val orderId: UUID,
        val reason: String,
    ) : OrderServiceException("결제를 진행할 수 없는 주문 상태입니다: orderId=$orderId, reason=$reason")

    /**
     * Order Service 호출 중 알 수 없는 에러 발생
     *
     * @param operation 수행 중이던 작업
     * @param cause 원인 예외
     */
    data class ServiceCallFailed(
        val operation: String,
        override val cause: Throwable,
    ) : OrderServiceException("Order Service 호출 실패: $operation", cause)
}
