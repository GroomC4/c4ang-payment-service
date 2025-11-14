package com.groom.payment.application.service

import com.groom.payment.application.dto.GetPaymentQuery
import com.groom.payment.application.dto.GetPaymentResult
import com.groom.payment.application.dto.PaymentHistoryItem
import com.groom.payment.domain.port.LoadPaymentHistoryPort
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.OrderPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 상세 조회 Application 서비스
 *
 * Payment 정보를 조회합니다 (이력 포함 가능).
 */
@Service
class GetPaymentService(
    private val loadPaymentPort: LoadPaymentPort,
    private val loadPaymentHistoryPort: LoadPaymentHistoryPort,
    private val orderPort: OrderPort,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun execute(query: GetPaymentQuery): GetPaymentResult {
        logger.debug { "Getting payment: paymentId=${query.paymentId}" }

        // 1. Payment 조회
        val payment =
            loadPaymentPort.loadById(query.paymentId)
                ?: throw IllegalArgumentException("Payment not found: ${query.paymentId}")

        // 2. History 조회 (옵션)
        val history =
            if (query.includeHistory) {
                loadPaymentHistoryPort
                    .loadByPaymentId(payment.id)
                    .map { h ->
                        PaymentHistoryItem(
                            eventType = h.eventType,
                            changeSummary = h.changeSummary,
                            recordedAt = h.recordedAt,
                        )
                    }
            } else {
                emptyList()
            }

        // 3. Order 정보 조회
        val orderInfo = orderPort.findById(payment.orderId)

        logger.debug { "Payment retrieved: paymentId=${payment.id}, historyCount=${history.size}" }

        return GetPaymentResult(
            paymentId = payment.id,
            orderId = payment.orderId,
            totalAmount = payment.totalAmount,
            paymentAmount = payment.paymentAmount,
            discountAmount = payment.discountAmount,
            deliveryFee = payment.deliveryFee,
            method = payment.method,
            status = payment.status,
            pgTransactionId = payment.pgTransactionId,
            pgApprovalNumber = payment.pgApprovalNumber,
            completedAt = payment.completedAt,
            createdAt = payment.createdAt,
            updatedAt = payment.updatedAt,
            orderInfo = orderInfo,
            history = history,
        )
    }
}
