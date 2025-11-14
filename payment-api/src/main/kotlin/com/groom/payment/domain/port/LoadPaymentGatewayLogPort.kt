package com.groom.payment.domain.port

import com.groom.payment.domain.model.PaymentGatewayLog
import java.util.UUID

/**
 * PaymentGatewayLog 조회 Port (Hexagonal Architecture)
 *
 * Domain이 외부 영속성 계층에 요구하는 계약.
 *
 * 구현체:
 * - PaymentGatewayLogPersistenceAdapter: JPA 기반 구현
 *
 * 사용처:
 * - PG 연동 관리 서비스
 */
interface LoadPaymentGatewayLogPort {
    /**
     * Payment ID로 PG 로그 조회
     *
     * @param paymentId Payment ID
     * @return PaymentGatewayLog 목록
     */
    fun loadByPaymentId(paymentId: UUID): List<PaymentGatewayLog>

    /**
     * PG 코드로 로그 조회
     *
     * @param pgCode PG 코드
     * @return PaymentGatewayLog 목록
     */
    fun loadByPgCode(pgCode: String): List<PaymentGatewayLog>

    /**
     * 상태로 로그 조회
     *
     * @param status 상태
     * @return PaymentGatewayLog 목록
     */
    fun loadByStatus(status: String): List<PaymentGatewayLog>
}
