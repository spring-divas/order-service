package org.spring.divas.order.feature.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxStatusService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public void markPublished(Long id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
        });
    }

    @Transactional
    public void recordFailure(Long id, Exception exception) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setAttempts(event.getAttempts() + 1);

            String message = exception.getMessage();
            event.setLastError(
                    message == null
                            ? exception.getClass().getSimpleName()
                            : message.substring(0, Math.min(message.length(), 2000))
            );
        });
    }
}