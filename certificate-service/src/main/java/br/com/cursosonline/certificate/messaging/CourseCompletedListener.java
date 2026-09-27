package br.com.cursosonline.certificate.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.cursosonline.certificate.event.CourseCompleted;
import br.com.cursosonline.certificate.service.CertificateService;

/** Consome a fila ligada ao topic exchange com a routing key {@code progress.course.completed}. */
@Component
public class CourseCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(CourseCompletedListener.class);

    private final CertificateService service;

    public CourseCompletedListener(CertificateService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.course-completed}")
    public void on(CourseCompleted event) {
        log.info("[CONSUME] CourseCompleted eventId={} matrícula={} aluno={} curso={}",
                event.eventId(), event.enrollmentId(), event.studentId(), event.courseId());
        service.issueFor(event);
    }
}
