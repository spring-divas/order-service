package org.spring.divas.order.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_PATH = "/actuator";

    private final RateLimiter rateLimiter;

    private final HandlerExceptionResolver exceptionResolver;

    public RateLimitFilter(
            RateLimiterRegistry rateLimiterRegistry,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter("orderApi");
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return requestPath(request).startsWith(ACTUATOR_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (rateLimiter.acquirePermission()) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestNotPermitted exception = RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        if (exceptionResolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    private static String requestPath(HttpServletRequest request) {
        return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    }
}
