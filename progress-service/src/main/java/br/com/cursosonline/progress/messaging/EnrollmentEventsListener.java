package br.com.cursosonline.progress.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.cursosonline.progress.event.EnrollmentCancelled;
import br.com.cursosonline.progress.event.EnrollmentCreated;
import br.com.cursosonline.progress.service.ProgressEventHandler;

/**
 * Consome a fila ligada ao topic exchange com o padrão {@code enrollment.*}
 * (EnrollmentCreated e EnrollmentCancelled).
 */
@Component
@RabbitListener(queues = "${app.rabbitmq.queues.enrollment-events}")
public class EnrollmentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentEventsListener.class);

    private final ProgressEventHandler handler;

    public EnrollmentEventsListener(ProgressEventHandler handler) {
        this.handler = handler;
    }

    @RabbitHandler
    public void on(EnrollmentCreated event) {
        log.info("[CONSUME] EnrollmentCreated eventId={} matrícula={} curso={}",
                event.eventId(), event.enrollmentId(), event.courseId());
        handler.onEnrollmentCreated(event);
    }

    @RabbitHandler
    public void on(EnrollmentCancelled event) {
        log.info("[CONSUME] EnrollmentCancelled eventId={} matrícula={}", event.eventId(), event.enrollmentId());
        handler.onEnrollmentCancelled(event);
    }
}
