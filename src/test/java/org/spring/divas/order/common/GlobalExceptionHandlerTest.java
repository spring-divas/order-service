package org.spring.divas.order.common;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.order.OrderNotFoundException;
import org.spring.divas.order.feature.orderitem.OrderItemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleOrderNotFound() {
        OrderNotFoundException ex = new OrderNotFoundException(10L);

        ProblemDetail problem = handler.handleOrderNotFound(ex);

        assertNotNull(problem);
        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Order Not Found", problem.getTitle());
        assertEquals("Order with id 10 not found", problem.getDetail());
        assertEquals(URI.create("about:blank"), problem.getType());
        assertNotNull(problem.getProperties());
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void shouldHandleOrderItemNotFound() {
        OrderItemNotFoundException ex = new OrderItemNotFoundException(25L);

        ProblemDetail problem = handler.handleOrderItemNotFound(ex);

        assertNotNull(problem);
        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Order Item Not Found", problem.getTitle());
        assertEquals("Order Item with id 25 not found", problem.getDetail());
        assertEquals(URI.create("about:blank"), problem.getType());
        assertNotNull(problem.getProperties());
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void shouldHandleRateLimitExceeded() {
        RequestNotPermitted ex = mock(RequestNotPermitted.class);

        ProblemDetail problem = handler.handleRateLimitExceeded(ex);

        assertNotNull(problem);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), problem.getStatus());
        assertEquals("Rate Limit Exceeded", problem.getTitle());
        assertEquals("Too many requests. Please try again later.", problem.getDetail());
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void shouldHandleValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("orderRequest", "venueId", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail problem = handler.handleValidation(ex);

        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Validation Error", problem.getTitle());
        assertEquals("Request validation failed", problem.getDetail());

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals("must not be null", errors.get("venueId"));
    }

    @Test
    void shouldHandleConstraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException(
                "tableId: must not be null", Set.of()
        );

        ProblemDetail problem = handler.handleConstraintViolation(ex);

        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Validation Error", problem.getTitle());
        assertEquals("tableId: must not be null", problem.getDetail());
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void shouldHandleUnexpectedException() {
        Exception ex = new RuntimeException("Database connection timeout");

        ProblemDetail problem = handler.handleUnexpectedException(ex);

        assertNotNull(problem);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("An unexpected error occurred", problem.getDetail());
        assertNotNull(problem.getProperties().get("timestamp"));
    }
}
