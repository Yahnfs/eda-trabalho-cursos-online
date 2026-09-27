package br.com.cursosonline.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.cursosonline.notification.domain.Notification;
import br.com.cursosonline.notification.domain.NotificationRepository;
import br.com.cursosonline.notification.event.CertificateIssued;
import br.com.cursosonline.notification.event.EnrollmentCreated;
import br.com.cursosonline.notification.exception.NotificationDeliveryException;
import br.com.cursosonline.notification.idempotency.IdempotencyGuard;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notifications;
    @Mock
    private IdempotencyGuard idempotency;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications, idempotency, new EmailGateway("falha.com"));
    }

    @Test
    void enrollmentCreatedSendsAndStoresNotification() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(notifications.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onEnrollmentCreated(enrollmentCreated("maria@email.com"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo("maria@email.com");
        assertThat(saved.getEventType()).isEqualTo("EnrollmentCreated");
        assertThat(saved.getSubject()).contains("Matrícula confirmada");
        assertThat(saved.getMessage()).contains("Maria", "3 aulas");
    }

    @Test
    void certificateIssuedMessageContainsCertificateCode() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(notifications.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onCertificateIssued(new CertificateIssued("e9", Instant.now(), 1L, "CERT-ABC", 1L, "aluno-1",
                "Maria", "maria@email.com", "EDA-101", "EDA", Instant.now()));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("CERT-ABC");
    }

    @Test
    void duplicateEventIsNotNotifiedTwice() {
        when(idempotency.firstTime(any())).thenReturn(false);

        service.onEnrollmentCreated(enrollmentCreated("maria@email.com"));

        verify(notifications, never()).save(any());
    }

    @Test
    void deliveryFailurePropagatesSoTheMessageIsRetried() {
        when(idempotency.firstTime(any())).thenReturn(true);

        assertThatThrownBy(() -> service.onEnrollmentCreated(enrollmentCreated("joao@falha.com")))
                .isInstanceOf(NotificationDeliveryException.class);
        verify(notifications, never()).save(any());
    }

    private static EnrollmentCreated enrollmentCreated(String email) {
        return new EnrollmentCreated("e1", Instant.now(), 1L, "aluno-1", "Maria", email, "EDA-101", "EDA", 3);
    }
}
