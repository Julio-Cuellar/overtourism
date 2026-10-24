package mx.negocio360.restaurantes.aplicacion;

import java.time.Duration;
import java.time.Instant;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;

/**
 * Arma lo que el tablero muestra de una comanda (CD-03 a CD-06). El tiempo de preparacion y el resaltado de las comandas
 * retrasadas los calcula el codigo; el umbral es de 15 minutos por omision (regla versionada mas adelante, CD-04).
 */
final class Tablero {

    /** Cuanto puede esperar una comanda sin estar lista antes de resaltarse (CD-04). */
    static final Duration UMBRAL_DE_RETRASO = Duration.ofMinutes(15);

    private Tablero() {
    }

    /** La vista de una comanda con las lineas de su cuenta; las anuladas salen marcadas para tacharlas (CD-05). */
    static ComandaVista vista(Comanda comanda, Cuenta cuenta, String mesa, Instant ahora) {
        var lineas = cuenta.lineas().stream().filter(l -> comanda.lineaIds().contains(l.id()))
                .map(l -> new LineaDeComandaVista(l.id(), l.nombre(), l.cantidad(), l.nota(), l.marcaAlergia(),
                        l.estado() == EstadoDeLinea.ANULADA)).toList();
        var esperando = comanda.estado() == EstadoDeComanda.NUEVA || comanda.estado() == EstadoDeComanda.EN_PREPARACION;
        var retrasada = esperando && Duration.between(comanda.enviadaEn(), ahora).compareTo(UMBRAL_DE_RETRASO) > 0;
        return new ComandaVista(comanda.id(), comanda.envioId(), comanda.cuentaId(), cuenta.numero(), mesa,
                comanda.estacion(), comanda.estado(), comanda.enviadaEn(), comanda.listaEn(),
                comanda.segundosDePreparacion(), retrasada, lineas, comanda.version());
    }
}
