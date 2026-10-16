package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.infraestructura.MenuEnMemoria;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/** Lo que comparten las pruebas del menu: un negocio, un reloj fijo, el menu en memoria y recetas que se programan a mano. */
final class EntornoDeMenu {

    static final UUID NEGOCIO_A = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    static final UUID NEGOCIO_B = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");
    /** 16 de octubre de 2026, mediodia en Ciudad de Mexico (UTC-6). */
    static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    final MenuEnMemoria menu = new MenuEnMemoria();
    final RecetasDePrueba recetas = new RecetasDePrueba();
    final UsoDePrueba uso = new UsoDePrueba();
    final MenuServicio servicio = new MenuServicio(menu, recetas, reloj);

    final ContextoNegocio ctxA = new ContextoNegocio(NEGOCIO_A, "ana");
    final ContextoNegocio ctxB = new ContextoNegocio(NEGOCIO_B, "beto");
    /** Todos los permisos del menu. */
    final Autorizacion todo = Autorizacion.de(Set.of(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_EDITAR,
            PermisosDeRestaurantes.MENU_AGOTAR));

    static Autorizacion solo(String... permisos) {
        return Autorizacion.de(Set.of(permisos));
    }

    /** Recetas que cada prueba declara con un platillo y su resultado. */
    static final class RecetasDePrueba implements ConsultaDeRecetas {
        final Set<UUID> conReceta = new HashSet<>();

        @Override
        public boolean tieneRecetaActiva(UUID negocioId, UUID platilloId) {
            return conReceta.contains(platilloId);
        }

        @Override
        public Set<UUID> platillosConReceta(UUID negocioId) {
            return Set.copyOf(conReceta);
        }
    }

    /** Cuentas abiertas que cada prueba declara. */
    static final class UsoDePrueba implements UsoDeLosPlatillos {
        final Set<UUID> enCuenta = new HashSet<>();

        @Override
        public boolean enCuentaAbierta(UUID negocioId, UUID platilloId) {
            return enCuenta.contains(platilloId);
        }
    }
}
