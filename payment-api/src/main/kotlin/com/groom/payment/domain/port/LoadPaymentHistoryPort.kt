package com.groom.payment.domain.port

import com.groom.payment.domain.model.PaymentHistory
import java.util.UUID

/**
 * PaymentHistory 조회 Port (Hexagonal Architecture)
 *
 * Domain이 외부 영속성 계층에 요구하는 계약.
 *
 * 구현체:
 * - PaymentHistoryPersistenceAdapter: JPA 기반 구현
 *
 * 사용처:
 * - GetPaymentService: 결제 이력 조회
 */
interface LoadPaymentHistoryPort {
    /**
     * Payment ID로 이력 조회
     *
     * @param paymentId Payment ID
     * @return PaymentHistory 목록
     */
    fun loadByPaymentId(paymentId: UUID): List<PaymentHistory>

    /**
     * 이벤트 타입으로 이력 조회
     *
     * @param eventType 이벤트 타입
     * @return PaymentHistory 목록
     */
    fun loadByEventType(eventType: String): List<PaymentHistory>
}
