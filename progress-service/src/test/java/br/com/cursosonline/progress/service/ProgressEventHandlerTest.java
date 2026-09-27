package br.com.cursosonline.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.cursosonline.progress.domain.CourseProgress;
import br.com.cursosonline.progress.domain.CourseProgressRepository;
import br.com.cursosonline.progress.domain.ProgressStatus;
import br.com.cursosonline.progress.event.EnrollmentCancelled;
import br.com.cursosonline.progress.event.EnrollmentCreated;
import br.com.cursosonline.progress.exception.InvalidEventException;
import br.com.cursosonline.progress.idempotency.IdempotencyGuard;

@ExtendWith(MockitoExtension.class)
class ProgressEventHandlerTest {

    @Mock
    private CourseProgressRepository progressRepository;
    @Mock
    private IdempotencyGuard idempotency;

    private ProgressEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProgressEventHandler(progressRepository, idempotency);
    }

    @Test
    void enrollmentCreatedStartsProgressTracking() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        handler.onEnrollmentCreated(created(3));

        ArgumentCaptor<CourseProgress> captor = ArgumentCaptor.forClass(CourseProgress.class);
        verify(progressRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalLessons()).isEqualTo(3);
        assertThat(captor.getValue().getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void duplicateEnrollmentCreatedIsIgnored() {
        when(idempotency.firstTime(any())).thenReturn(false);

        handler.onEnrollmentCreated(created(3));

        verify(progressRepository, never()).save(any());
    }

    @Test
    void invalidEnrollmentCreatedIsRejected() {
        assertThatThrownBy(() -> handler.onEnrollmentCreated(created(0))).isInstanceOf(InvalidEventException.class);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void enrollmentCancelledCancelsProgress() {
        CourseProgress progress = new CourseProgress(1L, "aluno-1", "Maria", "m@e.com", "EDA-101", "EDA", 3);
        when(idempotency.firstTime(any())).thenReturn(true);
        when(progressRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(progress));

        handler.onEnrollmentCancelled(new EnrollmentCancelled("e2", Instant.now(), 1L, "aluno-1", "Maria",
                "m@e.com", "EDA-101", "EDA", "motivo"));

        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.CANCELLED);
    }

    private static EnrollmentCreated created(int totalLessons) {
        return new EnrollmentCreated("e1", Instant.now(), 1L, "aluno-1", "Maria", "m@e.com", "EDA-101", "EDA",
                totalLessons);
    }
}
