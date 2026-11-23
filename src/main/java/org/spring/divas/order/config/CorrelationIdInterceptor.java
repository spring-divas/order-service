package org.spring.divas.order.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        String correlationId = CorrelationIdContext.get();

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        request.getHeaders().set(CORRELATION_ID, correlationId);
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            request.getHeaders().setBearerAuth(auth.getToken().getTokenValue());
        }

        return execution.execute(request, body);
    }
}
