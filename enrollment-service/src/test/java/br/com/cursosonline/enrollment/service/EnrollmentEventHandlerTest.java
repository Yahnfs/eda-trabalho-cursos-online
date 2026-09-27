package br.com.cursosonline.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.cursosonline.enrollment.domain.Course;
import br.com.cursosonline.enrollment.domain.Enrollment;
import br.com.cursosonline.enrollment.domain.EnrollmentRepository;
import br.com.cursosonline.enrollment.domain.EnrollmentStatus;
import br.com.cursosonline.enrollment.event.CertificateIssued;
import br.com.cursosonline.enrollment.event.CourseCompleted;
import br.com.cursosonline.enrollment.event.LessonCompleted;
import br.com.cursosonline.enrollment.idempotency.IdempotencyGuard;

@ExtendWith(MockitoExtension.class)
class EnrollmentEventHandlerTest {

    @Mock
    private EnrollmentRepository enrollments;
    @Mock
    private IdempotencyGuard idempotency;

    private EnrollmentEventHandler handler;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        handler = new EnrollmentEventHandler(enrollments, idempotency);
        enrollment = new Enrollment("aluno-1", "Maria", "maria@email.com", new Course("EDA-101", "EDA", 3, 12));
    }

    @Test
    void duplicateEventIsIgnored() {
        when(idempotency.firstTime(any())).thenReturn(false);

        handler.onLessonCompleted(lesson("e1", 1, 33));

        verify(enrollments, never()).findById(anyLong());
    }

    @Test
    void lessonCompletedUpdatesProgress() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(enrollments.findById(1L)).thenReturn(Optional.of(enrollment));

        handler.onLessonCompleted(lesson("e1", 2, 66));

        assertThat(enrollment.getCompletedLessons()).isEqualTo(2);
        assertThat(enrollment.getProgressPercentage()).isEqualTo(66);
    }

    @Test
    void outOfOrderLessonEventDoesNotRegressProgress() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(enrollments.findById(1L)).thenReturn(Optional.of(enrollment));

        handler.onLessonCompleted(lesson("e2", 2, 66));
        handler.onLessonCompleted(lesson("e1", 1, 33));

        assertThat(enrollment.getCompletedLessons()).isEqualTo(2);
    }

    @Test
    void certificateIssuedMarksEnrollmentAsCertified() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(enrollments.findById(1L)).thenReturn(Optional.of(enrollment));

        handler.onCertificateIssued(new CertificateIssued("e3", Instant.now(), 9L, "CERT-123", 1L, "aluno-1",
                "Maria", "maria@email.com", "EDA-101", "EDA", Instant.now()));

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CERTIFIED);
        assertThat(enrollment.getCertificateCode()).isEqualTo("CERT-123");
    }

    @Test
    void lateCourseCompletedDoesNotDowngradeCertifiedEnrollment() {
        enrollment.markCertified("CERT-123");
        when(idempotency.firstTime(any())).thenReturn(true);
        when(enrollments.findById(1L)).thenReturn(Optional.of(enrollment));

        handler.onCourseCompleted(new CourseCompleted("e4", Instant.now(), 1L, "aluno-1", "Maria",
                "maria@email.com", "EDA-101", "EDA", 3, Instant.now()));

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CERTIFIED);
    }

    private static LessonCompleted lesson(String eventId, int completed, int percentage) {
        return new LessonCompleted(eventId, Instant.now(), 1L, "aluno-1", "EDA-101", completed, completed, 3, percentage);
    }
}
