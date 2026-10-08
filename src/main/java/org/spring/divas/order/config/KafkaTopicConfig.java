package org.spring.divas.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic dishEventsTopic() {
        return TopicBuilder.name("dish-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
