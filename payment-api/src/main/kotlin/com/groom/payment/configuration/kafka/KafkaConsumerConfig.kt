package com.groom.payment.configuration.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.util.backoff.FixedBackOff

/**
 * Kafka Consumer 설정
 * - Order 도메인 이벤트를 구독
 * - Schema Registry를 통한 Avro 역직렬화
 *
 * ErrorHandlingDeserializer를 사용하여 역직렬화 실패 시에도
 * 무한 재시도를 방지하고 정상 메시지 처리를 계속합니다.
 */
@Configuration
class KafkaConsumerConfig {
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var groupId: String

    @Value("\${spring.kafka.consumer.properties.schema.registry.url}")
    private lateinit var schemaRegistryUrl: String

    /**
     * 공통 Consumer Factory
     * - 모든 Order 이벤트에 사용
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val configProps =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                // ErrorHandlingDeserializer로 래핑하여 역직렬화 에러 처리
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to KafkaAvroDeserializer::class.java,
                "schema.registry.url" to schemaRegistryUrl,
                "specific.avro.reader" to true, // SpecificRecord 사용
                // Consumer 설정
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false, // 수동 커밋
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 10,
            )
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(3) // 동시 처리 스레드 수
        factory.containerProperties.isMissingTopicsFatal = false // 토픽 없어도 시작
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL // 수동 커밋 활성화
        // 에러 핸들러: 3회 재시도 후 스킵 (1초 간격)
        factory.setCommonErrorHandler(
            DefaultErrorHandler(FixedBackOff(1000L, 3L))
        )
        return factory
    }
}
