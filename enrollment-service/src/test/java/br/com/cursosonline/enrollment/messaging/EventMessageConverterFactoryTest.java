package br.com.cursosonline.enrollment.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import br.com.cursosonline.enrollment.event.EnrollmentCreated;
import tools.jackson.databind.json.JsonMapper;

/** Garante o contrato JSON: header __TypeId__ lógico e payload legível pelos outros serviços. */
class EventMessageConverterFactoryTest {

    private final MessageConverter converter = EventMessageConverterFactory.create(JsonMapper.builder().build());

    @Test
    void serializesEventWithLogicalTypeIdAndDeserializesBack() {
        EnrollmentCreated event = new EnrollmentCreated("evt-1", Instant.parse("2026-08-01T10:00:00Z"), 1L,
                "aluno-1", "Maria", "maria@email.com", "EDA-101", "EDA", 3);

        Message message = converter.toMessage(event, new MessageProperties());

        assertThat(message.getMessageProperties().getHeaders()).containsEntry("__TypeId__", "EnrollmentCreated");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"eventId\":\"evt-1\"", "\"occurredAt\":\"2026-08-01T10:00:00Z\"",
                "\"courseId\":\"EDA-101\"");
        assertThat(converter.fromMessage(message)).isEqualTo(event);
    }

    @Test
    void rejectsUnknownEventType() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setHeader("__TypeId__", "com.hacker.Malicious");
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);

        // Tipos fora do mapeamento não são instanciados (proteção contra desserialização arbitrária).
        assertThatThrownBy(() -> converter.fromMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in the trusted packages");
    }
}
