package mx.negocio360.contabilidad.contratos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que Contabilidad lee de {@code caja.cerrada} v1. Caja no tiene diseno: este contrato es la propuesta de D-103 y
 * hay que congelarlo con su dueno. {@code ventas_efectivo} es opcional; sin el no se concilia contra las ventas.
 *
 * @param corteId          corte de caja; llave natural (E-02)
 * @param cajaSesionId     sesion de caja que se cerro
 * @param efectivoEsperado efectivo que Caja esperaba
 * @param efectivoContado  efectivo contado al cerrar
 * @param ventasEfectivo   ventas en efectivo de la sesion segun Caja, o nulo
 * @param cerradaEn        momento del cierre
 */
public record CajaCerradaV1(UUID corteId, UUID cajaSesionId, BigDecimal efectivoEsperado,
                            BigDecimal efectivoContado, BigDecimal ventasEfectivo, Instant cerradaEn) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "caja.cerrada";
    /** Unica version del contrato que Contabilidad sabe leer. */
    public static final int VERSION = 1;
    /** Campos del contrato que usa Contabilidad; son los unicos que se copian a un pendiente (E-04). */
    public static final List<String> CAMPOS = List.of("corte_id", "caja_sesion_id", "efectivo_esperado",
            "efectivo_contado", "ventas_efectivo", "cerrada_en");

    /**
     * Lee los datos del evento.
     *
     * @param datos carga del sobre
     * @return el contrato leido
     * @throws DatosInvalidosException si falta un campo obligatorio, si un monto es negativo o si un campo no tiene
     *                                 forma valida
     */
    public static CajaCerradaV1 leer(Map<String, Object> datos) {
        var lector = new LectorDatos(datos);
        var esperado = noNegativo(lector.dinero("efectivo_esperado"), "efectivo_esperado");
        var contado = noNegativo(lector.dinero("efectivo_contado"), "efectivo_contado");
        var ventas = lector.dineroOpcional("ventas_efectivo");
        if (ventas != null) {
            noNegativo(ventas, "ventas_efectivo");
        }
        return new CajaCerradaV1(lector.uuid("corte_id"), lector.uuid("caja_sesion_id"), esperado, contado, ventas,
                lector.instante("cerrada_en"));
    }

    private static BigDecimal noNegativo(BigDecimal monto, String campo) {
        if (monto.signum() < 0) {
            throw new DatosInvalidosException(campo);
        }
        return monto;
    }
}
