package org.spring.divas.order.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class JwtSecurityConfig {
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    RSAPublicKey jwtPublicKey(@Value("${security.jwt.public-key-url}") String keyUrl) throws InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        while (true) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(keyUrl)).timeout(Duration.ofSeconds(5)).GET().build();
                String encoded = client.send(request, HttpResponse.BodyHandlers.ofString()).body().trim();
                byte[] key = Base64.getDecoder().decode(encoded);
                return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
            } catch (Exception _) {
                Thread.sleep(3000);
            }
        }
    }

    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    JwtDecoder jwtDecoder(RSAPublicKey jwtPublicKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("user-service")));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Value("${security.jwt.enabled:true}") boolean jwtEnabled) {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    if (jwtEnabled) {
                        auth.requestMatchers("/actuator/health/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/order").permitAll()
                                .requestMatchers(HttpMethod.POST, "/order/*/pay").permitAll()
                                .requestMatchers(HttpMethod.PATCH, "/order/*/cancel").permitAll()
                                .anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                });
        if (jwtEnabled) {
            http.oauth2ResourceServer(resource ->
                    resource.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }
        return http.build();
    }
}
