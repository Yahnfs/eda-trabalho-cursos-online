package br.com.cursosonline.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import br.com.cursosonline.certificate.config.RabbitTopologyProperties;
import br.com.cursosonline.certificate.domain.Certificate;
import br.com.cursosonline.certificate.domain.CertificateRepository;
import br.com.cursosonline.certificate.event.CertificateIssued;
import br.com.cursosonline.certificate.event.CourseCompleted;
import br.com.cursosonline.certificate.event.DomainEvent;
import br.com.cursosonline.certificate.idempotency.IdempotencyGuard;
import br.com.cursosonline.certificate.messaging.EventPublisher;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificates;
    @Mock
    private IdempotencyGuard idempotency;
    @Mock
    private EventPublisher publisher;

    private CertificateService service;

    @BeforeEach
    void setUp() {
        RabbitTopologyProperties topology = new RabbitTopologyProperties("courses.events", "certificates.fanout",
                "courses.dlx", new RabbitTopologyProperties.RoutingKeys("progress.course.completed"),
                new RabbitTopologyProperties.Queues("q"));
        service = new CertificateService(certificates, idempotency, publisher, topology);
    }

    @Test
    void issuesCertificateAndBroadcastsCertificateIssued() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(certificates.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificates.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Certificate> result = service.issueFor(courseCompleted("e1"));

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).startsWith("CERT-");
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher).publish(eq("certificates.fanout"), eq(""), captor.capture());
        CertificateIssued issued = (CertificateIssued) captor.getValue();
        assertThat(issued.certificateCode()).isEqualTo(result.get().getCode());
        assertThat(issued.enrollmentId()).isEqualTo(1L);
    }

    @Test
    void duplicateEventDoesNotIssueAgain() {
        when(idempotency.firstTime(any())).thenReturn(false);

        assertThat(service.issueFor(courseCompleted("e1"))).isEmpty();

        verify(certificates, never()).save(any());
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void enrollmentWithCertificateIsNotCertifiedTwice() {
        when(idempotency.firstTime(any())).thenReturn(true);
        when(certificates.findByEnrollmentId(1L)).thenReturn(Optional.of(
                new Certificate(1L, "aluno-1", "Maria", "m@e.com", "EDA-101", "EDA", 3, Instant.now())));

        assertThat(service.issueFor(courseCompleted("e2"))).isEmpty();

        verify(certificates, never()).save(any());
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    private static CourseCompleted courseCompleted(String eventId) {
        return new CourseCompleted(eventId, Instant.now(), 1L, "aluno-1", "Maria", "m@e.com", "EDA-101", "EDA", 3,
                Instant.now());
    }
}
