package br.com.cursosonline.certificate.event;

import java.time.Instant;

/** Todas as aulas concluídas. Publicado pelo Progress Service no topic exchange (routing key {@code progress.course.completed}). */
public record CourseCompleted(
        String eventId,
        Instant occurredAt,
        Long enrollmentId,
        String studentId,
        String studentName,
        String studentEmail,
        String courseId,
        String courseName,
        int totalLessons,
        Instant completedAt) implements DomainEvent {
}
