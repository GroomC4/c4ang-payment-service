package com.groom.payment.configuration.kafka

import com.groom.ecommerce.payment.event.avro.PaymentCancelled
import com.groom.ecommerce.payment.event.avro.PaymentCompleted
import com.groom.ecommerce.payment.event.avro.PaymentFailed
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * Kafka Producer 설정
 * - Payment 도메인 이벤트를 Kafka에 발행
 * - Schema Registry를 통한 Avro 직렬화
 */
@Configuration
class KafkaProducerConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.producer.properties.schema.registry.url}")
    private lateinit var schemaRegistryUrl: String

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            "schema.registry.url" to schemaRegistryUrl,
            // Producer 성능 설정
            ProducerConfig.ACKS_CONFIG to "all", // 모든 replica 확인
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 1, // 배치 대기 시간
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy"
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun paymentCompletedKafkaTemplate(): KafkaTemplate<String, PaymentCompleted> {
        return KafkaTemplate(producerFactory() as ProducerFactory<String, PaymentCompleted>)
    }

    @Bean
    fun paymentFailedKafkaTemplate(): KafkaTemplate<String, PaymentFailed> {
        return KafkaTemplate(producerFactory() as ProducerFactory<String, PaymentFailed>)
    }

    @Bean
    fun paymentCancelledKafkaTemplate(): KafkaTemplate<String, PaymentCancelled> {
        return KafkaTemplate(producerFactory() as ProducerFactory<String, PaymentCancelled>)
    }
}
