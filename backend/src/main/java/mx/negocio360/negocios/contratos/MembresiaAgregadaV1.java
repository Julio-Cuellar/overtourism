package mx.negocio360.negocios.contratos;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Lo que Negocios lee de {@code identidad.membresia_agregada} v1 (B2): a quien se le dio una membresia en que negocio y
 * de donde viene. Por ahora solo sirve para avisar que el alta quedo lista (W1); atar la ficha del empleado con su
 * cuenta llega con el equipo. Lector tolerante (R5): ignora el rol y los campos que no usa. Lo que no entiende devuelve
 * vacio (EV-03).
 */
public record MembresiaAgregadaV1(UUID negocioId, UUID cuentaId, String origen) {

    public static final String TIPO = "identidad.membresia_agregada";
    public static final int VERSION = 1;
    public static final String ORIGEN_EVENTO = "identidad";
    public static final String DUENO_INICIAL = "DUENO_INICIAL";

    public MembresiaAgregadaV1 {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(origen, "origen");
    }

    public static Optional<MembresiaAgregadaV1> de(SobreEvento sobre) {
        if (!TIPO.equals(sobre.tipo()) || sobre.version() != VERSION || !ORIGEN_EVENTO.equals(sobre.origen())) {
            return Optional.empty();
        }
        if (!(sobre.datos().get("cuenta_id") instanceof String cuenta)
                || !(sobre.datos().get("origen") instanceof String origen)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MembresiaAgregadaV1(sobre.negocioId(), UUID.fromString(cuenta), origen));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** El alta del negocio termino de verdad cuando se le da la membresia al dueno inicial. */
    public boolean esDuenoInicial() {
        return DUENO_INICIAL.equals(origen);
    }
}
