package br.com.cursosonline.certificate.messaging;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;

/**
 * Utilitários para montar filas com Dead Letter Queue (DLQ).
 * <p>
 * Toda fila de consumo é criada com {@code x-dead-letter-exchange = courses.dlx} e
 * {@code x-dead-letter-routing-key = <fila>.dlq}. Quando uma mensagem é rejeitada sem requeue,
 * o RabbitMQ a republica no exchange direct de dead letter, que a roteia para {@code <fila>.dlq}.
 */
public final class DeadLetterSupport {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterSupport.class);

    private DeadLetterSupport() {
    }

    public static Queue queueWithDeadLetter(String name, DirectExchange deadLetterExchange) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(deadLetterExchange.getName())
                .deadLetterRoutingKey(dlqName(name))
                .build();
    }

    /** Agrupa a fila principal, seus bindings, a DLQ e o binding da DLQ no exchange de dead letter. */
    public static Declarables withDeadLetter(Queue queue, DirectExchange deadLetterExchange, Binding... bindings) {
        Queue dlq = QueueBuilder.durable(dlqName(queue.getName())).build();
        List<Declarable> declarables = new ArrayList<>(List.of(queue, dlq,
                BindingBuilder.bind(dlq).to(deadLetterExchange).with(dlqName(queue.getName()))));
        declarables.addAll(List.of(bindings));
        return new Declarables(declarables);
    }

    /**
     * Recoverer executado quando as retentativas com backoff exponencial se esgotam: registra o erro
     * e rejeita a mensagem sem requeue, fazendo o broker encaminhá-la para a DLQ.
     */
    public static MessageRecoverer loggingRecoverer() {
        RejectAndDontRequeueRecoverer delegate = new RejectAndDontRequeueRecoverer();
        return (message, cause) -> {
            log.error("[DLQ] Retentativas esgotadas: messageId={} fila={} tipo={} -> enviada para DLQ. Causa: {}",
                    message.getMessageProperties().getMessageId(),
                    message.getMessageProperties().getConsumerQueue(),
                    message.getMessageProperties().getHeaders().get("__TypeId__"),
                    rootCauseMessage(cause));
            delegate.recover(message, cause);
        };
    }

    private static String dlqName(String queueName) {
        return queueName + ".dlq";
    }

    private static String rootCauseMessage(Throwable cause) {
        Throwable root = cause;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
