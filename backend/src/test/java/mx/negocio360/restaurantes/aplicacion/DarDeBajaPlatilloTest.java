package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.comandos.FalloPreparacion;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MN-10: dar de baja un platillo es preparar -> confirmar, con permiso en los dos pasos y sin cuentas abiertas. */
class DarDeBajaPlatilloTest {

    private final EntornoDeMenu e = new EntornoDeMenu();
    private final PermisosEnPrueba permisos = new PermisosEnPrueba(Set.of(PermisosDeRestaurantes.MENU_EDITAR));
    private final ServicioPreparacion preparacion = new ServicioPreparacion(new PreparacionesEnMemoria(),
            List.of(new ManejadorDarDeBajaPlatillo(e.menu, e.uso, e.recetas, permisos, e.reloj)), e.reloj,
            fallo -> new RestaurantesException(CodigoError.valueOf(fallo.name())));

    /** Permisos que la prueba cambia a mano entre el paso de preparar y el de confirmar. */
    static final class PermisosEnPrueba implements AutorizacionDeRestaurantes {
        Set<String> concedidos;

        PermisosEnPrueba(Set<String> concedidos) {
            this.concedidos = concedidos;
        }

        @Override
        public boolean puede(ContextoNegocio contexto, String permiso) {
            return concedidos.contains(permiso);
        }
    }

    private PlatilloVista cemita() {
        return e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Cemita poblana", null, new BigDecimal("85"), null, Set.of(), null));
    }

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    @Test
    @DisplayName("preparar muestra el platillo y su precio sin tocarlo; confirmar lo da de baja y libera su nombre")
    void baja_feliz() {
        var p = cemita();

        var preparada = preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id()));
        assertThat(preparada.resumen()).contains("Cemita poblana").contains("85.00");
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).activo()).as("preparar no escribe").isTrue();

        var resultado = preparacion.confirmar(e.ctxA, preparada.token());

        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(resultado.recursoId()).isEqualTo(p.id());
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).activo()).isFalse();
        assertThat(e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 20, null).nodos()).isEmpty();
        assertThat(e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Cemita poblana", null, BigDecimal.TEN, null, Set.of(), null)))
                .as("el nombre queda libre").isNotNull();
    }

    @Test
    @DisplayName("si tiene receta, el resumen avisa que se conserva")
    void baja_conReceta() {
        var p = cemita();
        e.recetas.conReceta.add(p.id());

        assertThat(preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())).resumen()).contains("receta");
    }

    @Test
    @DisplayName("MN-10: con el platillo en una cuenta abierta falla con PLATILLO_EN_CUENTA_ABIERTA, al preparar y al confirmar")
    void baja_conCuentaAbierta() {
        var p = cemita();
        e.uso.enCuenta.add(p.id());
        falla(() -> preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())), CodigoError.PLATILLO_EN_CUENTA_ABIERTA);

        e.uso.enCuenta.clear();
        var token = preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())).token();
        e.uso.enCuenta.add(p.id());

        falla(() -> preparacion.confirmar(e.ctxA, token), CodigoError.PLATILLO_EN_CUENTA_ABIERTA);
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).activo()).isTrue();
    }

    @Test
    @DisplayName("el permiso se exige en preparar y otra vez en confirmar: si se lo quitan entre los dos pasos, no baja")
    void baja_permisoEnLosDosPasos() {
        var p = cemita();
        permisos.concedidos = Set.of();
        assertThatThrownBy(() -> preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())))
                .isInstanceOf(mx.negocio360.compartido.seguridad.PermisoDenegadoException.class);

        permisos.concedidos = Set.of(PermisosDeRestaurantes.MENU_EDITAR);
        var token = preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())).token();
        permisos.concedidos = Set.of();

        assertThatThrownBy(() -> preparacion.confirmar(e.ctxA, token))
                .isInstanceOf(mx.negocio360.compartido.seguridad.PermisoDenegadoException.class);
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).activo()).isTrue();
    }

    @Test
    @DisplayName("un platillo de otro negocio o que no existe falla con PLATILLO_NO_ENCONTRADO")
    void baja_noEncontrado() {
        var p = cemita();

        falla(() -> preparacion.preparar(e.ctxB, new DarDeBajaPlatilloComando(p.id())), CodigoError.PLATILLO_NO_ENCONTRADO);
        falla(() -> preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(UUID.randomUUID())), CodigoError.PLATILLO_NO_ENCONTRADO);
        falla(() -> preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(null)), CodigoError.PLATILLO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("el token es de un solo uso y de quien lo preparo: una segunda confirmacion falla con TOKEN_USADO")
    void baja_tokenDeUnSoloUso() {
        var p = cemita();
        var token = preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())).token();
        preparacion.confirmar(e.ctxA, token);

        falla(() -> preparacion.confirmar(e.ctxA, token), CodigoError.TOKEN_USADO);
        falla(() -> preparacion.confirmar(e.ctxB, token), CodigoError.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("si el platillo cambia entre los dos pasos, confirmar falla con COMANDO_CAMBIO")
    void baja_cambioEntreLosPasos() {
        var p = cemita();
        var token = preparacion.preparar(e.ctxA, new DarDeBajaPlatilloComando(p.id())).token();
        e.servicio.editarPlatillo(e.ctxA, e.todo, p.id(), p.version(), new DatosPlatillo("Cemita poblana", null, new BigDecimal("95"), null, Set.of(), null));

        falla(() -> preparacion.confirmar(e.ctxA, token), CodigoError.COMANDO_CAMBIO);
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).activo()).isTrue();
        assertThat(FalloPreparacion.values()).contains(FalloPreparacion.COMANDO_CAMBIO);
    }
}
