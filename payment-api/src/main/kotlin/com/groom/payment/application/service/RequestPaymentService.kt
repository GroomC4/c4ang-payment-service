package com.groom.payment.application.service

import com.groom.payment.application.dto.RequestPaymentCommand
import com.groom.payment.application.dto.RequestPaymentResult
import com.groom.payment.domain.port.LoadPaymentPort
import com.groom.payment.domain.port.PaymentGatewayPort
import com.groom.payment.domain.service.PaymentEventFactory
import com.groom.payment.domain.service.PaymentLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 요청 Application 서비스
 *
 * 사용자가 결제 수단을 선택하고 PG사로 결제 요청을 전송합니다.
 *
 * 플로우:
 * 1. Payment 조회
 * 2. 결제 수단 업데이트 (도메인 메서드가 상태 검증)
 * 3. PG사로 결제 요청
 * 4. Payment 상태를 PAYMENT_REQUEST로 변경 (도메인 메서드가 상태 검증)
 * 5. PaymentRequestedEvent 발행
 *
 * 동시성 제어:
 * - PaymentLockManager: 분산 락 관리
 *
 * 트랜잭션:
 * - @Transactional: JPA dirty checking으로 자동 저장
 */
@Service
class RequestPaymentService(
    private val loadPaymentPort: LoadPaymentPort,
    private val paymentGatewayPort: PaymentGatewayPort,
    private val paymentLockManager: PaymentLockManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentEventFactory: PaymentEventFactory,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: RequestPaymentCommand): RequestPaymentResult =
        paymentLockManager.executeWithLock(command.paymentId) {
            // 1. Payment 조회
            val payment =
                loadPaymentPort.loadById(command.paymentId)
                    ?: throw IllegalArgumentException("Payment not found: ${command.paymentId}")

            // 2. PG사로 결제 요청
            val pgResult =
                paymentGatewayPort.requestPayment(
                    paymentId = payment.id,
                    amount = command.paymentAmount,
                    orderNumber = "ORDER-${payment.orderId}",
                )

            logger.info {
                "PG 결제 요청 완료: paymentId=${payment.id}, " +
                    "pgTransactionId=${pgResult.pgTransactionId}, " +
                    "pgUrl=${pgResult.paymentUrl}"
            }

            // 3. Payment 상태 변경 및 금액 정보 설정 (PAYMENT_WAIT → PAYMENT_REQUEST)
            // JPA dirty checking으로 자동 저장
            payment.requestPayment(
                pgTransactionId = pgResult.pgTransactionId,
                totalAmount = command.totalAmount,
                paymentAmount = command.paymentAmount,
                discountAmount = command.discountAmount,
                deliveryFee = command.deliveryFee,
                method = command.paymentMethod,
            )

            logger.info {
                "Payment requested: paymentId=${payment.id}, orderId=${payment.orderId}, " +
                    "method=${command.paymentMethod}, totalAmount=${command.totalAmount}, " +
                    "paymentAmount=${command.paymentAmount}, pgTransactionId=${pgResult.pgTransactionId}"
            }

            // 4. 도메인 이벤트 생성 및 발행
            val event = paymentEventFactory.createPaymentRequestedEvent(payment)
            eventPublisher.publishEvent(event)

            RequestPaymentResult(
                paymentId = payment.id,
                orderId = payment.orderId,
                status = payment.status,
                pgTransactionId = pgResult.pgTransactionId,
                pgUrl = pgResult.paymentUrl,
                occurredAt = event.occurredAt,
            )
        }
}
