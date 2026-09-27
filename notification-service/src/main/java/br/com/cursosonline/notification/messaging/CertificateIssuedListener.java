package br.com.cursosonline.notification.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.cursosonline.notification.event.CertificateIssued;
import br.com.cursosonline.notification.service.NotificationService;

/** Consome a fila ligada ao fanout {@code certificates.fanout}. */
@Component
public class CertificateIssuedListener {

    private static final Logger log = LoggerFactory.getLogger(CertificateIssuedListener.class);

    private final NotificationService service;

    public CertificateIssuedListener(NotificationService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.certificate-issued}")
    public void on(CertificateIssued event) {
        log.info("[CONSUME] CertificateIssued eventId={} certificado={}", event.eventId(), event.certificateCode());
        service.onCertificateIssued(event);
    }
}
