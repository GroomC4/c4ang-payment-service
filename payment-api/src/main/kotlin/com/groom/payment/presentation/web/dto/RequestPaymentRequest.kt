package com.groom.payment.presentation.web.dto

import com.groom.payment.domain.model.PaymentMethod
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

/**
 * 결제 요청 Request
 *
 * 사용자가 결제 수단과 금액 정보를 확인하고 결제를 시작하는 요청
 *
 * 금액 정보는 프론트엔드에서 주문 정보를 조회한 후 전달합니다.
 */
@Schema(description = "결제 요청")
data class RequestPaymentRequest(
    @Schema(description = "결제 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @field:NotNull(message = "Payment ID is required")
    val paymentId: UUID,
    @Schema(description = "결제 수단", example = "CARD", required = true)
    @field:NotNull(message = "Payment method is required")
    val paymentMethod: PaymentMethod,
    @Schema(description = "총 주문 금액", example = "50000.00", required = true)
    @field:NotNull(message = "Total amount is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    val totalAmount: BigDecimal,
    @Schema(description = "실제 결제 금액", example = "48000.00", required = true)
    @field:NotNull(message = "Payment amount is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than 0")
    val paymentAmount: BigDecimal,
    @Schema(description = "할인 금액", example = "5000.00", required = true)
    @field:NotNull(message = "Discount amount is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Discount amount must be >= 0")
    val discountAmount: BigDecimal,
    @Schema(description = "배송비", example = "3000.00", required = true)
    @field:NotNull(message = "Delivery fee is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Delivery fee must be >= 0")
    val deliveryFee: BigDecimal,
)
