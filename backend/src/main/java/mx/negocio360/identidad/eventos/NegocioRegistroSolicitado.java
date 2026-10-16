package mx.negocio360.identidad.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Giro;

/**
 * Una persona pidio registrar un negocio (D-78). Lo consume el modulo de negocios. El sobre trae el id que Identidad
 * asigno al negocio; los datos, la cuenta que lo pide, el nombre del negocio y su giro. Nada de datos personales.
 */
public record NegocioRegistroSolicitado(UUID cuentaId, String nombre, Giro giro) {

    public static final String TIPO = "negocio.registro_solicitado";
    public static final int VERSION = 1;

    public NegocioRegistroSolicitado {
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(giro, "giro");
    }

    public Map<String, Object> datos() {
        return Map.of("cuenta_id", cuentaId.toString(), "nombre", nombre, "giro", giro.name());
    }
}
