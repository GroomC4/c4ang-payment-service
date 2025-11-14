package com.groom.payment.presentation.web

import com.groom.payment.application.dto.CancelPaymentCommand
import com.groom.payment.application.dto.RequestPaymentCommand
import com.groom.payment.application.dto.RequestPaymentRefundCommand
import com.groom.payment.application.service.CancelPaymentService
import com.groom.payment.application.service.RequestPaymentRefundService
import com.groom.payment.application.service.RequestPaymentService
import com.groom.payment.presentation.web.dto.CancelPaymentRequest
import com.groom.payment.presentation.web.dto.CancelPaymentResponse
import com.groom.payment.presentation.web.dto.RequestPaymentRefundRequest
import com.groom.payment.presentation.web.dto.RequestPaymentRefundResponse
import com.groom.payment.presentation.web.dto.RequestPaymentRequest
import com.groom.payment.presentation.web.dto.RequestPaymentResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Payment 명령(Command) REST API Controller
 *
 * 비동기 주문-결제 시스템의 Payment 상태 변경 API 엔드포인트
 *
 * API 분류:
 * - 내부 API: Order 서비스 또는 시스템 내부에서 호출
 * - 사용자 API: 고객이 직접 호출
 *
 * 참고: PG 콜백 API는 ExternalPaymentController에서 처리
 */
@Tag(name = "Payment Command", description = "결제 명령 API")
@RestController
@RequestMapping("/api/v1/payments")
class PaymentCommandController(
    private val requestPaymentService: RequestPaymentService,
    private val cancelPaymentService: CancelPaymentService,
    private val requestPaymentRefundService: RequestPaymentRefundService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 결제 요청
     *
     * POST /api/v1/payments/request
     *
     * 사용자 API: 사용자가 결제 수단을 선택하고 결제를 시작
     * - 재고 예약 완료 후 호출
     * - Payment 상태 변경: PAYMENT_WAIT → PAYMENT_REQUEST
     * - PG사로 결제 요청 전송
     *
     * @param request 결제 요청 정보 (paymentId, paymentMethod)
     * @return 결제 요청 결과 (status: PAYMENT_REQUEST, pgUrl)
     */
    @Operation(
        summary = "결제 요청",
        description = "사용자가 결제 수단을 선택하고 결제를 시작합니다. 재고 예약 완료 후 호출됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "결제 요청 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음"),
        ],
    )
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestPayment(
        @Valid @RequestBody request: RequestPaymentRequest,
    ): RequestPaymentResponse {
        logger.info {
            "Payment request received: paymentId=${request.paymentId}, method=${request.paymentMethod}"
        }

        val command =
            RequestPaymentCommand(
                paymentId = request.paymentId,
                paymentMethod = request.paymentMethod,
                totalAmount = request.totalAmount,
                paymentAmount = request.paymentAmount,
                discountAmount = request.discountAmount,
                deliveryFee = request.deliveryFee,
            )

        val result = requestPaymentService.execute(command)

        logger.info {
            "Payment request processed: paymentId=${result.paymentId}, status=${result.status}, " +
                "pgTransactionId=${result.pgTransactionId}"
        }

        return RequestPaymentResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            status = result.status.name,
            pgTransactionId = result.pgTransactionId,
            pgUrl = result.pgUrl,
            requestedAt = result.occurredAt,
        )
    }

    /**
     * 결제 취소
     *
     * POST /api/v1/payments/{paymentId}/cancel
     *
     * 내부 API: 사용자 취소 요청 또는 시스템 타임아웃 시 호출
     * - 취소 가능 상태: PAYMENT_WAIT, PAYMENT_REQUEST
     * - 재고 예약 복구
     * - 주문 상태 변경 (ORDER_CANCELLED)
     *
     * @param paymentId 결제 ID
     * @param request 취소 사유
     * @return 취소 결과 (previousStatus, currentStatus: PAYMENT_CANCELLED)
     */
    @Operation(
        summary = "결제 취소",
        description = "결제를 취소합니다. 재고 예약이 복구되고 주문 상태가 변경됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "결제 취소 성공"),
            ApiResponse(responseCode = "400", description = "취소 불가능한 상태"),
            ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음"),
        ],
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{paymentId}/cancel")
    fun cancelPayment(
        @Parameter(description = "결제 ID", required = true)
        @PathVariable
        paymentId: UUID,
        @Valid @RequestBody request: CancelPaymentRequest,
    ): CancelPaymentResponse {
        logger.info { "Payment cancellation requested: paymentId=$paymentId, reason=${request.reason}" }

        val command =
            CancelPaymentCommand(
                paymentId = paymentId,
                reason = request.reason,
            )

        val result = cancelPaymentService.execute(command)

        logger.info {
            "Payment cancelled: paymentId=${result.paymentId}, " +
                "previousStatus=${result.previousStatus}, currentStatus=${result.currentStatus}"
        }

        return CancelPaymentResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            previousStatus = result.previousStatus,
            currentStatus = result.currentStatus,
            reason = result.reason,
            cancelledAt = result.cancelledAt,
        )
    }

    /**
     * 환불 요청
     *
     * POST /api/v1/payments/{paymentId}/refund/request
     *
     * 사용자 API: 고객이 상품 반품 후 환불 요청 시 호출
     * - 환불 가능 상태: PAYMENT_COMPLETED
     * - PG사 환불 API 호출
     * - 주문 상태 변경 (REFUND_REQUESTED → REFUND_COMPLETED)
     *
     * @param paymentId 결제 ID
     * @param request 환불 요청 정보 (refundAmount, reason)
     * @return 환불 요청 결과 (status: REFUND_REQUESTED)
     */
    @Operation(
        summary = "환불 요청",
        description = "결제 완료된 주문에 대해 환불을 요청합니다. PG사 환불 API가 호출됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "202", description = "환불 요청 접수 성공"),
            ApiResponse(responseCode = "400", description = "환불 불가능한 상태"),
            ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음"),
        ],
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{paymentId}/refund/request")
    fun requestRefund(
        @Parameter(description = "결제 ID", required = true)
        @PathVariable
        paymentId: UUID,
        @Valid @RequestBody request: RequestPaymentRefundRequest,
    ): RequestPaymentRefundResponse {
        logger.info {
            "Refund requested: paymentId=$paymentId, amount=${request.refundAmount}, reason=${request.reason}"
        }

        val command =
            RequestPaymentRefundCommand(
                paymentId = paymentId,
                refundAmount = request.refundAmount,
                reason = request.reason,
            )

        val result = requestPaymentRefundService.execute(command)

        logger.info { "Refund request processed: paymentId=${result.paymentId}, status=${result.status}" }

        return RequestPaymentRefundResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            status = result.status,
            refundAmount = result.refundAmount,
            reason = result.reason,
            occurredAt = result.occurredAt,
        )
    }
}
