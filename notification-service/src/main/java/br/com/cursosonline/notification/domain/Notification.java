package br.com.cursosonline.notification.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Notificação enviada ao aluno (histórico persistido). */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    private Long enrollmentId;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(String eventId, String eventType, Long enrollmentId, String recipient,
                        String subject, String message) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.enrollmentId = enrollmentId;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.channel = "EMAIL";
        this.sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getChannel() {
        return channel;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
