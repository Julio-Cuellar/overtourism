package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.infraestructura.CuentasEnMemoria;
import mx.negocio360.restaurantes.infraestructura.MenuEnMemoria;
import mx.negocio360.restaurantes.infraestructura.RestaurantesEnMemoria;
import mx.negocio360.restaurantes.infraestructura.SalonEnMemoria;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * Lo que comparten las pruebas de cuentas y cocina: dos negocios, un reloj que se mueve a mano, el menu, el salon y las
 * cuentas en memoria, un menu con una cemita (cocina) y una horchata (barra) y los puestos del restaurante con sus permisos.
 */
final class EntornoDeCuentas {

    static final UUID NEGOCIO_A = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    static final UUID NEGOCIO_B = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");
    /** 16 de octubre de 2026, mediodia en Ciudad de Mexico (UTC-6). */
    static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    /** Un reloj que se mueve a mano. */
    static final class RelojMutable extends Clock {
        Instant ahora = AHORA;

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    final RelojMutable reloj = new RelojMutable();
    final MenuEnMemoria menu = new MenuEnMemoria();
    final SalonEnMemoria almacenSalon = new SalonEnMemoria();
    final CuentasEnMemoria almacen = new CuentasEnMemoria();
    final List<SobreEvento> eventos = new ArrayList<>();
    private final AtomicInteger contador = new AtomicInteger();
    final SalonServicio salon = new SalonServicio(almacenSalon, almacen, eventos::add,
            () -> String.format("token-%016d", contador.incrementAndGet()), "https://servicial.example/m/", reloj);
    final MenuServicio menuServicio = new MenuServicio(menu, new ConsultaDeRecetas() {
        @Override
        public boolean tieneRecetaActiva(UUID negocioId, UUID platilloId) {
            return false;
        }

        @Override
        public Set<UUID> platillosConReceta(UUID negocioId) {
            return Set.of();
        }
    }, reloj);
    final CuentasServicio servicio = new CuentasServicio(almacen, menu, almacenSalon, salon, eventos::add, reloj);
    final RestaurantesEnMemoria restaurantes = new RestaurantesEnMemoria();
    final HashDeDispositivo hash = HashDeDispositivo.conClave("clave de prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    final PedidosQrServicio qr = new PedidosQrServicio(almacenSalon, almacen, menu, restaurantes, servicio, salon, eventos::add,
            hash, LimitesDeQr.PREDETERMINADOS, reloj);
    final CocinaServicio cocina = new CocinaServicio(almacen, almacenSalon, eventos::add, reloj);

    final ContextoNegocio ctxA = new ContextoNegocio(NEGOCIO_A, "ana");
    final ContextoNegocio ctxB = new ContextoNegocio(NEGOCIO_B, "beto");

    private static Autorizacion de(String... permisos) {
        return Autorizacion.de(Set.of(permisos));
    }

    /** Todos los permisos de Restaurantes. */
    final Autorizacion dueno = de(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_EDITAR,
            PermisosDeRestaurantes.MENU_AGOTAR, PermisosDeRestaurantes.SALON_VER, PermisosDeRestaurantes.SALON_CONFIGURAR,
            PermisosDeRestaurantes.SERVICIO_OPERAR, PermisosDeRestaurantes.CUENTA_VER, PermisosDeRestaurantes.CUENTA_ABRIR,
            PermisosDeRestaurantes.CUENTA_CANCELAR, PermisosDeRestaurantes.LINEA_ANULAR,
            PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA, PermisosDeRestaurantes.COMANDA_VER,
            PermisosDeRestaurantes.COMANDA_PREPARAR, PermisosDeRestaurantes.COMANDA_ENTREGAR);
    /** El mesero (seccion 11): atiende cuentas y entrega, no cobra ni anula lo preparado. */
    final Autorizacion mesero = de(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.SALON_VER,
            PermisosDeRestaurantes.CUENTA_VER, PermisosDeRestaurantes.CUENTA_ABRIR, PermisosDeRestaurantes.CUENTA_CANCELAR,
            PermisosDeRestaurantes.LINEA_ANULAR, PermisosDeRestaurantes.COMANDA_VER, PermisosDeRestaurantes.COMANDA_ENTREGAR);
    /** El cocinero: ve y prepara comandas y marca agotados. */
    final Autorizacion cocinero = de(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_AGOTAR,
            PermisosDeRestaurantes.COMANDA_VER, PermisosDeRestaurantes.COMANDA_PREPARAR);
    /** El gerente: todo menos configurar el salon. */
    final Autorizacion gerente = de(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_EDITAR,
            PermisosDeRestaurantes.SALON_VER, PermisosDeRestaurantes.SERVICIO_OPERAR, PermisosDeRestaurantes.CUENTA_VER,
            PermisosDeRestaurantes.CUENTA_ABRIR, PermisosDeRestaurantes.CUENTA_CANCELAR, PermisosDeRestaurantes.LINEA_ANULAR,
            PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA, PermisosDeRestaurantes.COMANDA_VER,
            PermisosDeRestaurantes.COMANDA_PREPARAR, PermisosDeRestaurantes.COMANDA_ENTREGAR);

    /** El menu y el salon del negocio A. */
    UUID zona;
    UUID mesa1;
    UUID mesa2;
    UUID cemita;
    UUID horchata;
    UUID categoriaBebidas;

    /** Arma el salon con dos mesas, el menu con una cemita (cocina, $85) y una horchata (barra, $35) y abre el servicio. */
    EntornoDeCuentas armado() {
        zona = salon.crearZona(ctxA, dueno, "Salon", 0).id();
        mesa1 = salon.crearMesa(ctxA, dueno, zona, "Mesa 1", 4).id();
        mesa2 = salon.crearMesa(ctxA, dueno, zona, "Mesa 2", 4).id();
        categoriaBebidas = menuServicio.crearCategoria(ctxA, dueno, new DatosCategoria("Bebidas", 1, Estacion.BARRA)).id();
        cemita = menuServicio.crearPlatillo(ctxA, dueno,
                new DatosPlatillo("Cemita poblana", null, new BigDecimal("85.00"), null, Set.of(), null)).id();
        horchata = menuServicio.crearPlatillo(ctxA, dueno,
                new DatosPlatillo("Agua de horchata", null, new BigDecimal("35.00"), categoriaBebidas, Set.of(), null)).id();
        salon.abrirServicio(ctxA, dueno);
        restaurantes.guardar(mx.negocio360.restaurantes.dominio.Restaurante.nuevo(NEGOCIO_A, AHORA).conPublicado(true, AHORA));
        eventos.clear();
        return this;
    }

    /** El token del QR de una mesa del negocio A. */
    String token(UUID mesaId) {
        return almacenSalon.mesa(NEGOCIO_A, mesaId).orElseThrow().qrToken();
    }

    List<SobreEvento> eventosDe(String tipo) {
        return eventos.stream().filter(e -> e.tipo().equals(tipo)).toList();
    }
}
