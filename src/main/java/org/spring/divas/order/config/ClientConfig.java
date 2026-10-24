package org.spring.divas.order.config;

import org.spring.divas.order.feature.payment.PaymentClient;
import org.spring.divas.order.feature.user.UserClient;
import org.spring.divas.order.feature.venue.VenueClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientConfig {

    @Value("${venue.service.url}")
    private String venueServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Bean
    public VenueClient venueClient(RestClient.Builder builder) {
        return createClient(VenueClient.class, venueServiceUrl, builder);
    }

    @Bean
    public PaymentClient paymentClient(RestClient.Builder builder) {
        return createClient(PaymentClient.class, paymentServiceUrl, builder);
    }

    @Bean
    public UserClient userClient(RestClient.Builder builder) {
        return createClient(UserClient.class, userServiceUrl, builder);
    }

    private <T> T createClient(Class<T> clientClass, String baseUrl, RestClient.Builder builder) {
        RestClient restClient = builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory())
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(clientClass);
    }

    private ClientHttpRequestFactory createRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return requestFactory;
    }
}
