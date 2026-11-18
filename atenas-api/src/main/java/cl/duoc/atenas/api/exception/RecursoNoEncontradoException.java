package cl.duoc.atenas.api.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(Long id) {
        super("No existe la oportunidad con id " + id);
    }
}
