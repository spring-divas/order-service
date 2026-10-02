package org.spring.divas.order;

import org.spring.divas.order.feature.venue.VenueClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

@TestConfiguration
public class TestClientConfig {

    @Value("${venue.service.url}")
    private String venueClientUl;

    @Bean
    @Primary
    @Qualifier("testVenueClient")
    public VenueClient testVenueClient(RestClient.Builder builder) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = builder
                .baseUrl(venueClientUl)
                .requestFactory(new JdkClientHttpRequestFactory(client))
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(VenueClient.class);
    }
}
