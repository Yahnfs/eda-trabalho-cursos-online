package br.com.cursosonline.notification.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.cursosonline.notification.event.CourseCompleted;
import br.com.cursosonline.notification.event.EnrollmentCancelled;
import br.com.cursosonline.notification.event.EnrollmentCreated;
import br.com.cursosonline.notification.service.NotificationService;

/**
 * Consome a fila ligada ao topic exchange pelos bindings {@code enrollment.*} e
 * {@code progress.course.completed}.
 */
@Component
@RabbitListener(queues = "${app.rabbitmq.queues.course-events}")
public class CourseEventsListener {

    private static final Logger log = LoggerFactory.getLogger(CourseEventsListener.class);

    private final NotificationService service;

    public CourseEventsListener(NotificationService service) {
        this.service = service;
    }

    @RabbitHandler
    public void on(EnrollmentCreated event) {
        log.info("[CONSUME] EnrollmentCreated eventId={} matrícula={}", event.eventId(), event.enrollmentId());
        service.onEnrollmentCreated(event);
    }

    @RabbitHandler
    public void on(EnrollmentCancelled event) {
        log.info("[CONSUME] EnrollmentCancelled eventId={} matrícula={}", event.eventId(), event.enrollmentId());
        service.onEnrollmentCancelled(event);
    }

    @RabbitHandler
    public void on(CourseCompleted event) {
        log.info("[CONSUME] CourseCompleted eventId={} matrícula={}", event.eventId(), event.enrollmentId());
        service.onCourseCompleted(event);
    }
}
