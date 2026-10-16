package mx.negocio360.seguridad;

import java.util.UUID;

/** Solo pruebas: un error de negocio cuyo mensaje lleva un dato personal, para comprobar que nunca sale. */
public final class ErrorDeNegocioDePrueba extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final UUID RECURSO = UUID.fromString("0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10");

    public ErrorDeNegocioDePrueba() {
        super("El telefono 2221234567 ya existe");
    }
}
