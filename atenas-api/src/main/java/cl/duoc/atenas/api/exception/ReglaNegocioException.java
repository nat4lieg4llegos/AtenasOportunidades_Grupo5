package cl.duoc.atenas.api.exception;

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
