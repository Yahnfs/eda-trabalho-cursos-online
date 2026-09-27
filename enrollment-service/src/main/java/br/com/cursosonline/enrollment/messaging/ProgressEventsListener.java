package br.com.cursosonline.enrollment.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.cursosonline.enrollment.event.CourseCompleted;
import br.com.cursosonline.enrollment.event.LessonCompleted;
import br.com.cursosonline.enrollment.service.EnrollmentEventHandler;

/**
 * Consome a fila ligada ao topic exchange com o padrão {@code progress.#}. Uma única fila recebe
 * dois tipos de evento; o método é escolhido pelo tipo convertido a partir do header {@code __TypeId__}.
 */
@Component
@RabbitListener(queues = "${app.rabbitmq.queues.progress-events}")
public class ProgressEventsListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressEventsListener.class);

    private final EnrollmentEventHandler handler;

    public ProgressEventsListener(EnrollmentEventHandler handler) {
        this.handler = handler;
    }

    @RabbitHandler
    public void on(LessonCompleted event) {
        log.info("[CONSUME] LessonCompleted eventId={} matrícula={} aula={}",
                event.eventId(), event.enrollmentId(), event.lessonNumber());
        handler.onLessonCompleted(event);
    }

    @RabbitHandler
    public void on(CourseCompleted event) {
        log.info("[CONSUME] CourseCompleted eventId={} matrícula={}", event.eventId(), event.enrollmentId());
        handler.onCourseCompleted(event);
    }
}
