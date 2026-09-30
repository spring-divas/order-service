package org.spring.divas.order.config;

import org.spring.divas.order.feature.venue.VenueClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    @Value("${venue.service.url}")
    private String venueServiceUrl;

    @Bean
    public VenueClient venueClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(venueServiceUrl)
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(VenueClient.class);
    }
}
