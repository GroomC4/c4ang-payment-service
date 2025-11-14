package com.groom.payment.application.event

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 주문 타임아웃 이벤트 핸들러 (Payment 도메인)
 *
 * Order 도메인에서 발행한 OrderTimeoutEvent를 수신하여
 * 연결된 Payment를 실패 처리합니다.
 *
 * DDD 패턴:
 * - 도메인 간 이벤트 기반 통신
 * - Payment 도메인의 독립성 유지
 * - MSA 전환 준비 (이벤트만 Message Queue로 전환)
 *
 * 트랜잭션:
 * - REQUIRES_NEW: Order 트랜잭션과 독립적으로 실행
 * - 실패해도 Order 타임아웃 처리는 완료됨
 */
@Component
class PaymentOrderTimeoutEventHandler {
    private val logger = KotlinLogging.logger {}

    // TODO("Order 서비스 연동 필요 - OrderTimeoutEvent 수신 후 Payment 실패 처리")
}
