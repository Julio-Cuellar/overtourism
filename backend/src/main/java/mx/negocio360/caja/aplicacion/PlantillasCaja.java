package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.compartido.dinero.DecimalPlano;
import org.springframework.modulith.NamedInterface;

/** Textos fijos de Caja en espanol de Mexico; las cifras llegan ya calculadas por el codigo. */
@NamedInterface("textos")
public final class PlantillasCaja {

    /** Mensaje al confirmar CrearCaja. */
    public static final String CAJA_CREADA = "Caja creada.";
    /** Mensaje al confirmar AbrirTurno. */
    public static final String TURNO_ABIERTO = "Turno abierto.";
    /** Mensaje al confirmar un movimiento manual. */
    public static final String MOVIMIENTO_REGISTRADO = "Movimiento registrado.";
    /** Mensaje al confirmar el corte. */
    public static final String CORTE_CONFIRMADO = "Corte confirmado. El turno quedó cerrado.";
    /** Mensaje al confirmar DescartarCorte. */
    public static final String CORTE_DESCARTADO = "Corte descartado. El turno sigue abierto.";
    /** Resumen de DescartarCorte. */
    public static final String DESCARTAR_CORTE = "Descartar el corte preparado y seguir con el turno abierto.";

    private PlantillasCaja() {
    }

    static String resumenCrearCaja(String nombre) {
        return "Crear la caja «%s».".formatted(nombre);
    }

    static String resumenAbrirTurno(String caja, BigDecimal fondo) {
        return "Abrir turno en «%s» con fondo de %s en efectivo.".formatted(caja, texto(fondo));
    }

    static String resumenMovimiento(TipoMovimientoManual tipo, BigDecimal monto, String nota) {
        var accion = tipo == TipoMovimientoManual.ENTRADA ? "Entrada" : "Retiro";
        return "%s de %s en efectivo: %s.".formatted(accion, texto(monto), nota);
    }

    /**
     * Resumen determinista del corte (CJ-12): confirmar lo recalcula y lo compara.
     *
     * @param esperado   por metodo, escala 2
     * @param contado    por metodo, escala 2
     * @param diferencia contado − esperado en efectivo
     * @return texto del resumen
     */
    static String resumenCorte(Map<MetodoPago, BigDecimal> esperado, Map<MetodoPago, BigDecimal> contado,
                               BigDecimal diferencia) {
        return "Corte del turno. Esperado: %s. Contado: %s. Diferencia en efectivo: %s.".formatted(
                porMetodo(esperado), porMetodo(contado), texto(diferencia));
    }

    /**
     * Texto fijo de cada codigo de error.
     *
     * @param codigo codigo
     * @return mensaje para la persona
     */
    public static String de(CodigoError codigo) {
        return switch (codigo) {
            case CAJA_NO_ENCONTRADA -> "No encontramos esa caja.";
            case CAJA_INACTIVA -> "Esa caja está desactivada.";
            case CAJA_DUPLICADA -> "Ya tienes una caja con ese nombre.";
            case NOMBRE_CAJA_INVALIDO -> "El nombre de la caja debe tener entre 1 y 60 caracteres.";
            case TURNO_NO_ENCONTRADO -> "No encontramos ese turno.";
            case TURNO_YA_ABIERTO -> "Esa caja ya tiene un turno abierto.";
            case TURNO_NO_ABIERTO -> "El turno no está abierto.";
            case TURNO_EN_CORTE -> "El turno está en corte. Termina o descarta el corte primero.";
            case FONDO_INVALIDO -> "El fondo debe ser cero o más, con hasta 2 decimales.";
            case MONTO_INVALIDO -> "El monto debe ser mayor que cero, con hasta 2 decimales.";
            case RETIRO_MAYOR_AL_EFECTIVO -> "No hay tanto efectivo en la caja para ese retiro.";
            case NOTA_REQUERIDA -> "Escribe una nota de hasta 200 caracteres.";
            case CORTE_VENTAS_PENDIENTES -> "Hay ventas por confirmar en este turno. Espera unos minutos.";
            case CORTE_EVENTOS_PENDIENTES ->
                    "Aún no llegan todas las ventas del turno. Espera un momento y vuelve a preparar el corte.";
            case CORTE_REQUIERE_AUTORIZACION -> "La diferencia es grande: un encargado debe autorizar el corte.";
            case CONTEO_INVALIDO -> "Revisa el conteo: montos de cero o más, con hasta 2 decimales.";
            case TOKEN_INVALIDO -> "La confirmación no es válida. Vuelve a empezar.";
            case TOKEN_EXPIRADO -> "La confirmación venció. Vuelve a empezar.";
            case TOKEN_USADO -> "Esa confirmación ya se usó.";
            case COMANDO_CAMBIO -> "Lo que ibas a confirmar cambió. Vuelve a empezar.";
            case CONCURRENCIA -> "Alguien más modificó este turno. Recarga y vuelve a empezar.";
            case ERROR_TEMPORAL -> "Hubo un problema temporal. Espera un momento y vuelve a empezar.";
        };
    }

    private static String porMetodo(Map<MetodoPago, BigDecimal> montos) {
        return montos.entrySet().stream().map(e -> e.getKey().name() + " " + texto(e.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String texto(BigDecimal valor) {
        return DecimalPlano.escribir(valor);
    }
}
