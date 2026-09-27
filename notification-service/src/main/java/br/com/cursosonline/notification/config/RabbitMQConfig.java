package br.com.cursosonline.notification.config;

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

import br.com.cursosonline.notification.messaging.DeadLetterSupport;
import br.com.cursosonline.notification.messaging.EventMessageConverterFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Topologia RabbitMQ do Notification Service.
 * <ul>
 *   <li>Uma fila ligada ao topic exchange com DOIS bindings: {@code enrollment.*} e {@code progress.course.completed}</li>
 *   <li>Uma fila ligada ao fanout {@code certificates.fanout}</li>
 *   <li>Cada fila possui DLQ ligada ao exchange direct {@code courses.dlx}</li>
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
    Declarables courseEventsQueue(TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        Queue queue = DeadLetterSupport.queueWithDeadLetter(props.queues().courseEvents(), deadLetterExchange);
        return DeadLetterSupport.withDeadLetter(queue, deadLetterExchange,
                BindingBuilder.bind(queue).to(eventsExchange).with(props.routingKeys().enrollmentEvents()),
                BindingBuilder.bind(queue).to(eventsExchange).with(props.routingKeys().courseCompleted()));
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
