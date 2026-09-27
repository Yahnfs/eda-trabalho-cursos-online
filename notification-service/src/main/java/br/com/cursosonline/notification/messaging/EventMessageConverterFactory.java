package br.com.cursosonline.notification.messaging;

import java.util.Map;

import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import br.com.cursosonline.notification.event.CertificateIssued;
import br.com.cursosonline.notification.event.CourseCompleted;
import br.com.cursosonline.notification.event.EnrollmentCancelled;
import br.com.cursosonline.notification.event.EnrollmentCreated;
import tools.jackson.databind.json.JsonMapper;

/**
 * Conversor JSON das mensagens.
 * <p>
 * O header {@code __TypeId__} carrega um nome lógico do evento (ex.: {@code EnrollmentCreated}) e não
 * o nome da classe Java. Assim cada microsserviço mantém sua própria cópia do contrato, sem
 * compartilhar bibliotecas nem pacotes com os demais serviços.
 */
public final class EventMessageConverterFactory {

    static final Map<String, Class<?>> EVENT_TYPES = Map.of(
            "EnrollmentCreated", EnrollmentCreated.class,
            "EnrollmentCancelled", EnrollmentCancelled.class,
            "CourseCompleted", CourseCompleted.class,
            "CertificateIssued", CertificateIssued.class);

    private EventMessageConverterFactory() {
    }

    public static MessageConverter create(JsonMapper jsonMapper) {
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setIdClassMapping(EVENT_TYPES);
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
