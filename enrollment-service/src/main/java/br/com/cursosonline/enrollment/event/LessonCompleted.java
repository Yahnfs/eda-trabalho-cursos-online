package br.com.cursosonline.enrollment.event;

import java.time.Instant;

/** Aula concluída. Publicado pelo Progress Service no topic exchange (routing key {@code progress.lesson.completed}). */
public record LessonCompleted(
        String eventId,
        Instant occurredAt,
        Long enrollmentId,
        String studentId,
        String courseId,
        int lessonNumber,
        int completedLessons,
        int totalLessons,
        int progressPercentage) implements DomainEvent {
}
