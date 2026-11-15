package cl.duoc.atenas.api.exception;

/** Se lanza cuando se pide una oportunidad que no existe (la API responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(Long id) {
        super("No existe la oportunidad con id " + id);
    }
}
