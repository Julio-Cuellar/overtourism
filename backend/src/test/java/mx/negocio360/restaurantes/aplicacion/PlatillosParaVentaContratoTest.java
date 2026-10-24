package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.ContratoDeFuenteDeProductosTest;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La fuente de platillos de Restaurantes cumple el contrato de cualquier {@link FuenteDeProductos} (kit de la plataforma,
 * HZ-28) y declara el modo de inventario de cada platillo (R-V2, D-163, D-214). Aisla por negocio y distingue la baja, asi
 * que corre las dos pruebas opcionales del kit.
 */
class PlatillosParaVentaContratoTest extends ContratoDeFuenteDeProductosTest {

    private static final mx.negocio360.compartido.catalogo.ModoInventario NINGUNO =
            mx.negocio360.compartido.catalogo.ModoInventario.NINGUNO;

    private final EntornoDeMenu e = new EntornoDeMenu();
    private final PlatillosParaVenta fuente = new PlatillosParaVenta(e.menu, e.reloj);
    private final Set<UUID> vendibles = new HashSet<>();
    private final Set<UUID> deB = new HashSet<>();
    private UUID baja;

    {
        vendibles.add(crearEnA("Cemita poblana", "85").id());
        vendibles.add(crearEnA("Mole poblano", "120").id());
        var dadoDeBaja = crearEnA("Taco de canasta", "12");
        e.menu.reemplazar(e.menu.platillo(EntornoDeMenu.NEGOCIO_A, dadoDeBaja.id()).orElseThrow()
                .conBaja(EntornoDeMenu.AHORA), dadoDeBaja.version());
        baja = dadoDeBaja.id();
        deB.add(e.servicio.crearPlatillo(e.ctxB, e.todo, new DatosPlatillo("Cemita de B", null, BigDecimal.TEN, null,
                Set.of(), null)).id());
    }

    private PlatilloVista crearEnA(String nombre, String precio) {
        return e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo(nombre, null, new BigDecimal(precio), null,
                Set.of(), null));
    }

    @Override
    protected FuenteDeProductos fuente() {
        return fuente;
    }

    @Override
    protected UUID negocioConProductos() {
        return EntornoDeMenu.NEGOCIO_A;
    }

    @Override
    protected Set<UUID> idsVendibles() {
        return vendibles;
    }

    @Override
    protected Optional<UUID> idDadoDeBaja() {
        return Optional.of(baja);
    }

    @Override
    protected Optional<UUID> otroNegocioConProductos() {
        return Optional.of(EntornoDeMenu.NEGOCIO_B);
    }

    @Override
    protected Set<UUID> idsDelOtroNegocio() {
        return deB;
    }

    @Test
    @DisplayName("R-V2 y D-163: el modo de inventario de la fuente es el del platillo, y NINGUNO si es de otro negocio")
    void modoDeInventarioDelPlatillo() {
        var id = vendibles.iterator().next();
        assertThat(fuente.modoInventario(EntornoDeMenu.NEGOCIO_A, id)).isEqualTo(NINGUNO);

        e.servicio.cambiarModo(e.ctxA, e.todo, id, mx.negocio360.restaurantes.dominio.ModoInventario.VINCULO_DIRECTO);

        assertThat(fuente.modoInventario(EntornoDeMenu.NEGOCIO_A, id))
                .isEqualTo(mx.negocio360.compartido.catalogo.ModoInventario.VINCULO_DIRECTO);
        assertThat(fuente.modoInventario(EntornoDeMenu.NEGOCIO_B, id)).isEqualTo(NINGUNO);
    }
}
