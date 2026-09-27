package br.com.cursosonline.notification.exception;

/** Violação de regra de negócio / conflito de estado (mapeado para HTTP 409). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
