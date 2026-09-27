package br.com.cursosonline.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.cursosonline.notification.exception.NotificationDeliveryException;

/**
 * Gateway de e-mail SIMULADO: apenas registra o envio no log.
 * <p>
 * Para demonstrar retry com backoff e DLQ, destinatários do domínio configurado em
 * {@code app.notification.simulated-failure-domain} (padrão {@code falha.com}) sempre falham,
 * como se o provedor de e-mail estivesse fora do ar.
 */
@Component
public class EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(EmailGateway.class);

    private final String failureDomain;

    public EmailGateway(@Value("${app.notification.simulated-failure-domain:falha.com}") String failureDomain) {
        this.failureDomain = failureDomain;
    }

    public void send(String recipient, String subject, String body) {
        if (recipient != null && recipient.toLowerCase().endsWith("@" + failureDomain)) {
            log.error("[EMAIL] Falha simulada ao enviar e-mail para {}", recipient);
            throw new NotificationDeliveryException("Provedor de e-mail indisponível para " + recipient);
        }
        log.info("[EMAIL] Para: {} | Assunto: {} | Mensagem: {}", recipient, subject, body);
    }
}
