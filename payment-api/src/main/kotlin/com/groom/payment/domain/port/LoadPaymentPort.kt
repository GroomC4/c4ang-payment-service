package com.groom.payment.domain.port

import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

/**
 * Payment 조회 Port (Hexagonal Architecture)
 *
 * Domain이 외부 영속성 계층에 요구하는 계약.
 *
 * 구현체:
 * - PaymentPersistenceAdapter: JPA 기반 구현
 *
 * 사용처:
 * - Application Service: Payment 조회
 */
interface LoadPaymentPort {
    /**
     * ID로 Payment 조회
     *
     * @param id Payment ID
     * @return Payment 또는 null (존재하지 않을 경우)
     */
    fun loadById(id: UUID): Payment?

    /**
     * 주문 ID로 Payment 조회
     *
     * @param orderId Order ID
     * @return Payment 또는 null (존재하지 않을 경우)
     */
    fun loadByOrderId(orderId: UUID): Payment?

    /**
     * 상태별 Payment 목록 조회
     *
     * @param status 결제 상태
     * @return Payment 목록
     */
    fun loadByStatus(status: PaymentStatus): List<Payment>

    /**
     * 결제 수단별 Payment 목록 조회
     *
     * @param method 결제 수단
     * @return Payment 목록
     */
    fun loadByMethod(method: PaymentMethod): List<Payment>

    /**
     * 사용자별 결제 목록 조회 (페이지네이션)
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return Payment 페이지
     */
    fun loadByOrderUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<Payment>

    /**
     * 사용자별 + 상태별 결제 목록 조회 (페이지네이션)
     *
     * @param userId 사용자 ID
     * @param status 결제 상태
     * @param pageable 페이지 정보
     * @return Payment 페이지
     */
    fun loadByOrderUserIdAndStatus(
        userId: UUID,
        status: PaymentStatus,
        pageable: Pageable,
    ): Page<Payment>
}
