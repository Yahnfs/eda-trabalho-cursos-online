package br.com.cursosonline.progress.exception;

/** Evento recebido com dados inconsistentes; após as retentativas, a mensagem vai para a DLQ. */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
