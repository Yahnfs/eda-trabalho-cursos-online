package br.com.cursosonline.notification.web;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cursosonline.notification.domain.Notification;
import br.com.cursosonline.notification.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam(required = false) String recipient) {
        return service.list(recipient).stream().map(NotificationResponse::from).toList();
    }

    public record NotificationResponse(
            Long id,
            String eventId,
            String eventType,
            Long enrollmentId,
            String recipient,
            String channel,
            String subject,
            String message,
            Instant sentAt) {

        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getEventId(), n.getEventType(), n.getEnrollmentId(),
                    n.getRecipient(), n.getChannel(), n.getSubject(), n.getMessage(), n.getSentAt());
        }
    }
}
