package org.spring.divas.order;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.dish.kafka.DishCreatedEvent;
import org.spring.divas.order.feature.dish.kafka.DishEventListener;
import org.spring.divas.order.feature.dish.kafka.DishEventMapper;
import org.spring.divas.order.feature.dish.kafka.DishUpdatedEvent;
import org.spring.divas.order.feature.processed.ProcessedEventRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "venue-and-menu-service", providerType = ProviderType.ASYNCH)
class DishEventsPactTest {

    @Pact(consumer = "order-service", provider = "venue-and-menu-service")
    public V4Pact createDishEventsPact(PactBuilder builder) {
        PactDslJsonBody content = dishEventContent();
        return builder
                .expectsToReceiveMessageInteraction("a DishCreated event", message -> message
                        .withContents(contents -> contents
                                .withMetadata(Map.of("eventType", "DishCreated"))
                                .withContent(content)))
                .expectsToReceiveMessageInteraction("a DishUpdated event", message -> message
                        .withContents(contents -> contents
                                .withMetadata(Map.of("eventType", "DishUpdated"))
                                .withContent(content)))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createDishEventsPact")
    void consumerProcessesContractMessages(List<V4Interaction.AsynchronousMessage> messages) {
        DishService dishService = mock(DishService.class);
        ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        DishEventListener listener = new DishEventListener(
                new DishEventMapper(),
                dishService,
                processedEventRepository,
                new JsonMapper()
        );
        handleCreatedEvent(listener, messages.getFirst());
        handleUpdatedEvent(listener, messages.getLast());
        verify(dishService).create(any());
        verify(dishService).update(any());
    }

    private PactDslJsonBody dishEventContent() {
        return new PactDslJsonBody()
                .uuid("eventId")
                .integerType("dishId", 123L)
                .integerType("venueId", 456L)
                .stringType("name", "Borscht")
                .decimalType("price", new BigDecimal("120.50"));
    }

    private void handleCreatedEvent(
            DishEventListener listener,
            V4Interaction.AsynchronousMessage message
    ) {
        String payload = new String(message.contentsAsBytes(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new JsonMapper();
        DishCreatedEvent event = objectMapper.readValue(payload, DishCreatedEvent.class);
        listener.handleDishEvent(payload, event.eventId().toString(), "DishCreated");
    }

    private void handleUpdatedEvent(
            DishEventListener listener,
            V4Interaction.AsynchronousMessage message
    ) {
        String payload = new String(message.contentsAsBytes(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new JsonMapper();
        DishUpdatedEvent event = objectMapper.readValue(payload, DishUpdatedEvent.class);
        listener.handleDishEvent(payload, event.eventId().toString(), "DishUpdated");
    }
}
