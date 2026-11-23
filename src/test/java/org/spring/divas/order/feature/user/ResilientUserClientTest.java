package org.spring.divas.order.feature.user;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

@SpringBootTest(properties = {
        "resilience4j.retry.instances.userClient.max-attempts=1",
        "resilience4j.circuitbreaker.instances.userClient.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.userClient.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.userClient.failure-rate-threshold=50"
})
class ResilientUserClientTest {
    private static final int NETWORK_DELAY_MS = 3_100;
    private static final String USER_PATH = "/api/user/" + UserFixture.USER_ID;

    @RegisterExtension
    static WireMockExtension userService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private ResilientUserClient userClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Autowired
    private ObjectMapper objectMapper;

    private CircuitBreaker circuitBreaker;

    @DynamicPropertySource
    static void registerUserServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("user.service.url", () -> "http://localhost:" + userService.getPort());
    }

    @BeforeEach
    void setUp() {
        userService.resetAll();
        circuitBreaker = circuitBreakers.circuitBreaker("userClient");
        circuitBreaker.reset();
    }

    @Test
    void returnsUserWhenUserServiceResponds() {
        UserResponseDto expected = UserFixture.response();
        stubUser(expected, 0);
        UserResponseDto user = userClient.findById(UserFixture.USER_ID);
        assertThat(user).isEqualTo(expected);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void returnsEmptyUserWhenUserServiceTimesOut() {
        stubUser(UserFixture.response(), NETWORK_DELAY_MS);
        assertThat(userClient.findById(UserFixture.USER_ID))
                .isEqualTo(UserFixture.fallbackResponse());
    }

    @Test
    void opensCircuitBreakerAndUsesEmptyUserWhenUserServiceIsSlow() {
        stubUser(UserFixture.response(), NETWORK_DELAY_MS);
        assertThat(userClient.findById(UserFixture.USER_ID))
                .isEqualTo(UserFixture.fallbackResponse());
        assertThat(userClient.findById(UserFixture.USER_ID))
                .isEqualTo(UserFixture.fallbackResponse());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        userService.resetRequests();
        assertThat(userClient.findById(UserFixture.USER_ID))
                .isEqualTo(UserFixture.fallbackResponse());
        userService.verify(0, getRequestedFor(urlEqualTo(USER_PATH)));
    }

    private void stubUser(UserResponseDto user, int delay) {
        userService.stubFor(get(urlEqualTo(USER_PATH))
                .willReturn(aResponse()
                        .withFixedDelay(delay)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(user))));
    }

    @UtilityClass
    private static class UserFixture {
        static final long USER_ID = 1L;

        UserResponseDto response() {
            return UserResponseDto.builder()
                    .id(USER_ID)
                    .firstName("Mariia")
                    .lastName("Stepanenko")
                    .email("user@example.com")
                    .role(UserRole.CUSTOMER)
                    .build();
        }

        UserResponseDto fallbackResponse() {
            return new UserResponseDto(USER_ID, "", "", "", null);
        }
    }
}
