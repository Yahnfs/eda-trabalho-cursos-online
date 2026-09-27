package br.com.cursosonline.certificate.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cursosonline.certificate.config.RabbitTopologyProperties;
import br.com.cursosonline.certificate.domain.Certificate;
import br.com.cursosonline.certificate.domain.CertificateRepository;
import br.com.cursosonline.certificate.event.CertificateIssued;
import br.com.cursosonline.certificate.event.CourseCompleted;
import br.com.cursosonline.certificate.exception.ResourceNotFoundException;
import br.com.cursosonline.certificate.idempotency.IdempotencyGuard;
import br.com.cursosonline.certificate.messaging.EventPublisher;

/**
 * Emite certificados ao receber {@code CourseCompleted} e anuncia a emissão com
 * {@code CertificateIssued} no fanout exchange (todos os interessados recebem uma cópia).
 */
@Service
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificates;
    private final IdempotencyGuard idempotency;
    private final EventPublisher publisher;
    private final RabbitTopologyProperties topology;

    public CertificateService(CertificateRepository certificates, IdempotencyGuard idempotency,
                              EventPublisher publisher, RabbitTopologyProperties topology) {
        this.certificates = certificates;
        this.idempotency = idempotency;
        this.publisher = publisher;
        this.topology = topology;
    }

    /**
     * Idempotência em dois níveis: pelo eventId (mesma mensagem reentregue) e pela regra de negócio
     * "um certificado por matrícula" (eventos distintos referentes à mesma conclusão).
     */
    @Transactional
    public Optional<Certificate> issueFor(CourseCompleted event) {
        if (!idempotency.firstTime(event)) {
            return Optional.empty();
        }
        Optional<Certificate> existing = certificates.findByEnrollmentId(event.enrollmentId());
        if (existing.isPresent()) {
            log.warn("[CERTIFICATE] Matrícula {} já possui o certificado {} - nenhuma nova emissão",
                    event.enrollmentId(), existing.get().getCode());
            return Optional.empty();
        }

        Certificate certificate = certificates.save(new Certificate(event.enrollmentId(), event.studentId(),
                event.studentName(), event.studentEmail(), event.courseId(), event.courseName(),
                event.totalLessons(), event.completedAt()));
        log.info("[CERTIFICATE] Certificado {} emitido para {} no curso '{}' (matrícula {})",
                certificate.getCode(), certificate.getStudentName(), certificate.getCourseName(), event.enrollmentId());

        // Fanout: a routing key é ignorada; o evento é copiado para todas as filas ligadas ao exchange.
        publisher.publish(topology.certificatesExchange(), "",
                new CertificateIssued(UUID.randomUUID().toString(), Instant.now(), certificate.getId(),
                        certificate.getCode(), certificate.getEnrollmentId(), certificate.getStudentId(),
                        certificate.getStudentName(), certificate.getStudentEmail(), certificate.getCourseId(),
                        certificate.getCourseName(), certificate.getIssuedAt()));
        return Optional.of(certificate);
    }

    @Transactional(readOnly = true)
    public List<Certificate> list(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return certificates.findAll();
        }
        return certificates.findByStudentIdOrderByIdDesc(studentId);
    }

    @Transactional(readOnly = true)
    public Certificate findByCode(String code) {
        return certificates.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado: " + code));
    }

    @Transactional(readOnly = true)
    public Certificate findByEnrollment(Long enrollmentId) {
        return certificates.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum certificado para a matrícula " + enrollmentId));
    }
}
