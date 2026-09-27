package br.com.cursosonline.enrollment.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.cursosonline.enrollment.event.CertificateIssued;
import br.com.cursosonline.enrollment.service.EnrollmentEventHandler;

/** Consome a fila ligada ao fanout {@code certificates.fanout}. */
@Component
public class CertificateIssuedListener {

    private static final Logger log = LoggerFactory.getLogger(CertificateIssuedListener.class);

    private final EnrollmentEventHandler handler;

    public CertificateIssuedListener(EnrollmentEventHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.certificate-issued}")
    public void on(CertificateIssued event) {
        log.info("[CONSUME] CertificateIssued eventId={} matrícula={} certificado={}",
                event.eventId(), event.enrollmentId(), event.certificateCode());
        handler.onCertificateIssued(event);
    }
}
