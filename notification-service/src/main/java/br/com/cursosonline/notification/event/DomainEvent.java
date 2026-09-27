package br.com.cursosonline.notification.event;

import java.time.Instant;

/**
 * Contrato comum dos eventos: todo evento carrega um identificador único (usado para idempotência)
 * e o instante em que o fato ocorreu.
 */
public interface DomainEvent {

    String eventId();

    Instant occurredAt();
}
