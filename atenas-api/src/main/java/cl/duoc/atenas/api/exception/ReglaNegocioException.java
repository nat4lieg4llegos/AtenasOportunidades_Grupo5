package cl.duoc.atenas.api.exception;

/** Error de una regla del caso que involucra más de un campo (la API responde 400). */
public class ReglaNegocioException extends RuntimeException {
    private final String campo;

    public ReglaNegocioException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
