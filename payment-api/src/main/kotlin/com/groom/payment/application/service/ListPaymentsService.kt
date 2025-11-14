package com.groom.payment.application.service

import com.groom.payment.application.dto.ListPaymentsQuery
import com.groom.payment.application.dto.ListPaymentsResult
import com.groom.payment.application.dto.PaginationInfo
import com.groom.payment.application.dto.PaymentSummary
import com.groom.payment.domain.port.LoadPaymentPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 목록 조회 Application 서비스
 *
 * 사용자별 결제 목록을 조회합니다 (상태 필터링, 페이지네이션).
 */
@Service
class ListPaymentsService(
    private val loadPaymentPort: LoadPaymentPort,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun execute(query: ListPaymentsQuery): ListPaymentsResult {
        logger.debug { "Listing payments: userId=${query.userId}, status=${query.status}, page=${query.page}" }

        // 1. Pageable 생성
        val pageable =
            PageRequest.of(
                query.page - 1, // 0-based index
                query.limit,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

        // 2. 상태 필터링에 따라 조회
        val paymentsPage =
            if (query.status != null) {
                loadPaymentPort.loadByOrderUserIdAndStatus(
                    userId = query.userId,
                    status = query.status,
                    pageable = pageable,
                )
            } else {
                loadPaymentPort.loadByOrderUserId(
                    userId = query.userId,
                    pageable = pageable,
                )
            }

        logger.debug { "Found ${paymentsPage.totalElements} payments for user ${query.userId}" }

        // 3. DTO 변환
        return ListPaymentsResult(
            payments =
                paymentsPage.content.map { payment ->
                    PaymentSummary(
                        paymentId = payment.id,
                        orderId = payment.orderId,
                        totalAmount = payment.totalAmount,
                        paymentAmount = payment.paymentAmount,
                        method = payment.method,
                        status = payment.status,
                        completedAt = payment.completedAt,
                        createdAt = payment.createdAt,
                    )
                },
            pagination =
                PaginationInfo(
                    page = query.page,
                    limit = query.limit,
                    total = paymentsPage.totalElements,
                ),
        )
    }
}
