package br.com.cursosonline.enrollment.exception;

/** Recurso inexistente (mapeado para HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
