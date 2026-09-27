package br.com.cursosonline.enrollment.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.cursosonline.enrollment.messaging.DeadLetterSupport;
import br.com.cursosonline.enrollment.messaging.EventMessageConverterFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Topologia RabbitMQ do Enrollment Service.
 * <ul>
 *   <li>Publica em {@code courses.events} (topic): enrollment.created / enrollment.cancelled</li>
 *   <li>Consome {@code progress.#} do topic exchange (progresso e conclusão de curso)</li>
 *   <li>Consome {@code certificates.fanout} (broadcast de certificados emitidos)</li>
 *   <li>Cada fila possui uma DLQ ligada ao exchange direct {@code courses.dlx}</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    private final RabbitTopologyProperties props;

    public RabbitMQConfig(RabbitTopologyProperties props) {
        this.props = props;
    }

    @Bean
    TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(props.eventsExchange()).durable(true).build();
    }

    @Bean
    FanoutExchange certificatesExchange() {
        return ExchangeBuilder.fanoutExchange(props.certificatesExchange()).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.deadLetterExchange()).durable(true).build();
    }

    @Bean
    Declarables progressEventsQueue(TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        Queue queue = DeadLetterSupport.queueWithDeadLetter(props.queues().progressEvents(), deadLetterExchange);
        return DeadLetterSupport.withDeadLetter(queue, deadLetterExchange,
                BindingBuilder.bind(queue).to(eventsExchange).with(props.routingKeys().progressEvents()));
    }

    @Bean
    Declarables certificateIssuedQueue(FanoutExchange certificatesExchange, DirectExchange deadLetterExchange) {
        Queue queue = DeadLetterSupport.queueWithDeadLetter(props.queues().certificateIssued(), deadLetterExchange);
        return DeadLetterSupport.withDeadLetter(queue, deadLetterExchange,
                BindingBuilder.bind(queue).to(certificatesExchange));
    }

    @Bean
    MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return EventMessageConverterFactory.create(jsonMapper);
    }

    @Bean
    MessageRecoverer deadLetterRecoverer() {
        return DeadLetterSupport.loggingRecoverer();
    }
}
