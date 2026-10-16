package mx.negocio360.negocios.contratos;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Lo que Negocios lee de {@code negocio.registro_solicitado} v1 (B1, D-78), que publica Identidad cuando alguien pide
 * un negocio. Lector tolerante (R5): ignora los campos que no usa. El negocio es el del sobre (lo asigno Identidad,
 * D-89); los datos traen la cuenta que lo pide, el nombre y el giro. Solo vale lo que publica Identidad (origen
 * {@value #ORIGEN}). Lo que no entiende (otro tipo, version u origen, o datos mal formados) devuelve vacio:
 * reintentarlo no lo arreglaria (EV-03).
 */
public record NegocioRegistroSolicitadoV1(UUID negocioId, UUID cuentaId, String nombre, String giro, UUID eventoId,
                                          UUID correlacionId) {

    public static final String TIPO = "negocio.registro_solicitado";
    public static final int VERSION = 1;
    public static final String ORIGEN = "identidad";

    public NegocioRegistroSolicitadoV1 {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(giro, "giro");
        Objects.requireNonNull(eventoId, "eventoId");
        Objects.requireNonNull(correlacionId, "correlacionId");
    }

    public static Optional<NegocioRegistroSolicitadoV1> de(SobreEvento sobre) {
        if (!TIPO.equals(sobre.tipo()) || sobre.version() != VERSION || !ORIGEN.equals(sobre.origen())) {
            return Optional.empty();
        }
        var datos = sobre.datos();
        if (!(datos.get("nombre") instanceof String nombre) || !(datos.get("giro") instanceof String giro)
                || !(datos.get("cuenta_id") instanceof String cuenta)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new NegocioRegistroSolicitadoV1(sobre.negocioId(), UUID.fromString(cuenta), nombre, giro,
                    sobre.id(), sobre.correlacionId()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Sin el nombre ni la cuenta: son datos de una persona y no deben acabar en un registro. */
    @Override
    public String toString() {
        return "NegocioRegistroSolicitadoV1[negocioId=" + negocioId + ", eventoId=" + eventoId + "]";
    }
}
