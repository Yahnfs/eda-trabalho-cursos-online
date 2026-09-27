package br.com.cursosonline.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.cursosonline.progress.config.RabbitTopologyProperties;
import br.com.cursosonline.progress.domain.CourseProgress;
import br.com.cursosonline.progress.domain.CourseProgressRepository;
import br.com.cursosonline.progress.domain.LessonCompletionRepository;
import br.com.cursosonline.progress.domain.ProgressStatus;
import br.com.cursosonline.progress.event.CourseCompleted;
import br.com.cursosonline.progress.event.DomainEvent;
import br.com.cursosonline.progress.event.LessonCompleted;
import br.com.cursosonline.progress.exception.BusinessRuleException;
import br.com.cursosonline.progress.exception.ResourceNotFoundException;
import br.com.cursosonline.progress.messaging.EventPublisher;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private CourseProgressRepository progressRepository;
    @Mock
    private LessonCompletionRepository lessonRepository;
    @Mock
    private EventPublisher publisher;

    private ProgressService service;
    private CourseProgress progress;

    @BeforeEach
    void setUp() {
        RabbitTopologyProperties topology = new RabbitTopologyProperties("courses.events", "courses.dlx",
                new RabbitTopologyProperties.RoutingKeys("progress.lesson.completed", "progress.course.completed",
                        "enrollment.*"),
                new RabbitTopologyProperties.Queues("q"));
        service = new ProgressService(progressRepository, lessonRepository, publisher, topology);
        progress = new CourseProgress(1L, "aluno-1", "Maria", "maria@email.com", "EDA-101", "EDA", 2);
    }

    @Test
    void completingALessonPublishesLessonCompleted() {
        when(progressRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(progress));

        service.completeLesson(1L, 1);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher).publish(eq("courses.events"), eq("progress.lesson.completed"), captor.capture());
        LessonCompleted event = (LessonCompleted) captor.getValue();
        assertThat(event.completedLessons()).isEqualTo(1);
        assertThat(event.progressPercentage()).isEqualTo(50);
        verify(publisher, never()).publish(anyString(), eq("progress.course.completed"), any());
        verify(lessonRepository).save(any());
    }

    @Test
    void completingTheLastLessonAlsoPublishesCourseCompleted() {
        when(progressRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(progress));

        service.completeLesson(1L, 1);
        service.completeLesson(1L, 2);

        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher, times(3)).publish(eq("courses.events"), anyString(), captor.capture());
        List<DomainEvent> events = captor.getAllValues();
        assertThat(events.get(2)).isInstanceOf(CourseCompleted.class);
        CourseCompleted completed = (CourseCompleted) events.get(2);
        assertThat(completed.studentEmail()).isEqualTo("maria@email.com");
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void sameLessonCannotBeCompletedTwice() {
        when(progressRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(progress));
        when(lessonRepository.existsByEnrollmentIdAndLessonNumber(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.completeLesson(1L, 1)).isInstanceOf(BusinessRuleException.class);
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void lessonNumberMustBeWithinCourseRange() {
        when(progressRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> service.completeLesson(1L, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.completeLesson(1L, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelledEnrollmentCannotCompleteLessons() {
        progress.cancel();
        when(progressRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> service.completeLesson(1L, 1)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unknownEnrollmentReturnsNotFound() {
        when(progressRepository.findByEnrollmentId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeLesson(99L, 1)).isInstanceOf(ResourceNotFoundException.class);
    }
}
