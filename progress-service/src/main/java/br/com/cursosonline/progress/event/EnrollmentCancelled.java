package br.com.cursosonline.progress.event;

import java.time.Instant;

/** Matrícula ativa cancelada. Publicado pelo Enrollment Service no topic exchange (routing key {@code enrollment.cancelled}). */
public record EnrollmentCancelled(
        String eventId,
        Instant occurredAt,
        Long enrollmentId,
        String studentId,
        String studentName,
        String studentEmail,
        String courseId,
        String courseName,
        String reason) implements DomainEvent {
}
