package br.com.cursosonline.enrollment.event;

import java.time.Instant;

/** Certificado emitido. Publicado pelo Certificate Service no fanout exchange {@code certificates.fanout}. */
public record CertificateIssued(
        String eventId,
        Instant occurredAt,
        Long certificateId,
        String certificateCode,
        Long enrollmentId,
        String studentId,
        String studentName,
        String studentEmail,
        String courseId,
        String courseName,
        Instant issuedAt) implements DomainEvent {
}
