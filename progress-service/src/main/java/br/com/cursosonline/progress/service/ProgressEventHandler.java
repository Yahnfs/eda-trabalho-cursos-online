package br.com.cursosonline.progress.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.progress.domain.CourseProgress;
import br.com.cursosonline.progress.domain.CourseProgressRepository;
import br.com.cursosonline.progress.domain.ProgressStatus;
import br.com.cursosonline.progress.event.EnrollmentCancelled;
import br.com.cursosonline.progress.event.EnrollmentCreated;
import br.com.cursosonline.progress.exception.InvalidEventException;
import br.com.cursosonline.progress.idempotency.IdempotencyGuard;

/** Reage aos eventos de matrícula criando/cancelando o acompanhamento de progresso. */
@Service
public class ProgressEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ProgressEventHandler.class);

    private final CourseProgressRepository progressRepository;
    private final IdempotencyGuard idempotency;

    public ProgressEventHandler(CourseProgressRepository progressRepository, IdempotencyGuard idempotency) {
        this.progressRepository = progressRepository;
        this.idempotency = idempotency;
    }

    @Transactional
    public void onEnrollmentCreated(EnrollmentCreated event) {
        if (event.enrollmentId() == null || event.totalLessons() <= 0) {
            // Mensagem inválida: falha -> retentativas -> DLQ para análise manual.
            throw new InvalidEventException("EnrollmentCreated inválido (enrollmentId/totalLessons): " + event);
        }
        if (!idempotency.firstTime(event)) {
            return;
        }
        if (progressRepository.existsByEnrollmentId(event.enrollmentId())) {
            log.warn("[PROGRESS] Já existe progresso para a matrícula {} - EnrollmentCreated ignorado", event.enrollmentId());
            return;
        }
        CourseProgress progress = progressRepository.save(new CourseProgress(event.enrollmentId(), event.studentId(),
                event.studentName(), event.studentEmail(), event.courseId(), event.courseName(), event.totalLessons()));
        log.info("[PROGRESS] Acompanhamento id={} criado para matrícula {} (curso {}, {} aulas)",
                progress.getId(), event.enrollmentId(), event.courseId(), event.totalLessons());
    }

    @Transactional
    public void onEnrollmentCancelled(EnrollmentCancelled event) {
        if (!idempotency.firstTime(event)) {
            return;
        }
        progressRepository.findByEnrollmentId(event.enrollmentId()).ifPresentOrElse(progress -> {
            if (progress.getStatus() == ProgressStatus.IN_PROGRESS) {
                progress.cancel();
                log.info("[PROGRESS] Acompanhamento da matrícula {} CANCELADO ({})", event.enrollmentId(), event.reason());
            } else {
                log.info("[PROGRESS] Matrícula {} já está {} - cancelamento sem efeito",
                        event.enrollmentId(), progress.getStatus());
            }
        }, () -> log.warn("[PROGRESS] Cancelamento para matrícula desconhecida {} - ignorado", event.enrollmentId()));
    }
}
