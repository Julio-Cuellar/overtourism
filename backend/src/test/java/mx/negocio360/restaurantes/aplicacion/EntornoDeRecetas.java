package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.infraestructura.MenuEnMemoria;
import mx.negocio360.restaurantes.infraestructura.RecetasEnMemoria;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/** Menu, recetas e Inventario de mentiras para las pruebas de R2 y R3, con el reloj fijo del 16 de octubre de 2026. */
final class EntornoDeRecetas {

    static final UUID NEGOCIO_A = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    static final UUID NEGOCIO_B = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");
    static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    final MutableClock reloj = new MutableClock(AHORA);
    final MenuEnMemoria menuAlmacen = new MenuEnMemoria();
    final RecetasEnMemoria recetasAlmacen = new RecetasEnMemoria();
    final InventarioDePrueba inventario = new InventarioDePrueba();
    final PermisosVariables permisos = new PermisosVariables();
    final MenuServicio menu = new MenuServicio(menuAlmacen, recetasAlmacen, reloj);
    final RecetasServicio recetas = new RecetasServicio(recetasAlmacen, menuAlmacen, inventario, reloj);
    final ServicioPreparacion preparacion = new ServicioPreparacion(new PreparacionesEnMemoria(),
            List.of(new ManejadorGuardarReceta(menuAlmacen, recetasAlmacen, inventario, permisos, reloj),
                    new ManejadorDarDeBajaPlatillo(menuAlmacen, new SinUso(), recetasAlmacen, permisos, reloj)),
            reloj, fallo -> new RestaurantesException(CodigoError.valueOf(fallo.name())));

    final ContextoNegocio ctxA = new ContextoNegocio(NEGOCIO_A, "ana");
    final ContextoNegocio ctxB = new ContextoNegocio(NEGOCIO_B, "beto");
    final Autorizacion todo = Autorizacion.de(Set.of(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_EDITAR,
            PermisosDeRestaurantes.MENU_AGOTAR, PermisosDeRestaurantes.RECETA_VER, PermisosDeRestaurantes.RECETA_EDITAR,
            PermisosDeRestaurantes.COSTO_VER));

    EntornoDeRecetas() {
        permisos.concedidos = Set.of(PermisosDeRestaurantes.MENU_EDITAR, PermisosDeRestaurantes.RECETA_EDITAR,
                PermisosDeRestaurantes.COSTO_VER);
    }

    /** Un reloj que la prueba avanza a mano. */
    static final class MutableClock extends Clock {
        private Instant ahora;

        MutableClock(Instant ahora) {
            this.ahora = ahora;
        }

        void avanzar(java.time.Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return Clock.fixed(ahora, zona);
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    /** Permisos de los comandos que la prueba cambia a mano. */
    static final class PermisosVariables implements AutorizacionDeRestaurantes {
        Set<String> concedidos = Set.of();

        @Override
        public boolean puede(ContextoNegocio contexto, String permiso) {
            return concedidos.contains(permiso);
        }
    }

    private static final class SinUso implements UsoDeLosPlatillos {
        @Override
        public boolean enCuentaAbierta(UUID negocioId, UUID platilloId) {
            return false;
        }
    }
}
