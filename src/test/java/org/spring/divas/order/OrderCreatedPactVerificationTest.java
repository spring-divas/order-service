package org.spring.divas.order;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderEventMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Provider("order-service")
@Consumer("payment-service")
@PactFolder("${pactfolder.path}")
class OrderCreatedPactVerificationTest {

    private final OrderEventMapper orderEventMapper = new OrderEventMapper(new JsonMapper());

    @BeforeEach
    @SuppressWarnings("JUnitMalformedDeclaration")
    void configureTarget(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget(List.of(getClass().getPackageName())));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifiesPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @PactVerifyProvider("an OrderCreated event")
    String orderCreatedEvent() {
        return orderEventMapper
                .toOutboxEvent(Order.builder().id(123L).build())
                .getPayload();
    }
}
