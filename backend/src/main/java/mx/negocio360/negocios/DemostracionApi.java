package mx.negocio360.negocios;

import java.util.List;
import java.util.UUID;

/**
 * La demo del stand (DM-01 a DM-05, D-104, D-105): siembra negocios simulados por el mismo camino del alta y restablece
 * lo que hicieron los visitantes. Solo funciona con el modo demostracion encendido.
 */
public interface DemostracionApi {

    /** @param negocios los doce ids; {@code negociosCreados} y {@code resenasSembradas} son lo que esta corrida hizo de nuevo */
    record ResultadoSiembra(List<UUID> negocios, int negociosCreados, int resenasSembradas) {

        public ResultadoSiembra {
            negocios = List.copyOf(negocios);
        }
    }

    record ResultadoRestablecimiento(int negocios, int resenasBorradas, int resenasRestauradas) {
    }

    /** Idempotente: lo que ya existe no se repite. */
    ResultadoSiembra sembrar();

    /** Borra las reseñas, la disponibilidad y los limites del stand, y deja la siembra como estaba. No toca otros negocios. */
    ResultadoRestablecimiento restablecer();
}
