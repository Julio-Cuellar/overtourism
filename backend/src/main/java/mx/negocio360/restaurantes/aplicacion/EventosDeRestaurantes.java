package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.LineaDeCuenta;

/**
 * Los eventos de uso propio de Restaurantes (seccion 9.1 del diseno): {@code comanda_enviada}, {@code comanda_actualizada},
 * {@code pedido_qr_recibido}, {@code cuenta_solicitada}, {@code qr_pausado} y {@code cuenta_con_discrepancia}. Llevan solo ids, la mesa, el numero de cuenta y las lineas, sin
 * datos personales. Alimentan las suscripciones del tablero y del salon con aislamiento por negocio.
 */
public final class EventosDeRestaurantes {

    /** Una comanda recien enviada. */
    public static final String COMANDA_ENVIADA = "restaurantes.comanda_enviada";
    /** Una comanda que cambio de estado o a la que se le anulo una linea. */
    public static final String COMANDA_ACTUALIZADA = "restaurantes.comanda_actualizada";
    /** Un pedido que llego por QR. */
    public static final String PEDIDO_QR_RECIBIDO = "restaurantes.pedido_qr_recibido";
    /** Una cuenta que paso a por cobrar. */
    public static final String CUENTA_SOLICITADA = "restaurantes.cuenta_solicitada";

    /** El QR de una mesa se pauso solo por cuentas canceladas sin comensales (QR-17). */
    public static final String QR_PAUSADO = "restaurantes.qr_pausado";

    /** Una cuenta se pago con una venta cuyas lineas no son las de la cuenta (CT-08, CT-09). */
    public static final String CUENTA_CON_DISCREPANCIA = "restaurantes.cuenta_con_discrepancia";

    private EventosDeRestaurantes() {
    }

    /** {@code restaurantes.comanda_enviada} de una comanda con sus lineas. */
    public static SobreEvento comandaEnviada(Comanda comanda, Cuenta cuenta, String mesa, List<LineaDeCuenta> lineas,
                                             Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("comanda_id", comanda.id().toString());
        datos.put("envio_id", comanda.envioId().toString());
        datos.put("estacion", comanda.estacion().name());
        datos.put("cuenta_id", cuenta.id().toString());
        datos.put("numero", cuenta.numero());
        datos.put("mesa", mesa);
        var lista = new ArrayList<java.util.Map<String, Object>>();
        for (var linea : lineas) {
            var fila = new LinkedHashMap<String, Object>();
            fila.put("platillo", linea.nombre());
            fila.put("cantidad", linea.cantidad());
            if (linea.nota() != null) {
                fila.put("nota", linea.nota());
            }
            fila.put("marca_alergia", linea.marcaAlergia());
            lista.add(fila);
        }
        datos.put("lineas", lista);
        return evento(COMANDA_ENVIADA, comanda.negocioId(), ahora, datos);
    }

    /** {@code restaurantes.comanda_actualizada}, con las lineas anuladas si las hay. */
    public static SobreEvento comandaActualizada(Comanda comanda, List<UUID> lineasAnuladas, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("comanda_id", comanda.id().toString());
        datos.put("envio_id", comanda.envioId().toString());
        datos.put("estacion", comanda.estacion().name());
        datos.put("estado", comanda.estado().name());
        datos.put("lineas_anuladas", lineasAnuladas.stream().map(UUID::toString).toList());
        return evento(COMANDA_ACTUALIZADA, comanda.negocioId(), ahora, datos);
    }

    /** {@code restaurantes.cuenta_solicitada}. */
    public static SobreEvento cuentaSolicitada(Cuenta cuenta, String mesa, String solicitadaPor, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("cuenta_id", cuenta.id().toString());
        datos.put("mesa", mesa);
        datos.put("solicitada_por", solicitadaPor);
        return evento(CUENTA_SOLICITADA, cuenta.negocioId(), ahora, datos);
    }

    /** {@code restaurantes.pedido_qr_recibido}. */
    public static SobreEvento pedidoQrRecibido(Cuenta cuenta, String mesa, int lineas, boolean abrioLaCuenta, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("cuenta_id", cuenta.id().toString());
        datos.put("mesa", mesa);
        datos.put("lineas", lineas);
        datos.put("abrio_la_cuenta", abrioLaCuenta);
        return evento(PEDIDO_QR_RECIBIDO, cuenta.negocioId(), ahora, datos);
    }

    /** {@code restaurantes.cuenta_con_discrepancia}: la venta que pago la cuenta no vendio lo mismo que la cuenta (CT-08). */
    public static SobreEvento cuentaConDiscrepancia(Cuenta cuenta, String mesa, UUID ventaId, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("cuenta_id", cuenta.id().toString());
        datos.put("mesa", mesa);
        datos.put("venta_id", ventaId.toString());
        return evento(CUENTA_CON_DISCREPANCIA, cuenta.negocioId(), ahora, datos);
    }

    /** {@code restaurantes.qr_pausado}: el QR de esa mesa no acepta pedidos hasta {@code hasta} (QR-17). */
    public static SobreEvento qrPausado(UUID negocioId, String mesa, Instant hasta, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("mesa", mesa);
        datos.put("hasta", hasta.toString());
        return evento(QR_PAUSADO, negocioId, ahora, datos);
    }

    private static SobreEvento evento(String tipo, UUID negocioId, Instant ahora, LinkedHashMap<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(ahora), tipo, 1, negocioId, SalonServicio.ORIGEN, ahora,
                UuidV7.generar(ahora), null, datos);
    }
}
