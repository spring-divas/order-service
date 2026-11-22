package org.spring.divas.order;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spring.divas.order.feature.order.OrderService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("order-service")
@Consumer("review-service")
@PactFolder("${pactfolder.path}")
class ReviewOrderPactVerificationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private OrderService orderService;

    @BeforeEach
    void configureTarget(PactVerificationContext context) {
        when(orderService.hasUserOrderedDish(123L, 456L)).thenReturn(true);
        when(orderService.hasUserBeenToVenue(123L, 456L)).thenReturn(true);
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifiesPact(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
