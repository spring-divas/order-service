package org.spring.divas.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "resilience4j.ratelimiter.instances.orderApi.limit-for-period=3",
        "resilience4j.ratelimiter.instances.orderApi.limit-refresh-period=1m",
        "resilience4j.ratelimiter.instances.orderApi.timeout-duration=0s"
})
@AutoConfigureMockMvc
class OrderRateLimiterIntegrationTest {

    private static final long MISSING_ORDER_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectRequestsWithProblemDetailWhenRateLimitIsExceeded() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/order/{id}/pay", MISSING_ORDER_ID))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(post("/order/{id}/pay", MISSING_ORDER_ID))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));

        mockMvc.perform(get("/actuator-other"))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(not(HttpStatus.TOO_MANY_REQUESTS.value())));

        mockMvc.perform(get(URI.create("/%61ctuator/health")))
                .andExpect(status().is(not(HttpStatus.TOO_MANY_REQUESTS.value())));
    }
}
