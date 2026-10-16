package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * Catalogo base y reglas por defecto que recibe cada negocio al activar Contabilidad (C-01, D-95). Es el mismo para
 * restaurantes, museos y misceláneos: el giro no cambia el catalogo. Los codigos son ilustrativos y una persona
 * contadora debe validarlos (pregunta 5 del diseno).
 */
public final class CatalogoBase {

    /** Fuente de las reglas que no llevan tasa fiscal. */
    public static final String FUENTE_CATALOGO = "Catalogo base de Contabilidad v0.2 (docs/diseno/contabilidad.md)";

    /** Codigo de la cuenta de caja. */
    public static final String CAJA = "101.01";
    /** Codigo de la cuenta de bancos. */
    public static final String BANCOS = "102.01";
    /** Codigo de la cuenta de deudores por ventas con tarjeta. */
    public static final String DEUDORES_TARJETA = "103.01";
    /** Codigo del IVA trasladado cobrado. */
    public static final String IVA_TRASLADADO = "208.01";
    /** Codigo de las propinas por entregar. */
    public static final String PROPINAS = "209.01";
    /** Codigo de ventas. */
    public static final String VENTAS = "401.01";
    /** Codigo de descuentos sobre ventas. */
    public static final String DESCUENTOS = "402.01";
    /** Codigo de faltantes de caja. */
    public static final String FALTANTES = "601.01";
    /** Codigo de otros ingresos por sobrantes de caja. */
    public static final String SOBRANTES = "704.01";

    private record Definicion(String codigo, String nombre, TipoCuenta tipo, Naturaleza naturaleza, String padre,
                              boolean deSistema) {
    }

    private static final List<Definicion> CUENTAS = new ArrayList<>();

    static {
        grupo("101", "Caja", TipoCuenta.ACTIVO, Naturaleza.DEUDORA, CAJA, "Caja", true);
        grupo("102", "Bancos", TipoCuenta.ACTIVO, Naturaleza.DEUDORA, BANCOS, "Bancos", true);
        grupo("103", "Deudores diversos", TipoCuenta.ACTIVO, Naturaleza.DEUDORA, DEUDORES_TARJETA,
                "Deudores por ventas con tarjeta", true);
        grupo("105", "Cuentas por cobrar", TipoCuenta.ACTIVO, Naturaleza.DEUDORA, "105.01",
                "Cuentas por cobrar a la plataforma", false);
        grupo("109", "Efectivo en transito", TipoCuenta.ACTIVO, Naturaleza.DEUDORA, "109.01",
                "Efectivo en transito", false);
        grupo("208", "Impuestos trasladados", TipoCuenta.PASIVO, Naturaleza.ACREEDORA, IVA_TRASLADADO,
                "IVA trasladado cobrado", true);
        grupo("209", "Propinas", TipoCuenta.PASIVO, Naturaleza.ACREEDORA, PROPINAS, "Propinas por entregar",
                false);
        grupo("401", "Ventas", TipoCuenta.INGRESO, Naturaleza.ACREEDORA, VENTAS, "Ventas", true);
        grupo("402", "Descuentos sobre ventas", TipoCuenta.INGRESO, Naturaleza.DEUDORA, DESCUENTOS,
                "Descuentos sobre ventas", true);
        grupo("601", "Gastos de operacion", TipoCuenta.GASTO, Naturaleza.DEUDORA, FALTANTES,
                "Faltantes de caja", true);
        grupo("704", "Otros ingresos", TipoCuenta.INGRESO, Naturaleza.ACREEDORA, SOBRANTES,
                "Otros ingresos: sobrantes de caja", true);
    }

    private CatalogoBase() {
    }

    /** Agrega una cuenta padre (no afectable) y su unica hija afectable, que hereda tipo y naturaleza (C-02). */
    private static void grupo(String codigoPadre, String nombrePadre, TipoCuenta tipo, Naturaleza naturaleza,
                              String codigoHija, String nombreHija, boolean hijaDeSistema) {
        CUENTAS.add(new Definicion(codigoPadre, nombrePadre, tipo, naturaleza, null, false));
        CUENTAS.add(new Definicion(codigoHija, nombreHija, tipo, naturaleza, codigoPadre, hijaDeSistema));
    }

    /**
     * Cuentas del catalogo base para un negocio, con ids UUIDv7 nuevos. Los padres van antes que sus hijas.
     *
     * @param negocioId negocio que recibe la copia
     * @param ahora     instante para generar los ids
     * @return las cuentas, en orden de codigo
     */
    public static List<Cuenta> cuentasPara(UUID negocioId, Instant ahora) {
        var ids = new HashMap<String, UUID>();
        var cuentas = new ArrayList<Cuenta>();
        for (var definicion : CUENTAS) {
            var id = UuidV7.generar(ahora);
            ids.put(definicion.codigo(), id);
            cuentas.add(new Cuenta(id, negocioId, definicion.codigo(), definicion.nombre(), definicion.tipo(),
                    definicion.naturaleza(), definicion.padre() == null ? null : ids.get(definicion.padre()),
                    definicion.padre() != null, definicion.deSistema(), null, EstadoCuenta.ACTIVA, 0));
        }
        return List.copyOf(cuentas);
    }

    /**
     * Reglas por defecto. Las de venta y cupon usan la tasa capturada al activar, con su fuente; nunca una tasa de
     * memoria (F-01).
     *
     * @param negocioId  negocio
     * @param porCodigo  ids de las cuentas del negocio, por codigo
     * @param tasaIva    tasa capturada al activar, o nula si es exento
     * @param fuenteTasa de donde salio la tasa
     * @param ahora      inicio de vigencia
     * @return las reglas
     */
    public static List<ReglaContable> reglasPara(UUID negocioId, Map<String, UUID> porCodigo, BigDecimal tasaIva,
                                                 String fuenteTasa, Instant ahora) {
        var reglas = new ArrayList<ReglaContable>();
        for (var medio : List.of(new String[] {"EFECTIVO", CAJA}, new String[] {"TARJETA", DEUDORES_TARJETA},
                new String[] {"TRANSFERENCIA", BANCOS})) {
            reglas.add(regla(negocioId, Disparador.VENTA, medio[0], porCodigo.get(medio[1]), porCodigo.get(VENTAS),
                    porCodigo.get(IVA_TRASLADADO), tasaIva, fuenteTasa, ahora));
        }
        reglas.add(regla(negocioId, Disparador.CUPON, "NEGOCIO", porCodigo.get(DESCUENTOS), porCodigo.get(VENTAS),
                null, tasaIva, fuenteTasa, ahora));
        reglas.add(regla(negocioId, Disparador.CORTE_FALTANTE, null, porCodigo.get(FALTANTES), porCodigo.get(CAJA),
                null, null, FUENTE_CATALOGO, ahora));
        reglas.add(regla(negocioId, Disparador.CORTE_SOBRANTE, null, porCodigo.get(CAJA), porCodigo.get(SOBRANTES),
                null, null, FUENTE_CATALOGO, ahora));
        return List.copyOf(reglas);
    }

    private static ReglaContable regla(UUID negocioId, Disparador disparador, String condicion, UUID cargo,
                                       UUID abono, UUID impuesto, BigDecimal tasa, String fuente, Instant ahora) {
        return new ReglaContable(UuidV7.generar(ahora), negocioId, disparador, condicion, cargo, abono, impuesto,
                tasa, fuente, ahora, null, 0);
    }
}
