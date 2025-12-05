package com.groom.payment.configuration.kafka

/**
 * Kafka Topic 상수 정의
 * - 토픽 이름을 중앙에서 관리
 * - 오타 방지 및 유지보수 용이
 *
 * 참고: c4ang-contract-hub/docs/interface/kafka-event-specifications.md
 */
object KafkaTopics {
    // Payment Service가 발행하는 이벤트
    const val PAYMENT_COMPLETED = "payment.completed"
    const val PAYMENT_FAILED = "payment.failed"
    const val PAYMENT_CANCELLED = "payment.cancelled"

    // Payment Service가 구독하는 이벤트
    const val ORDER_CONFIRMED = "order.confirmed"

    // SAGA 이벤트 - Payment Service가 소비
    const val SAGA_STOCK_CONFIRMATION_FAILED = "saga.stock-confirmation.failed"

    // SAGA 이벤트 - Payment Service가 발행
    const val SAGA_PAYMENT_INITIALIZATION_FAILED = "saga.payment-initialization.failed"
}
