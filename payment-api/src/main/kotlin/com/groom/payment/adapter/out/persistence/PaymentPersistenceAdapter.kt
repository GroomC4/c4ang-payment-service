package com.groom.payment.adapter.out.persistence

import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.SavePaymentPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Payment Persistence Adapter (Hexagonal Architecture)
 *
 * Port 인터페이스를 구현하여 JPA를 통한 영속성을 제공합니다.
 *
 * 역할:
 * - LoadPaymentPort, SavePaymentPort 구현
 * - JpaRepository 호출 및 결과 변환
 * - Domain은 이 Adapter의 존재를 알지 못함 (의존성 역전)
 *
 * 주의:
 * - JPA Repository는 internal로 선언하여 Adapter 외부에서 접근 불가
 * - Domain Model을 직접 반환 (별도 Entity 변환 불필요)
 */
@Component
class PaymentPersistenceAdapter(
    private val paymentJpaRepository: PaymentJpaRepository,
) : LoadPaymentPort,
    SavePaymentPort {
    override fun loadById(id: UUID): Payment? = paymentJpaRepository.findById(id).orElse(null)

    override fun loadByOrderId(orderId: UUID): Payment? = paymentJpaRepository.findByOrderId(orderId)

    override fun loadByStatus(status: PaymentStatus): List<Payment> = paymentJpaRepository.findByStatus(status)

    override fun loadByMethod(method: PaymentMethod): List<Payment> = paymentJpaRepository.findByMethod(method)

    override fun loadByOrderUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<Payment> = paymentJpaRepository.findByOrderUserId(userId, pageable)

    override fun loadByOrderUserIdAndStatus(
        userId: UUID,
        status: PaymentStatus,
        pageable: Pageable,
    ): Page<Payment> = paymentJpaRepository.findByOrderUserIdAndStatus(userId, status, pageable)

    override fun save(payment: Payment): Payment = paymentJpaRepository.save(payment)
}
