package br.com.cursosonline.enrollment.web;

import java.time.Instant;

import br.com.cursosonline.enrollment.domain.Course;
import br.com.cursosonline.enrollment.domain.Enrollment;
import br.com.cursosonline.enrollment.domain.EnrollmentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs da API REST do Enrollment Service. */
public final class EnrollmentDtos {

    private EnrollmentDtos() {
    }

    public record CreateEnrollmentRequest(
            @NotBlank @Size(max = 50) String studentId,
            @NotBlank @Size(max = 120) String studentName,
            @NotBlank @Email String studentEmail,
            @NotBlank String courseId) {
    }

    public record CancelEnrollmentRequest(@Size(max = 255) String reason) {
    }

    public record EnrollmentResponse(
            Long id,
            String studentId,
            String studentName,
            String studentEmail,
            String courseId,
            String courseName,
            int totalLessons,
            EnrollmentStatus status,
            int completedLessons,
            int progressPercentage,
            String certificateCode,
            String cancellationReason,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt) {

        public static EnrollmentResponse from(Enrollment e) {
            return new EnrollmentResponse(e.getId(), e.getStudentId(), e.getStudentName(), e.getStudentEmail(),
                    e.getCourseId(), e.getCourseName(), e.getTotalLessons(), e.getStatus(),
                    e.getCompletedLessons(), e.getProgressPercentage(), e.getCertificateCode(),
                    e.getCancellationReason(), e.getCreatedAt(), e.getUpdatedAt(), e.getCompletedAt());
        }
    }

    public record CourseResponse(String id, String name, int totalLessons, int workloadHours) {

        public static CourseResponse from(Course c) {
            return new CourseResponse(c.getId(), c.getName(), c.getTotalLessons(), c.getWorkloadHours());
        }
    }
}
