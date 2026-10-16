package mx.negocio360.identidad;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de iniciar sesion o registrarse: el unico momento en que el token viaja en claro. {@code negocioActivo} es
 * nulo mientras la persona no tenga uno (explicito o el unico que tiene). {@code negociosPendientes} son los negocios
 * que pidio y el modulo de negocios aun no registra (D-79). {@code toString} no revela el token.
 */
public record SesionIniciada(String token, Instant expiraEn, CuentaVista cuenta, UUID negocioActivo,
                             List<NegocioVista> negocios, List<NegocioPendienteVista> negociosPendientes) {

    public SesionIniciada {
        negocios = List.copyOf(negocios);
        negociosPendientes = List.copyOf(negociosPendientes);
    }

    /** Sin negocios pendientes. */
    public SesionIniciada(String token, Instant expiraEn, CuentaVista cuenta, UUID negocioActivo,
                          List<NegocioVista> negocios) {
        this(token, expiraEn, cuenta, negocioActivo, negocios, List.of());
    }

    @Override
    public String toString() {
        return "SesionIniciada[cuentaId=" + cuenta.id() + ", expiraEn=" + expiraEn + "]";
    }
}
