package br.com.cursosonline.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.cursosonline.enrollment.config.RabbitTopologyProperties;
import br.com.cursosonline.enrollment.domain.Course;
import br.com.cursosonline.enrollment.domain.CourseRepository;
import br.com.cursosonline.enrollment.domain.Enrollment;
import br.com.cursosonline.enrollment.domain.EnrollmentRepository;
import br.com.cursosonline.enrollment.domain.EnrollmentStatus;
import br.com.cursosonline.enrollment.event.DomainEvent;
import br.com.cursosonline.enrollment.event.EnrollmentCancelled;
import br.com.cursosonline.enrollment.event.EnrollmentCreated;
import br.com.cursosonline.enrollment.exception.BusinessRuleException;
import br.com.cursosonline.enrollment.exception.ResourceNotFoundException;
import br.com.cursosonline.enrollment.messaging.EventPublisher;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Course COURSE = new Course("EDA-101", "EDA com RabbitMQ", 3, 12);

    @Mock
    private EnrollmentRepository enrollments;
    @Mock
    private CourseRepository courses;
    @Mock
    private EventPublisher publisher;

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        RabbitTopologyProperties topology = new RabbitTopologyProperties("courses.events", "certificates.fanout",
                "courses.dlx",
                new RabbitTopologyProperties.RoutingKeys("enrollment.created", "enrollment.cancelled", "progress.#"),
                new RabbitTopologyProperties.Queues("q1", "q2"));
        service = new EnrollmentService(enrollments, courses, publisher, topology);
    }

    @Test
    void enrollSavesEnrollmentAndPublishesEnrollmentCreated() {
        when(courses.findById("EDA-101")).thenReturn(Optional.of(COURSE));
        when(enrollments.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

        Enrollment result = service.enroll("aluno-1", "Maria", "maria@email.com", "EDA-101");

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher).publish(eq("courses.events"), eq("enrollment.created"), captor.capture());
        EnrollmentCreated event = (EnrollmentCreated) captor.getValue();
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.enrollmentId()).isEqualTo(10L);
        assertThat(event.courseId()).isEqualTo("EDA-101");
        assertThat(event.totalLessons()).isEqualTo(3);
        assertThat(event.studentEmail()).isEqualTo("maria@email.com");
    }

    @Test
    void enrollRejectsDuplicateActiveEnrollment() {
        when(courses.findById("EDA-101")).thenReturn(Optional.of(COURSE));
        when(enrollments.existsByStudentIdAndCourseIdAndStatusNot("aluno-1", "EDA-101", EnrollmentStatus.CANCELLED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.enroll("aluno-1", "Maria", "maria@email.com", "EDA-101"))
                .isInstanceOf(BusinessRuleException.class);
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void enrollFailsForUnknownCourse() {
        when(courses.findById("XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll("aluno-1", "Maria", "maria@email.com", "XX"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(enrollments, never()).save(any());
    }

    @Test
    void cancelActiveEnrollmentPublishesEnrollmentCancelled() {
        Enrollment enrollment = withId(new Enrollment("aluno-1", "Maria", "maria@email.com", COURSE), 5L);
        when(enrollments.findById(5L)).thenReturn(Optional.of(enrollment));

        service.cancel(5L, "Sem tempo");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher).publish(eq("courses.events"), eq("enrollment.cancelled"), captor.capture());
        assertThat(((EnrollmentCancelled) captor.getValue()).reason()).isEqualTo("Sem tempo");
    }

    @Test
    void cancelRejectsNonActiveEnrollment() {
        Enrollment enrollment = withId(new Enrollment("aluno-1", "Maria", "maria@email.com", COURSE), 5L);
        enrollment.markCompleted(null);
        when(enrollments.findById(5L)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.cancel(5L, null)).isInstanceOf(BusinessRuleException.class);
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    private static Enrollment withId(Enrollment enrollment, Long id) {
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }
}
