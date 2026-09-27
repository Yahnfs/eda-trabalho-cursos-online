package br.com.cursosonline.certificate.config;

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

import br.com.cursosonline.certificate.messaging.DeadLetterSupport;
import br.com.cursosonline.certificate.messaging.EventMessageConverterFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Topologia RabbitMQ do Certificate Service.
 * <ul>
 *   <li>Consome {@code progress.course.completed} do topic exchange {@code courses.events}</li>
 *   <li>Publica {@code CertificateIssued} no fanout exchange {@code certificates.fanout} (broadcast)</li>
 *   <li>A fila possui DLQ ligada ao exchange direct {@code courses.dlx}</li>
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
    Declarables courseCompletedQueue(TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        Queue queue = DeadLetterSupport.queueWithDeadLetter(props.queues().courseCompleted(), deadLetterExchange);
        return DeadLetterSupport.withDeadLetter(queue, deadLetterExchange,
                BindingBuilder.bind(queue).to(eventsExchange).with(props.routingKeys().courseCompleted()));
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
