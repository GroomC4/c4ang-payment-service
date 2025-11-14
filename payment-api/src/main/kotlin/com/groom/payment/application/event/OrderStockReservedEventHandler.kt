package com.groom.payment.application.event

import com.groom.payment.domain.port.OrderPort
import com.groom.payment.domain.port.SavePaymentGatewayLogPort
import com.groom.payment.domain.port.SavePaymentPort
import org.springframework.stereotype.Component

/**
 * 재고 예약 완료 → Payment 자동 생성 이벤트 핸들러
 *
 * StockReservedEvent 발생 시:
 * 1. 주문당 결제 중복 검사 (비즈니스 규칙)
 * 2. Payment 엔티티 생성 (PAYMENT_WAIT 상태)
 * 3. Order와 Payment 연결 (Order.markPaymentPending)
 * 4. PaymentGatewayLog 초기 이력 기록
 *
 * 사용자가 결제 수단을 선택하고 RequestPaymentService를 호출하면
 * PG사로 결제 요청이 전송되고 Payment 상태가 PAYMENT_REQUEST로 변경됩니다.
 *
 * 패키지 위치:
 * - payment 패키지: Payment 엔티티를 생성하는 책임이 있으므로
 * - OrderPort를 통한 간접 의존 (Hexagonal Architecture)
 */
@Component
class OrderStockReservedEventHandler(
    private val orderPort: OrderPort,
    private val savePaymentPort: SavePaymentPort,
    private val savePaymentGatewayLogPort: SavePaymentGatewayLogPort,
) {
    fun handleStockReserved(event: Any) {
        TODO("Order 서비스 이벤트 연동 필요")
    }
}
