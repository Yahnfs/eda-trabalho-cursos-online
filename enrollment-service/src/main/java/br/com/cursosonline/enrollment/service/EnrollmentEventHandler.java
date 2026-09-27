package br.com.cursosonline.enrollment.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.enrollment.domain.Enrollment;
import br.com.cursosonline.enrollment.domain.EnrollmentRepository;
import br.com.cursosonline.enrollment.domain.EnrollmentStatus;
import br.com.cursosonline.enrollment.event.CertificateIssued;
import br.com.cursosonline.enrollment.event.CourseCompleted;
import br.com.cursosonline.enrollment.event.LessonCompleted;
import br.com.cursosonline.enrollment.idempotency.IdempotencyGuard;

/**
 * Aplica, na visão local da matrícula, os eventos publicados pelos demais serviços.
 * Cada método é transacional e idempotente.
 */
@Service
public class EnrollmentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentEventHandler.class);

    private final EnrollmentRepository enrollments;
    private final IdempotencyGuard idempotency;

    public EnrollmentEventHandler(EnrollmentRepository enrollments, IdempotencyGuard idempotency) {
        this.enrollments = enrollments;
        this.idempotency = idempotency;
    }

    @Transactional
    public void onLessonCompleted(LessonCompleted event) {
        if (!idempotency.firstTime(event)) {
            return;
        }
        findEnrollment(event.enrollmentId()).ifPresent(enrollment -> {
            if (!enrollment.isActive()) {
                log.info("[ENROLLMENT] Matrícula id={} está {}: progresso da aula {} ignorado",
                        enrollment.getId(), enrollment.getStatus(), event.lessonNumber());
                return;
            }
            if (enrollment.registerProgress(event.completedLessons(), event.progressPercentage())) {
                log.info("[ENROLLMENT] Matrícula id={} progresso atualizado: {}/{} aulas ({}%)",
                        enrollment.getId(), event.completedLessons(), event.totalLessons(), event.progressPercentage());
            } else {
                log.info("[ENROLLMENT] Matrícula id={} já possui progresso mais recente - evento fora de ordem ignorado",
                        enrollment.getId());
            }
        });
    }

    @Transactional
    public void onCourseCompleted(CourseCompleted event) {
        if (!idempotency.firstTime(event)) {
            return;
        }
        findEnrollment(event.enrollmentId()).ifPresent(enrollment -> {
            // Se o certificado chegou antes (filas distintas não garantem ordem), não rebaixa o status.
            if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                enrollment.markCompleted(event.completedAt());
                log.info("[ENROLLMENT] Matrícula id={} marcada como COMPLETED", enrollment.getId());
            } else {
                log.info("[ENROLLMENT] Matrícula id={} já está {} - CourseCompleted sem efeito",
                        enrollment.getId(), enrollment.getStatus());
            }
        });
    }

    @Transactional
    public void onCertificateIssued(CertificateIssued event) {
        if (!idempotency.firstTime(event)) {
            return;
        }
        findEnrollment(event.enrollmentId()).ifPresent(enrollment -> {
            if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
                log.warn("[ENROLLMENT] Certificado {} recebido para matrícula cancelada id={}",
                        event.certificateCode(), enrollment.getId());
                return;
            }
            enrollment.markCertified(event.certificateCode());
            log.info("[ENROLLMENT] Matrícula id={} marcada como CERTIFIED (certificado {})",
                    enrollment.getId(), event.certificateCode());
        });
    }

    private Optional<Enrollment> findEnrollment(Long enrollmentId) {
        Optional<Enrollment> enrollment = enrollments.findById(enrollmentId);
        if (enrollment.isEmpty()) {
            log.warn("[ENROLLMENT] Evento referencia matrícula inexistente id={} - ignorado", enrollmentId);
        }
        return enrollment;
    }
}
