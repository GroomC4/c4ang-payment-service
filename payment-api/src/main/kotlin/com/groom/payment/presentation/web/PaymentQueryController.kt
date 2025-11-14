package com.groom.payment.presentation.web

import com.groom.payment.application.dto.GetPaymentQuery
import com.groom.payment.application.dto.ListPaymentsQuery
import com.groom.payment.application.service.GetPaymentService
import com.groom.payment.application.service.ListPaymentsService
import com.groom.payment.domain.model.PaymentStatus
import com.groom.payment.presentation.web.dto.GetPaymentResponse
import com.groom.payment.presentation.web.dto.ListPaymentsResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Payment 조회(Query) REST API Controller
 *
 * 결제 정보 조회 전용 API 엔드포인트
 *
 * 구현된 조회 API:
 * - GET /api/v1/payments/{paymentId} - 결제 상세 조회
 * - GET /api/v1/payments - 결제 목록 조회 (사용자별, 상태 필터링, 페이지네이션)
 */
@Tag(name = "Payment Query", description = "결제 조회 API")
@RestController
@RequestMapping("/api/v1/payments")
class PaymentQueryController(
    private val getPaymentService: GetPaymentService,
    private val listPaymentsService: ListPaymentsService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 결제 상세 조회 API
     *
     * @param paymentId 조회할 결제 ID
     * @param includeHistory 결제 이력 포함 여부 (기본값: false)
     * @return 결제 상세 정보
     */
    @Operation(summary = "결제 상세 조회", description = "결제 ID로 결제 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "결제 상세 조회 성공"),
            ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음"),
        ],
    )
    @GetMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.OK)
    fun getPayment(
        @Parameter(description = "결제 ID", required = true)
        @PathVariable
        paymentId: UUID,
        @Parameter(description = "결제 이력 포함 여부", required = false)
        @RequestParam(required = false, defaultValue = "false")
        includeHistory: Boolean,
    ): ResponseEntity<GetPaymentResponse> {
        logger.info { "GET /api/v1/payments/$paymentId?includeHistory=$includeHistory" }

        val query =
            GetPaymentQuery(
                paymentId = paymentId,
                includeHistory = includeHistory,
            )

        val result = getPaymentService.execute(query)
        return ResponseEntity.ok(GetPaymentResponse.from(result))
    }

    /**
     * 결제 목록 조회 API
     *
     * @param userId 사용자 ID
     * @param status 결제 상태 필터 (옵션)
     * @param page 페이지 번호 (기본값: 1)
     * @param limit 페이지당 개수 (기본값: 20)
     * @return 결제 목록 및 페이지네이션 정보
     */
    @Operation(summary = "결제 목록 조회", description = "사용자별 결제 목록을 조회합니다. 상태 필터링 및 페이지네이션을 지원합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "결제 목록 조회 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        ],
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun listPayments(
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam
        userId: UUID,
        @Parameter(description = "결제 상태 필터", required = false)
        @RequestParam(required = false)
        status: PaymentStatus?,
        @Parameter(description = "페이지 번호 (1부터 시작)", required = false)
        @RequestParam(required = false, defaultValue = "1")
        page: Int,
        @Parameter(description = "페이지당 결제 개수", required = false)
        @RequestParam(required = false, defaultValue = "20")
        limit: Int,
    ): ResponseEntity<ListPaymentsResponse> {
        logger.info { "GET /api/v1/payments?userId=$userId&status=$status&page=$page&limit=$limit" }

        val query =
            ListPaymentsQuery(
                userId = userId,
                status = status,
                page = page,
                limit = limit,
            )

        val result = listPaymentsService.execute(query)
        return ResponseEntity.ok(ListPaymentsResponse.from(result))
    }
}
