package com.groom.payment.adapter.outbound.client

import com.groom.payment.adapter.outbound.client.dto.OrderServiceGetOrderResponse
import com.groom.payment.adapter.outbound.client.dto.OrderServiceHasPaymentResponse
import com.groom.payment.adapter.outbound.client.dto.OrderServiceMarkPaymentPendingRequest
import com.groom.payment.adapter.outbound.client.dto.OrderServiceMarkPaymentPendingResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

/**
 * Order Service Feign Client
 *
 * Order Service의 Internal API를 호출합니다.
 * Consumer Driven Contract를 통해 검증됩니다.
 */
@FeignClient(
    name = "order-service",
    url = "\${feign.clients.order-service.url}",
)
interface OrderServiceClient {
    /**
     * 주문 정보 조회
     *
     * @param orderId 주문 ID
     * @return 주문 정보
     */
    @GetMapping("/internal/v1/orders/{orderId}")
    fun getOrder(
        @PathVariable orderId: UUID,
    ): OrderServiceGetOrderResponse

    /**
     * 주문에 결제가 연결되어 있는지 확인
     *
     * @param orderId 주문 ID
     * @return 결제 존재 여부
     */
    @GetMapping("/internal/v1/orders/{orderId}/has-payment")
    fun hasPayment(
        @PathVariable orderId: UUID,
    ): OrderServiceHasPaymentResponse

    /**
     * 주문을 결제 대기 상태로 변경
     *
     * @param orderId 주문 ID
     * @param request 결제 대기 요청
     * @return 결제 대기 응답
     */
    @PostMapping("/internal/v1/orders/{orderId}/payment-pending")
    fun markPaymentPending(
        @PathVariable orderId: UUID,
        @RequestBody request: OrderServiceMarkPaymentPendingRequest,
    ): OrderServiceMarkPaymentPendingResponse
}
