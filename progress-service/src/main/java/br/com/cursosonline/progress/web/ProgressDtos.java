package br.com.cursosonline.progress.web;

import java.time.Instant;

import br.com.cursosonline.progress.domain.CourseProgress;
import br.com.cursosonline.progress.domain.LessonCompletion;
import br.com.cursosonline.progress.domain.ProgressStatus;

/** DTOs da API REST do Progress Service. */
public final class ProgressDtos {

    private ProgressDtos() {
    }

    public record ProgressResponse(
            Long enrollmentId,
            String studentId,
            String studentName,
            String courseId,
            String courseName,
            int totalLessons,
            int completedLessons,
            int progressPercentage,
            ProgressStatus status,
            Instant startedAt,
            Instant updatedAt,
            Instant completedAt) {

        public static ProgressResponse from(CourseProgress p) {
            return new ProgressResponse(p.getEnrollmentId(), p.getStudentId(), p.getStudentName(), p.getCourseId(),
                    p.getCourseName(), p.getTotalLessons(), p.getCompletedLessons(), p.getProgressPercentage(),
                    p.getStatus(), p.getStartedAt(), p.getUpdatedAt(), p.getCompletedAt());
        }
    }

    public record LessonResponse(int lessonNumber, Instant completedAt) {

        public static LessonResponse from(LessonCompletion l) {
            return new LessonResponse(l.getLessonNumber(), l.getCompletedAt());
        }
    }
}
