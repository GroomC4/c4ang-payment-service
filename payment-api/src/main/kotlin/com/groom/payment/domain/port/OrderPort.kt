package com.groom.payment.domain.port

import com.groom.payment.application.dto.OrderInfo
import java.util.UUID

/**
 * Order 도메인 연동 Port (Hexagonal Architecture)
 *
 * Payment 도메인에서 Order 도메인과 상호작용할 때 사용합니다.
 * 모놀리식: OrderRepository + StockReservationService 사용
 * MSA: HTTP Client로 교체 가능
 */
interface OrderPort {
    /**
     * Order 조회 (상품 정보 포함)
     *
     * @param orderId Order ID
     * @return Order 정보 (상품 포함)
     */
    fun findById(orderId: UUID): OrderInfo?

    /**
     * 주문을 결제 대기 상태로 변경
     *
     * Payment 생성 시점에 Order와 Payment를 연결합니다.
     * STOCK_RESERVED → PAYMENT_PENDING
     *
     * @param orderId Order ID
     * @param paymentId Payment ID
     * @throws IllegalStateException Order를 찾을 수 없거나 상태 전이가 불가능한 경우
     */
    fun markOrderPaymentPending(
        orderId: UUID,
        paymentId: UUID,
    )

    /**
     * 주문에 이미 결제 정보가 있는지 확인
     *
     * 비즈니스 규칙: 주문당 결제는 1개만 가능
     *
     * @param orderId Order ID
     * @return 이미 결제 정보가 있으면 true
     */
    fun hasPayment(orderId: UUID): Boolean

    /**
     * 재고 예약 확정
     *
     * 결제 완료 시 Redis에 예약된 재고를 DB로 확정합니다.
     *
     * @param orderId Order ID
     * @throws IllegalStateException 재고 예약을 찾을 수 없거나 확정에 실패한 경우
     */
    fun confirmStockReservation(orderId: UUID)
}
