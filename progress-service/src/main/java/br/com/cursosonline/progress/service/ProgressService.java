package br.com.cursosonline.progress.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.progress.config.RabbitTopologyProperties;
import br.com.cursosonline.progress.domain.CourseProgress;
import br.com.cursosonline.progress.domain.CourseProgressRepository;
import br.com.cursosonline.progress.domain.LessonCompletion;
import br.com.cursosonline.progress.domain.LessonCompletionRepository;
import br.com.cursosonline.progress.domain.ProgressStatus;
import br.com.cursosonline.progress.event.CourseCompleted;
import br.com.cursosonline.progress.event.LessonCompleted;
import br.com.cursosonline.progress.exception.BusinessRuleException;
import br.com.cursosonline.progress.exception.ResourceNotFoundException;
import br.com.cursosonline.progress.messaging.EventPublisher;

/**
 * Registra a conclusão de aulas. Cada aula concluída gera {@code LessonCompleted}; a última aula
 * também gera {@code CourseCompleted}, que dispara a emissão do certificado.
 */
@Service
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final CourseProgressRepository progressRepository;
    private final LessonCompletionRepository lessonRepository;
    private final EventPublisher publisher;
    private final RabbitTopologyProperties topology;

    public ProgressService(CourseProgressRepository progressRepository, LessonCompletionRepository lessonRepository,
                           EventPublisher publisher, RabbitTopologyProperties topology) {
        this.progressRepository = progressRepository;
        this.lessonRepository = lessonRepository;
        this.publisher = publisher;
        this.topology = topology;
    }

    @Transactional
    public CourseProgress completeLesson(Long enrollmentId, int lessonNumber) {
        CourseProgress progress = findByEnrollment(enrollmentId);

        if (progress.getStatus() != ProgressStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Matrícula %d está %s: não é possível concluir aulas"
                    .formatted(enrollmentId, progress.getStatus()));
        }
        if (lessonNumber < 1 || lessonNumber > progress.getTotalLessons()) {
            throw new IllegalArgumentException("Aula %d inválida: o curso %s possui aulas de 1 a %d"
                    .formatted(lessonNumber, progress.getCourseId(), progress.getTotalLessons()));
        }
        if (lessonRepository.existsByEnrollmentIdAndLessonNumber(enrollmentId, lessonNumber)) {
            throw new BusinessRuleException("Aula %d já foi concluída na matrícula %d".formatted(lessonNumber, enrollmentId));
        }

        lessonRepository.save(new LessonCompletion(enrollmentId, lessonNumber));
        progress.registerLessonCompleted();
        log.info("[PROGRESS] Matrícula {} concluiu a aula {} -> {}/{} aulas ({}%)", enrollmentId, lessonNumber,
                progress.getCompletedLessons(), progress.getTotalLessons(), progress.getProgressPercentage());

        publisher.publish(topology.eventsExchange(), topology.routingKeys().lessonCompleted(),
                new LessonCompleted(newEventId(), Instant.now(), enrollmentId, progress.getStudentId(),
                        progress.getCourseId(), lessonNumber, progress.getCompletedLessons(),
                        progress.getTotalLessons(), progress.getProgressPercentage()));

        if (progress.isCompleted()) {
            log.info("[PROGRESS] Matrícula {} concluiu TODAS as aulas do curso {}", enrollmentId, progress.getCourseId());
            publisher.publish(topology.eventsExchange(), topology.routingKeys().courseCompleted(),
                    new CourseCompleted(newEventId(), Instant.now(), enrollmentId, progress.getStudentId(),
                            progress.getStudentName(), progress.getStudentEmail(), progress.getCourseId(),
                            progress.getCourseName(), progress.getTotalLessons(), progress.getCompletedAt()));
        }
        return progress;
    }

    @Transactional(readOnly = true)
    public CourseProgress findByEnrollment(Long enrollmentId) {
        return progressRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Progresso não encontrado para a matrícula " + enrollmentId
                                + " (o evento EnrollmentCreated pode ainda não ter sido processado)"));
    }

    @Transactional(readOnly = true)
    public List<CourseProgress> list(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return progressRepository.findAll();
        }
        return progressRepository.findByStudentIdOrderByIdDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<LessonCompletion> lessons(Long enrollmentId) {
        findByEnrollment(enrollmentId);
        return lessonRepository.findByEnrollmentIdOrderByLessonNumber(enrollmentId);
    }

    private static String newEventId() {
        return UUID.randomUUID().toString();
    }
}
