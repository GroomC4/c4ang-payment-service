package com.groom.payment.presentation.web

import com.groom.payment.application.dto.CompletePaymentCommand
import com.groom.payment.application.dto.CompletePaymentRefundCommand
import com.groom.payment.application.dto.MarkPaymentFailedCommand
import com.groom.payment.application.service.CompletePaymentRefundService
import com.groom.payment.application.service.CompletePaymentService
import com.groom.payment.application.service.MarkPaymentFailedService
import com.groom.payment.presentation.web.dto.CancelPaymentRequest
import com.groom.payment.presentation.web.dto.CancelPaymentResponse
import com.groom.payment.presentation.web.dto.CompletePaymentRequest
import com.groom.payment.presentation.web.dto.CompletePaymentResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * External Payment Gateway Callback API Controller
 *
 * PG사(Payment Gateway)로부터의 웹훅/콜백 요청을 처리하는 외부 API 컨트롤러
 *
 * 경로: /external/pg/callback/payment/ (wildcard)
 * 인증: PG사 전용 인증 방식 적용 (API Key, IP Whitelist 등)
 *
 * 처리 API:
 * - POST /external/pg/callback/payment/complete : 결제 완료 콜백
 * - POST /external/pg/callback/payment/refund : 환불 완료 콜백
 * - POST /external/pg/callback/payment/fail : 결제 실패 콜백
 */
@Tag(name = "External Payment", description = "외부 결제 시스템 연동 API (PG 콜백)")
@RestController
@RequestMapping("/external/pg/callback/payment")
class ExternalPaymentController(
    private val completePaymentService: CompletePaymentService,
    private val completePaymentRefundService: CompletePaymentRefundService,
    private val markPaymentFailedService: MarkPaymentFailedService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 결제 완료 (PG 콜백)
     *
     * POST /external/pg/callback/payment/complete
     *
     * PG 콜백 API: PG사가 결제 성공 시 호출
     * - 멱등성 보장 (idempotencyKey)
     * - 재고 예약 확정 (Redis → DB)
     * - 주문 상태 변경 (PAYMENT_COMPLETED)
     *
     * @param request 결제 완료 정보 (paymentId, pgApprovalNumber, idempotencyKey)
     * @return 결제 완료 결과 (status: PAYMENT_COMPLETED, alreadyProcessed)
     */
    @Operation(
        summary = "결제 완료 (PG 콜백)",
        description = "PG사로부터 결제 완료 콜백을 수신합니다. 멱등성이 보장됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "결제 완료 처리 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/complete")
    fun completePayment(
        @Valid @RequestBody request: CompletePaymentRequest,
    ): CompletePaymentResponse {
        logger.info {
            "Payment completion callback received: paymentId=${request.paymentId}, idempotencyKey=${request.idempotencyKey}"
        }

        val command =
            CompletePaymentCommand(
                paymentId = request.paymentId,
                pgApprovalNumber = request.pgApprovalNumber,
                idempotencyKey = request.idempotencyKey,
            )

        val result = completePaymentService.execute(command)

        return CompletePaymentResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            status = result.status,
            pgApprovalNumber = result.pgApprovalNumber,
            completedAt = result.completedAt,
            alreadyProcessed = result.alreadyProcessed,
        )
    }

    /**
     * 환불 완료 (PG 콜백)
     *
     * POST /external/pg/callback/payment/refund
     *
     * PG 콜백 API: PG사가 환불 완료 시 호출
     * - 멱등성 보장 (idempotencyKey)
     * - 주문 상태 변경 (REFUND_COMPLETED)
     *
     * @param request 환불 완료 정보 (paymentId, pgApprovalNumber는 환불 거래 ID, idempotencyKey)
     * @return 환불 완료 결과 (status: REFUND_COMPLETED, alreadyProcessed)
     */
    @Operation(
        summary = "환불 완료 (PG 콜백)",
        description = "PG사로부터 환불 완료 콜백을 수신합니다. 멱등성이 보장됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "환불 완료 처리 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/refund")
    fun completeRefund(
        @Valid @RequestBody request: CompletePaymentRequest,
    ): CompletePaymentResponse {
        logger.info {
            "Refund completion callback received: paymentId=${request.paymentId}, idempotencyKey=${request.idempotencyKey}"
        }

        val command =
            CompletePaymentRefundCommand(
                paymentId = request.paymentId,
                refundTransactionId = request.pgApprovalNumber,
                idempotencyKey = request.idempotencyKey,
            )

        val result = completePaymentRefundService.execute(command)

        if (result.alreadyProcessed) {
            logger.info { "Refund already processed (idempotent): paymentId=${result.paymentId}" }
        } else {
            logger.info { "Refund completed successfully: paymentId=${result.paymentId}" }
        }

        return CompletePaymentResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            status = result.status,
            pgApprovalNumber = result.refundTransactionId,
            completedAt = result.refundedAt,
            alreadyProcessed = result.alreadyProcessed,
        )
    }

    /**
     * 결제 실패 (PG 콜백)
     *
     * POST /external/pg/callback/payment/fail
     *
     * PG 콜백 API: PG사가 결제 실패 시 호출
     * - 재고 예약 복구
     * - 주문 상태 변경 (PAYMENT_FAILED)
     *
     * @param request 실패 정보 (paymentId, reason)
     * @return 실패 처리 결과 (currentStatus: PAYMENT_FAILED)
     */
    @Operation(
        summary = "결제 실패 (PG 콜백)",
        description = "PG사로부터 결제 실패 콜백을 수신합니다. 재고 예약이 복구됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "결제 실패 처리 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/fail")
    fun markFailed(
        @Valid @RequestBody request: CancelPaymentRequest,
    ): CancelPaymentResponse {
        logger.info { "Payment failure callback received: paymentId=${request.paymentId}, reason=${request.reason}" }

        val command =
            MarkPaymentFailedCommand(
                paymentId = request.paymentId,
                reason = request.reason,
            )

        val result = markPaymentFailedService.execute(command)

        logger.info { "Payment marked as failed: paymentId=${result.paymentId}, status=${result.status}" }

        return CancelPaymentResponse(
            paymentId = result.paymentId,
            orderId = result.orderId,
            previousStatus = result.status,
            currentStatus = result.status,
            reason = result.reason,
            cancelledAt = result.occurredAt,
        )
    }
}
