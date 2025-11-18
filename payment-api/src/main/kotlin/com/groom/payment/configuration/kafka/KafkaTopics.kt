package com.groom.payment.configuration.kafka

/**
 * Kafka Topic 상수 정의
 * - 토픽 이름을 중앙에서 관리
 * - 오타 방지 및 유지보수 용이
 */
object KafkaTopics {
    // Payment Service가 발행하는 이벤트
    const val PAYMENT_COMPLETED = "payment.completed"
    const val PAYMENT_FAILED = "payment.failed"
    const val PAYMENT_CANCELLED = "payment.cancelled"

    // Payment Service가 구독하는 이벤트
    const val ORDER_CREATED = "order.created"
    const val ORDER_CONFIRMED = "order.confirmed"
    const val STOCK_CONFIRMED = "stock.confirmed"
}
