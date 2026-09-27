package br.com.cursosonline.certificate.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import br.com.cursosonline.certificate.event.DomainEvent;

/**
 * Publica eventos no RabbitMQ em formato JSON.
 * <p>
 * Usa publisher confirms (o broker confirma o recebimento) e mandatory/returns (avisa quando nenhuma
 * fila está ligada à routing key), registrando ambos nos logs.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            String eventId = correlation != null ? correlation.getId() : "?";
            if (ack) {
                log.debug("[CONFIRM] Broker confirmou o evento eventId={}", eventId);
            } else {
                log.error("[CONFIRM] Broker NÃO confirmou o evento eventId={}: {}", eventId, cause);
            }
        });
        this.rabbitTemplate.setReturnsCallback(returned -> log.warn(
                "[RETURN] Evento sem fila de destino: exchange={} routingKey={} motivo={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
    }

    public void publish(String exchange, String routingKey, DomainEvent event) {
        log.info("[PUBLISH] {} eventId={} -> exchange='{}' routingKey='{}' payload={}",
                event.getClass().getSimpleName(), event.eventId(), exchange, routingKey, event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
            message.getMessageProperties().setMessageId(event.eventId());
            message.getMessageProperties().setAppId("certificate-service");
            return message;
        }, new CorrelationData(event.eventId()));
    }
}
