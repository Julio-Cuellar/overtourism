package mx.negocio360.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Revision de codigo M3: los ids de movimientos crecen aunque el reloj tenga resolucion menor a un milisegundo. */
class SecuenciaDeIdsTest {

    private static final Instant BASE = Instant.parse("2026-10-15T18:00:00Z");

    @Test
    @DisplayName("con un reloj de microsegundos, dos ids del mismo milisegundo salen en orden creciente")
    void siguiente_microsegundosDentroDeUnMilisegundo_ordenCreciente() {
        var secuencia = new SecuenciaDeIds();
        var ids = new ArrayList<UUID>();

        for (var i = 0; i < 50; i++) {
            ids.add(secuencia.siguiente(BASE.plusNanos(i * 10_000L)));
        }

        for (var i = 1; i < ids.size(); i++) {
            assertThat(UuidV7.instanteDe(ids.get(i)).toEpochMilli())
                    .isGreaterThan(UuidV7.instanteDe(ids.get(i - 1)).toEpochMilli());
        }
    }

    @Test
    @DisplayName("si el reloj ya avanzo mas de un milisegundo, el id usa la hora real")
    void siguiente_relojAvanzo_usaLaHoraReal() {
        var secuencia = new SecuenciaDeIds();
        secuencia.siguiente(BASE);

        var id = secuencia.siguiente(BASE.plusSeconds(5));

        assertThat(UuidV7.instanteDe(id)).isEqualTo(BASE.plusSeconds(5));
    }
}
