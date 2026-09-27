package br.com.cursosonline.certificate.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.certificate.event.DomainEvent;

/**
 * Garante que cada evento seja processado uma única vez (consumidor idempotente).
 * <p>
 * Deve ser chamado dentro da mesma transação que aplica o efeito do evento: o registro em
 * {@code processed_events} e a alteração de negócio são confirmados (ou desfeitos) juntos. Como o
 * ACK só é enviado ao RabbitMQ depois do commit, uma reentrega (at-least-once) é detectada aqui.
 */
@Component
public class IdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyGuard.class);

    private final ProcessedEventRepository repository;

    public IdempotencyGuard(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /** @return {@code true} se é a primeira vez que o evento é visto (e o registra); {@code false} se é duplicado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(DomainEvent event) {
        if (repository.existsById(event.eventId())) {
            log.warn("[IDEMPOTENCY] Evento {} eventId={} já processado anteriormente - ignorando duplicata",
                    event.getClass().getSimpleName(), event.eventId());
            return false;
        }
        repository.save(new ProcessedEvent(event.eventId(), event.getClass().getSimpleName()));
        return true;
    }
}
