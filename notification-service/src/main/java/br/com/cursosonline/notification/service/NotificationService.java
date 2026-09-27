package br.com.cursosonline.notification.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.notification.domain.Notification;
import br.com.cursosonline.notification.domain.NotificationRepository;
import br.com.cursosonline.notification.event.CertificateIssued;
import br.com.cursosonline.notification.event.CourseCompleted;
import br.com.cursosonline.notification.event.DomainEvent;
import br.com.cursosonline.notification.event.EnrollmentCancelled;
import br.com.cursosonline.notification.event.EnrollmentCreated;
import br.com.cursosonline.notification.idempotency.IdempotencyGuard;

/** Transforma eventos de domínio em notificações para o aluno. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final IdempotencyGuard idempotency;
    private final EmailGateway emailGateway;

    public NotificationService(NotificationRepository notifications, IdempotencyGuard idempotency,
                               EmailGateway emailGateway) {
        this.notifications = notifications;
        this.idempotency = idempotency;
        this.emailGateway = emailGateway;
    }

    @Transactional
    public void onEnrollmentCreated(EnrollmentCreated e) {
        notify(e, e.enrollmentId(), e.studentEmail(), "Matrícula confirmada: " + e.courseName(),
                "Olá, %s! Sua matrícula (nº %d) no curso '%s' foi confirmada. São %d aulas. Bons estudos!"
                        .formatted(e.studentName(), e.enrollmentId(), e.courseName(), e.totalLessons()));
    }

    @Transactional
    public void onEnrollmentCancelled(EnrollmentCancelled e) {
        notify(e, e.enrollmentId(), e.studentEmail(), "Matrícula cancelada: " + e.courseName(),
                "Olá, %s. Sua matrícula (nº %d) no curso '%s' foi cancelada. Motivo: %s"
                        .formatted(e.studentName(), e.enrollmentId(), e.courseName(), e.reason()));
    }

    @Transactional
    public void onCourseCompleted(CourseCompleted e) {
        notify(e, e.enrollmentId(), e.studentEmail(), "Parabéns! Você concluiu " + e.courseName(),
                "Olá, %s! Você concluiu as %d aulas do curso '%s'. Seu certificado está sendo emitido."
                        .formatted(e.studentName(), e.totalLessons(), e.courseName()));
    }

    @Transactional
    public void onCertificateIssued(CertificateIssued e) {
        notify(e, e.enrollmentId(), e.studentEmail(), "Seu certificado está disponível",
                "Olá, %s! O certificado %s do curso '%s' foi emitido. Consulte em /api/certificates/%s"
                        .formatted(e.studentName(), e.certificateCode(), e.courseName(), e.certificateCode()));
    }

    @Transactional(readOnly = true)
    public List<Notification> list(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return notifications.findAllByOrderByIdDesc();
        }
        return notifications.findByRecipientOrderByIdDesc(recipient);
    }

    /**
     * Envia e registra a notificação na mesma transação do controle de idempotência: se o envio
     * falhar, nada é gravado e a mensagem será reprocessada pelo mecanismo de retry.
     */
    private void notify(DomainEvent event, Long enrollmentId, String recipient, String subject, String message) {
        if (!idempotency.firstTime(event)) {
            return;
        }
        emailGateway.send(recipient, subject, message);
        Notification notification = notifications.save(new Notification(event.eventId(),
                event.getClass().getSimpleName(), enrollmentId, recipient, subject, message));
        log.info("[NOTIFICATION] Notificação id={} ({}) registrada para {}",
                notification.getId(), notification.getEventType(), recipient);
    }
}
