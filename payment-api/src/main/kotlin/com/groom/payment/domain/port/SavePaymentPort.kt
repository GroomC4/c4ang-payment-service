package com.groom.payment.domain.port

import com.groom.payment.domain.model.Payment

/**
 * Payment 저장 Port (Hexagonal Architecture)
 *
 * Domain이 외부 영속성 계층에 요구하는 계약.
 *
 * 구현체:
 * - PaymentPersistenceAdapter: JPA 기반 구현
 *
 * 사용처:
 * - Application Service: Payment 저장
 */
interface SavePaymentPort {
    /**
     * Payment 저장
     *
     * @param payment 저장할 Payment
     * @return 저장된 Payment
     */
    fun save(payment: Payment): Payment
}
