package com.groom.payment.domain.port

import com.groom.payment.domain.model.PaymentGatewayLog

/**
 * PaymentGatewayLog 저장 Port (Hexagonal Architecture)
 *
 * Domain이 외부 영속성 계층에 요구하는 계약.
 *
 * 구현체:
 * - PaymentGatewayLogPersistenceAdapter: JPA 기반 구현
 *
 * 사용처:
 * - PG 연동 어댑터
 */
interface SavePaymentGatewayLogPort {
    /**
     * PaymentGatewayLog 저장
     *
     * @param log 저장할 PaymentGatewayLog
     * @return 저장된 PaymentGatewayLog
     */
    fun save(log: PaymentGatewayLog): PaymentGatewayLog
}
