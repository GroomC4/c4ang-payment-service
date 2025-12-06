package com.groom.payment.adapter.outbound.client

import com.groom.payment.adapter.outbound.client.dto.OrderServiceMarkPaymentPendingRequest
import com.groom.payment.application.dto.OrderInfo
import com.groom.payment.common.exception.OrderServiceException
import com.groom.payment.configuration.feign.FeignClientException
import com.groom.payment.domain.port.OrderPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * OrderPort 구현체 (Adapter)
 *
 * Order Service의 Internal API를 호출하여 주문 정보를 조회하고 상태를 변경합니다.
 * Consumer Driven Contract를 통해 API 스펙이 검증됩니다.
 *
 * @see OrderServiceClient
 */
@Component
class OrderAdapter(
    private val orderServiceClient: OrderServiceClient,
) : OrderPort {
    /**
     * 주문 정보 조회
     *
     * @param orderId 주문 ID
     * @return 주문 정보, 없으면 null
     */
    override fun findById(orderId: UUID): OrderInfo? =
        try {
            logger.debug { "Fetching order from Order Service: orderId=$orderId" }
            val response = orderServiceClient.getOrder(orderId)
            logger.debug { "Order fetched successfully: orderId=$orderId, orderNumber=${response.orderNumber}" }
            response.toOrderInfo()
        } catch (e: FeignClientException.NotFound) {
            logger.warn { "Order not found: orderId=$orderId" }
            null
        } catch (e: FeignClientException) {
            logger.error(e) { "Failed to fetch order from Order Service: orderId=$orderId" }
            throw OrderServiceException.ServiceCallFailed("findById(orderId=$orderId)", e)
        }

    /**
     * 주문을 결제 대기 상태로 변경
     *
     * @param orderId 주문 ID
     * @param paymentId 결제 ID
     * @throws OrderServiceException.OrderNotFound 주문을 찾을 수 없는 경우
     * @throws OrderServiceException.PaymentAlreadyExists 이미 결제가 연결된 경우
     * @throws OrderServiceException.InvalidOrderStatus 주문 상태가 결제 대기로 변경할 수 없는 경우
     */
    override fun markOrderPaymentPending(
        orderId: UUID,
        paymentId: UUID,
    ) {
        try {
            logger.info { "Marking order as payment pending: orderId=$orderId, paymentId=$paymentId" }
            val request = OrderServiceMarkPaymentPendingRequest(paymentId = paymentId)
            val response = orderServiceClient.markPaymentPending(orderId, request)
            logger.info {
                "Order marked as payment pending successfully: orderId=$orderId, status=${response.status}"
            }
        } catch (e: FeignClientException.NotFound) {
            logger.warn { "Order not found for payment pending: orderId=$orderId" }
            throw OrderServiceException.OrderNotFound(orderId)
        } catch (e: FeignClientException.Conflict) {
            logger.warn { "Conflict while marking order as payment pending: orderId=$orderId, message=${e.message}" }
            // Conflict는 ORDER_STATUS_INVALID 또는 PAYMENT_ALREADY_EXISTS일 수 있음
            throw OrderServiceException.InvalidOrderStatus(
                orderId = orderId,
                reason = e.message ?: "Unknown conflict",
            )
        } catch (e: FeignClientException) {
            logger.error(e) { "Failed to mark order as payment pending: orderId=$orderId" }
            throw OrderServiceException.ServiceCallFailed(
                "markOrderPaymentPending(orderId=$orderId, paymentId=$paymentId)",
                e,
            )
        }
    }

    /**
     * 주문에 이미 결제가 연결되어 있는지 확인
     *
     * @param orderId 주문 ID
     * @return 결제가 연결되어 있으면 true
     * @throws OrderServiceException.OrderNotFound 주문을 찾을 수 없는 경우
     */
    override fun hasPayment(orderId: UUID): Boolean =
        try {
            logger.debug { "Checking if order has payment: orderId=$orderId" }
            val response = orderServiceClient.hasPayment(orderId)
            logger.debug { "Order payment check result: orderId=$orderId, hasPayment=${response.hasPayment}" }
            response.hasPayment
        } catch (e: FeignClientException.NotFound) {
            logger.warn { "Order not found for payment check: orderId=$orderId" }
            throw OrderServiceException.OrderNotFound(orderId)
        } catch (e: FeignClientException) {
            logger.error(e) { "Failed to check order payment: orderId=$orderId" }
            throw OrderServiceException.ServiceCallFailed("hasPayment(orderId=$orderId)", e)
        }

    // 재고 예약 확정은 이벤트 기반으로 처리됩니다.
    // Payment Service → payment.completed 발행 → Product Service가 재고 확정
    // 참고: docs/INTEGRATION-v2.md
}
