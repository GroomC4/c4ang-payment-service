package com.groom.payment.adapter.out.persistence

import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.model.PaymentMethod
import com.groom.payment.domain.model.PaymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/**
 * Payment JPA Repository (Adapter 내부)
 *
 * Hexagonal Architecture에서 이 Repository는 Adapter 내부에만 위치합니다.
 * Domain/Application은 Port 인터페이스를 통해서만 접근합니다.
 */
interface PaymentJpaRepository : JpaRepository<Payment, UUID> {
    /**
     * 주문 ID로 Payment 조회
     */
    fun findByOrderId(orderId: UUID): Payment?

    /**
     * 상태별 Payment 목록 조회
     */
    fun findByStatus(status: PaymentStatus): List<Payment>

    /**
     * 결제 수단별 Payment 목록 조회
     */
    fun findByMethod(method: PaymentMethod): List<Payment>

    /**
     * 사용자별 결제 목록 조회 (페이지네이션)
     *
     * Order와 조인하여 userId로 필터링합니다.
     */
    @Query(
        """
        SELECT p FROM Payment p
        JOIN Order o ON p.orderId = o.id
        WHERE o.userExternalId = :userId
        ORDER BY p.createdAt DESC
    """,
    )
    fun findByOrderUserId(
        @Param("userId") userId: UUID,
        pageable: Pageable,
    ): Page<Payment>

    /**
     * 사용자별 + 상태별 결제 목록 조회 (페이지네이션)
     */
    @Query(
        """
        SELECT p FROM Payment p
        JOIN Order o ON p.orderId = o.id
        WHERE o.userExternalId = :userId AND p.status = :status
        ORDER BY p.createdAt DESC
    """,
    )
    fun findByOrderUserIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("status") status: PaymentStatus,
        pageable: Pageable,
    ): Page<Payment>
}
