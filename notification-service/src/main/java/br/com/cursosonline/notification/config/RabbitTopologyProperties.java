package br.com.cursosonline.notification.config;

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

    public record RoutingKeys(String enrollmentEvents, String courseCompleted) {
    }

    public record Queues(String courseEvents, String certificateIssued) {
    }
}
