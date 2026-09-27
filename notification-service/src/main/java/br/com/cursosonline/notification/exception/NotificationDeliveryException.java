package br.com.cursosonline.notification.exception;

/** Falha (transitória) no envio de notificação: provoca retentativas e, se persistir, DLQ. */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message) {
        super(message);
    }
}
