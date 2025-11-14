package com.groom.payment.infrastructure.adapter

import com.groom.payment.application.dto.OrderInfo
import com.groom.payment.domain.port.OrderPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * OrderPort 구현체 (Adapter)
 *
 * MSA 전환 시 HTTP Client로 구현됩니다.
 */
@Component
class OrderAdapter : OrderPort {
    override fun findById(orderId: UUID): OrderInfo? {
        TODO("Order 서비스 연동 필요")
    }

    override fun markOrderPaymentPending(
        orderId: UUID,
        paymentId: UUID,
    ) {
        TODO("Order 서비스 연동 필요")
    }

    override fun hasPayment(orderId: UUID): Boolean {
        TODO("Order 서비스 연동 필요")
    }

    override fun confirmStockReservation(orderId: UUID) {
        TODO("Order 서비스 연동 필요")
    }
}
