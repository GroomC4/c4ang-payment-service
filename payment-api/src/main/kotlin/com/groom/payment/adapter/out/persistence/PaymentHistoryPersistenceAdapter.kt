package com.groom.payment.adapter.out.persistence

import com.groom.payment.domain.model.PaymentHistory
import com.groom.payment.domain.port.LoadPaymentHistoryPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * PaymentHistory Persistence Adapter (Hexagonal Architecture)
 *
 * Port 인터페이스를 구현하여 JPA를 통한 영속성을 제공합니다.
 */
@Component
class PaymentHistoryPersistenceAdapter(
    private val paymentHistoryJpaRepository: PaymentHistoryJpaRepository,
) : LoadPaymentHistoryPort {
    override fun loadByPaymentId(paymentId: UUID): List<PaymentHistory> = paymentHistoryJpaRepository.findByPaymentId(paymentId)

    override fun loadByEventType(eventType: String): List<PaymentHistory> = paymentHistoryJpaRepository.findByEventType(eventType)
}
