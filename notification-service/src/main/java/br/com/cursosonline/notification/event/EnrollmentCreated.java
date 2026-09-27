package br.com.cursosonline.notification.event;

import java.time.Instant;

/** Aluno matriculado em um curso. Publicado pelo Enrollment Service no topic exchange (routing key {@code enrollment.created}). */
public record EnrollmentCreated(
        String eventId,
        Instant occurredAt,
        Long enrollmentId,
        String studentId,
        String studentName,
        String studentEmail,
        String courseId,
        String courseName,
        int totalLessons) implements DomainEvent {
}
