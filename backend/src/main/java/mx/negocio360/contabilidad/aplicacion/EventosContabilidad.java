package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.UUID;
import mx.negocio360.compartido.dinero.DecimalPlano;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.contabilidad.dominio.Poliza;

/**
 * Arma los sobres de los eventos que publica Contabilidad (seccion 8.2 del diseno). Ningun evento lleva conceptos,
 * actores ni otros textos libres: solo ids, tipos y montos (SK-07).
 */
final class EventosContabilidad {

    /** Origen de todos los eventos del modulo. */
    static final String ORIGEN = "contabilidad";
    /** Tipo del evento de una poliza registrada. */
    static final String POLIZA_REGISTRADA = "poliza.registrada";
    /** Tipo del evento de un mes cerrado. */
    static final String PERIODO_CERRADO = "periodo.cerrado";

    private EventosContabilidad() {
    }

    /**
     * {@code poliza.registrada} v1. La causa es el evento que origino la poliza, si lo hay; la correlacion es la
     * referencia (por ejemplo, la venta) o, si no hay, la propia poliza.
     */
    static SobreEvento polizaRegistrada(Poliza poliza, YearMonth periodo, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("poliza_id", poliza.id().toString());
        datos.put("tipo", poliza.tipo().name());
        datos.put("folio", poliza.folio());
        datos.put("origen", poliza.origen().name());
        datos.put("periodo", periodo.toString());
        datos.put("total", DecimalPlano.escribir(poliza.total()));
        if (poliza.referenciaTipo() != null) {
            datos.put("referencia_tipo", poliza.referenciaTipo());
            datos.put("referencia_id", poliza.referenciaId().toString());
        }
        var correlacion = poliza.referenciaId() == null ? poliza.id() : poliza.referenciaId();
        return new SobreEvento(UuidV7.generar(ahora), POLIZA_REGISTRADA, 1, poliza.negocioId(), ORIGEN, ahora,
                correlacion, poliza.eventoId(), datos);
    }

    /** {@code periodo.cerrado} v1, con las ventas netas y el resultado del mes. */
    static SobreEvento periodoCerrado(UUID negocioId, UUID periodoId, YearMonth periodo, BigDecimal ventasNetas,
                                      BigDecimal resultado, Instant cerradoEn) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("periodo", periodo.toString());
        datos.put("cerrado_en", cerradoEn.toString());
        datos.put("ventas_netas", DecimalPlano.escribir(ventasNetas));
        datos.put("resultado", DecimalPlano.escribir(resultado));
        return new SobreEvento(UuidV7.generar(cerradoEn), PERIODO_CERRADO, 1, negocioId, ORIGEN, cerradoEn,
                periodoId, null, datos);
    }
}
