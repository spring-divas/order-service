package org.spring.divas.order;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spring.divas.order.feature.user.UserClient;
import org.spring.divas.order.feature.user.UserResponseDto;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "user-service")
class UserPactTest {

    @Pact(consumer = "order-service", provider = "user-service")
    public V4Pact createUserPact(PactBuilder builder) {
        return builder
                .expectsToReceiveHttpInteraction("a request for a user", interaction -> interaction
                        .withRequest(request -> request
                                .path("/api/user/123")
                                .method("GET"))
                        .willRespondWith(response -> response
                                .status(200)
                                .body(new PactDslJsonBody()
                                        .integerType("id", 123L)
                                        .stringType("firstName", "Oksana")
                                        .stringType("lastName", "Stepanenko")
                                        .stringType("email", "oksana@example.com")
                                        .stringType("role", "CUSTOMER"))))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createUserPact")
    void clientReadsUserFromContract(MockServer mockServer) {
        UserClient client = createClient(mockServer.getUrl());
        UserResponseDto user = client.findById(123L);
        assertThat(user.getId()).isEqualTo(123L);
        assertThat(user.getEmail()).isEqualTo("oksana@example.com");
    }

    private UserClient createClient(String baseUrl) {
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(UserClient.class);
    }
}
