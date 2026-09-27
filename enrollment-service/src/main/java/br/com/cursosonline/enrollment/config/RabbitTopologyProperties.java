package br.com.cursosonline.enrollment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes de exchanges, routing keys e filas usados pelo serviço (definidos em application.properties).
 */
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitTopologyProperties(
        String eventsExchange,
        String certificatesExchange,
        String deadLetterExchange,
        RoutingKeys routingKeys,
        Queues queues) {

    public record RoutingKeys(String enrollmentCreated, String enrollmentCancelled, String progressEvents) {
    }

    public record Queues(String progressEvents, String certificateIssued) {
    }
}
