package com.groom.payment.application.service

import com.groom.payment.application.dto.CreatePaymentWaitCommand
import com.groom.payment.application.dto.CreatePaymentWaitResult
import com.groom.payment.domain.model.Payment
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.SavePaymentPort
import com.groom.platform.saga.SagaSteps
import com.groom.platform.saga.SagaTrackerClient
import com.groom.platform.saga.SagaType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 대기 생성 Application 서비스
 *
 * OrderConfirmed 이벤트를 수신하여 Payment 엔티티를 PAYMENT_WAIT 상태로 생성합니다.
 *
 * SAGA 플로우:
 * 1. Order Service: 주문 생성 → order.created 발행
 * 2. Product Service: 재고 예약 → stock.reserved 발행
 * 3. Order Service: 주문 확정 → order.confirmed 발행
 * 4. Payment Service: 결제 대기 생성 (이 서비스)
 *
 * 멱등성:
 * - eventId 또는 orderId 기반으로 중복 생성 방지
 * - 이미 존재하는 Payment가 있으면 기존 Payment 반환
 */
@Service
class CreatePaymentWaitService(
    private val loadPaymentPort: LoadPaymentPort,
    private val savePaymentPort: SavePaymentPort,
    private val sagaTrackerClient: SagaTrackerClient,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 결제 대기 생성
     *
     * @param command 결제 대기 생성 커맨드
     * @return 생성된 결제 대기 결과
     */
    @Transactional
    fun execute(command: CreatePaymentWaitCommand): CreatePaymentWaitResult {
        logger.info {
            "결제 대기 생성 시작: orderId=${command.orderId}, " +
                "userId=${command.userId}, totalAmount=${command.totalAmount}"
        }

        // 멱등성: 이미 해당 주문에 대한 Payment가 존재하는지 확인
        val existingPayment = loadPaymentPort.loadByOrderId(command.orderId)
        if (existingPayment != null) {
            logger.info {
                "이미 존재하는 Payment 반환: paymentId=${existingPayment.id}, " +
                    "orderId=${command.orderId}, status=${existingPayment.status}"
            }
            return CreatePaymentWaitResult(
                paymentId = existingPayment.id,
                orderId = existingPayment.orderId,
                userId = existingPayment.userId,
                status = existingPayment.status,
                createdAt = existingPayment.createdAt ?: existingPayment.requestedAt,
            )
        }

        // Payment 엔티티 생성 (PAYMENT_WAIT 상태)
        val payment = Payment(
            orderId = command.orderId,
            userId = command.userId,
            totalAmount = command.totalAmount,
        )

        // Payment 저장
        val savedPayment = savePaymentPort.save(payment)

        logger.info {
            "결제 대기 생성 완료: paymentId=${savedPayment.id}, " +
                "orderId=${savedPayment.orderId}, status=${savedPayment.status}"
        }

        // Saga Tracker 기록: PAYMENT_INITIALIZATION
        sagaTrackerClient.recordProgress(
            sagaId = savedPayment.id.toString(),
            sagaType = SagaType.ORDER_CREATION,
            step = SagaSteps.PAYMENT_INITIALIZATION,
            orderId = savedPayment.orderId.toString(),
            metadata = mapOf<String, Any>(
                "userId" to savedPayment.userId.toString(),
                "totalAmount" to (savedPayment.totalAmount?.toString() ?: "0"),
                "status" to savedPayment.status.name,
            ),
        )

        return CreatePaymentWaitResult(
            paymentId = savedPayment.id,
            orderId = savedPayment.orderId,
            userId = savedPayment.userId,
            status = savedPayment.status,
            createdAt = savedPayment.createdAt ?: savedPayment.requestedAt,
        )
    }
}
