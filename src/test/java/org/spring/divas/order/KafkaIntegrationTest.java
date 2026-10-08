package org.spring.divas.order;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"dish-events", "dish-events-dlt"})
public abstract class KafkaIntegrationTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected EmbeddedKafkaBroker broker;

    @Autowired
    protected ObjectMapper objectMapper;

    @SuppressWarnings("SameParameterValue")
    protected Consumer<String, String> consumer(String topic) {
        var props = KafkaTestUtils.consumerProps(
                broker, "test-group", true);
        var consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    protected ConsumerRecord<String, String> awaitRecord(
            Consumer<String, String> consumer, String marker
    ) {
        var received = new ArrayList<ConsumerRecord<String, String>>();
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            consumer.poll(Duration.ofMillis(200)).forEach(received::add);
            return received.stream().anyMatch(record -> record.value().contains(marker));
        });
        return received.stream()
                .filter(record -> record.value().contains(marker))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> T read(ConsumerRecord<String, String> record, Class<T> type) {
        return objectMapper.readValue(record.value(), type);
    }
}
