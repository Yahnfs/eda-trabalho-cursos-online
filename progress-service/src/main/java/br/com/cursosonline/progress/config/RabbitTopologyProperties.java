package br.com.cursosonline.progress.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes de exchanges, routing keys e filas usados pelo serviço (definidos em application.properties).
 */
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitTopologyProperties(
        String eventsExchange,
        String deadLetterExchange,
        RoutingKeys routingKeys,
        Queues queues) {

    public record RoutingKeys(String lessonCompleted, String courseCompleted, String enrollmentEvents) {
    }

    public record Queues(String enrollmentEvents) {
    }
}
