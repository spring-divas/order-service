package org.spring.divas.order;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.outbox.OutboxEvent;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.outbox.OutboxStatusService;
import org.spring.divas.order.feature.outbox.PollingOutboxScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-created"})
class OutboxKafkaIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxStatusService outboxStatusService;

    @Autowired
    private PollingOutboxScheduler scheduler;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pendingOutboxEventIsPublishedToKafka() throws Exception {
        UUID eventId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "orderId": 321,
                  "occurredAt": "%s"
                }
                """.formatted(eventId, Instant.now());

        OutboxEvent event = outboxRepository.save(
                OutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateType("Order")
                        .aggregateId("321")
                        .eventType("OrderCreated")
                        .payload(payload)
                        .build()
        );

        Map<String, Object> properties = KafkaTestUtils.consumerProps(
                embeddedKafka,
                "outbox-test-" + UUID.randomUUID(),
                false
        );

        properties.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        try (Consumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "order-created");

            scheduler.publishPendingEvents();

            ConsumerRecord<String, String> kafkaRecord = null;
            long deadline = System.currentTimeMillis() + 10000;

            while (System.currentTimeMillis() < deadline && kafkaRecord == null) {
                var records = consumer.poll(Duration.ofMillis(250));

                for (var item : records) {
                    if (item.value().contains(eventId.toString())) {
                        kafkaRecord = item;
                        break;
                    }
                }
            }

            assertThat(kafkaRecord).isNotNull();
            assertThat(kafkaRecord.key()).isEqualTo("321");
            assertThat(kafkaRecord.value()).contains(eventId.toString());
            JsonNode publishedJson = objectMapper.readTree(kafkaRecord.value());
            assertThat(publishedJson.get("eventId").asString()).isEqualTo(eventId.toString());
            assertThat(publishedJson.get("orderId").longValue()).isEqualTo(321L);

            OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();

            assertThat(updated.getPublishedAt()).isNotNull();
            assertThat(updated.getAttempts()).isZero();


        }
    }

    @Test
    void alreadyPublishedEventIsNotPublishedToKafkaAgain(){
        UUID eventId = UUID.randomUUID();

        String payload = """
            {
              "eventId": "%s",
              "orderId": 915,
              "occurredAt": "%s"
            }
            """.formatted(eventId, Instant.now());

        OutboxEvent event = outboxRepository.saveAndFlush(
                OutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateType("Order")
                        .aggregateId("915")
                        .eventType("OrderCreated")
                        .payload(payload)
                        .build()
        );

        outboxStatusService.markPublished(event.getId());

        Map<String, Object> properties = KafkaTestUtils.consumerProps(
                embeddedKafka,
                "outbox-test-" + UUID.randomUUID(),
                false
        );

        properties.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        try (Consumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            embeddedKafka.consumeFromAnEmbeddedTopic(
                    consumer,
                    "order-created"
            );

            scheduler.publishPendingEvents();

            boolean eventWasRepublished = false;
            long deadline = System.currentTimeMillis() + 2000;

            while (System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(200));

                for (var kafkaRecord : records) {
                    if (kafkaRecord.value().contains(eventId.toString())) {
                        eventWasRepublished = true;
                        break;
                    }
                }

                if (eventWasRepublished) {
                    break;
                }
            }

            assertThat(eventWasRepublished).isFalse();

            OutboxEvent persisted = outboxRepository.findById(event.getId())
                    .orElseThrow();

            assertThat(persisted.getPublishedAt()).isNotNull();
        }
    }

}