package mx.negocio360.identidad;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de iniciar sesion: el unico momento en que el token viaja en claro. {@code negocioActivo} es nulo y
 * {@code negocios} esta vacio hasta que existan las membresias (IDN-2). {@code toString} no revela el token.
 */
public record SesionIniciada(String token, Instant expiraEn, CuentaVista cuenta, UUID negocioActivo,
                             List<NegocioVista> negocios) {

    public SesionIniciada {
        negocios = List.copyOf(negocios);
    }

    @Override
    public String toString() {
        return "SesionIniciada[cuentaId=" + cuenta.id() + ", expiraEn=" + expiraEn + "]";
    }
}
