package br.com.cursosonline.enrollment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.enrollment.config.RabbitTopologyProperties;
import br.com.cursosonline.enrollment.domain.Course;
import br.com.cursosonline.enrollment.domain.CourseRepository;
import br.com.cursosonline.enrollment.domain.Enrollment;
import br.com.cursosonline.enrollment.domain.EnrollmentRepository;
import br.com.cursosonline.enrollment.domain.EnrollmentStatus;
import br.com.cursosonline.enrollment.event.EnrollmentCancelled;
import br.com.cursosonline.enrollment.event.EnrollmentCreated;
import br.com.cursosonline.enrollment.exception.BusinessRuleException;
import br.com.cursosonline.enrollment.exception.ResourceNotFoundException;
import br.com.cursosonline.enrollment.messaging.EventPublisher;

/**
 * Casos de uso de matrícula (lado produtor do fluxo).
 * <p>
 * A matrícula é gravada e o evento publicado dentro da mesma transação: se o broker estiver
 * indisponível a publicação lança exceção, a transação sofre rollback e o cliente recebe erro,
 * evitando uma matrícula gravada sem o evento correspondente.
 */
@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final EventPublisher publisher;
    private final RabbitTopologyProperties topology;

    public EnrollmentService(EnrollmentRepository enrollments, CourseRepository courses,
                             EventPublisher publisher, RabbitTopologyProperties topology) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.publisher = publisher;
        this.topology = topology;
    }

    @Transactional
    public Enrollment enroll(String studentId, String studentName, String studentEmail, String courseId) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso não encontrado: " + courseId));

        if (enrollments.existsByStudentIdAndCourseIdAndStatusNot(studentId, courseId, EnrollmentStatus.CANCELLED)) {
            throw new BusinessRuleException(
                    "Aluno %s já possui matrícula ativa ou concluída no curso %s".formatted(studentId, courseId));
        }

        Enrollment enrollment = enrollments.save(new Enrollment(studentId, studentName, studentEmail, course));
        log.info("[ENROLLMENT] Matrícula id={} criada: aluno={} curso={} ({} aulas)",
                enrollment.getId(), studentId, courseId, course.getTotalLessons());

        publisher.publish(topology.eventsExchange(), topology.routingKeys().enrollmentCreated(),
                new EnrollmentCreated(newEventId(), Instant.now(), enrollment.getId(),
                        enrollment.getStudentId(), enrollment.getStudentName(), enrollment.getStudentEmail(),
                        enrollment.getCourseId(), enrollment.getCourseName(), enrollment.getTotalLessons()));
        return enrollment;
    }

    @Transactional
    public Enrollment cancel(Long enrollmentId, String reason) {
        Enrollment enrollment = findById(enrollmentId);
        if (!enrollment.isActive()) {
            throw new BusinessRuleException("Somente matrículas ACTIVE podem ser canceladas (status atual: %s)"
                    .formatted(enrollment.getStatus()));
        }
        String finalReason = (reason == null || reason.isBlank()) ? "Cancelada a pedido do aluno" : reason;
        enrollment.cancel(finalReason);
        log.info("[ENROLLMENT] Matrícula id={} cancelada. Motivo: {}", enrollmentId, finalReason);

        publisher.publish(topology.eventsExchange(), topology.routingKeys().enrollmentCancelled(),
                new EnrollmentCancelled(newEventId(), Instant.now(), enrollment.getId(),
                        enrollment.getStudentId(), enrollment.getStudentName(), enrollment.getStudentEmail(),
                        enrollment.getCourseId(), enrollment.getCourseName(), finalReason));
        return enrollment;
    }

    @Transactional(readOnly = true)
    public Enrollment findById(Long id) {
        return enrollments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Enrollment> list(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return enrollments.findAll();
        }
        return enrollments.findByStudentIdOrderByIdDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<Course> listCourses() {
        return courses.findAll();
    }

    private static String newEventId() {
        return UUID.randomUUID().toString();
    }
}
