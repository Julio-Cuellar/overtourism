package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.infraestructura.SalonEnMemoria;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/** Lo que comparten las pruebas del salon: dos negocios, un reloj fijo, el salon en memoria y las cuentas que cada prueba declara. */
final class EntornoDeSalon {

    static final UUID NEGOCIO_A = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    static final UUID NEGOCIO_B = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");
    /** 16 de octubre de 2026, mediodia en Ciudad de Mexico (UTC-6). */
    static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");
    static final String BASE_QR = "https://servicial.example/m/";

    final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    final SalonEnMemoria almacen = new SalonEnMemoria();
    final OcupacionDePrueba ocupacion = new OcupacionDePrueba();
    final List<SobreEvento> eventos = new ArrayList<>();
    private final AtomicInteger contador = new AtomicInteger();
    final GeneradorDeTokens tokens = () -> String.format("token-%016d", contador.incrementAndGet());
    final SalonServicio servicio = new SalonServicio(almacen, ocupacion, eventos::add, tokens, BASE_QR, reloj);

    final ContextoNegocio ctxA = new ContextoNegocio(NEGOCIO_A, "ana");
    final ContextoNegocio ctxB = new ContextoNegocio(NEGOCIO_B, "beto");

    /** Todos los permisos del salon, el servicio y la anulacion de lo preparado (el gerente en adelante). */
    final Autorizacion todo = Autorizacion.de(Set.of(PermisosDeRestaurantes.SALON_VER,
            PermisosDeRestaurantes.SALON_CONFIGURAR, PermisosDeRestaurantes.SERVICIO_OPERAR,
            PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA));
    /** Un mesero: ve el salon y nada mas. */
    final Autorizacion mesero = Autorizacion.de(Set.of(PermisosDeRestaurantes.SALON_VER));
    /** Quien opera el servicio sin ser gerente. */
    final Autorizacion operador = Autorizacion.de(Set.of(PermisosDeRestaurantes.SALON_VER,
            PermisosDeRestaurantes.SERVICIO_OPERAR));

    static Autorizacion solo(String... permisos) {
        return Autorizacion.de(Set.of(permisos));
    }

    /** Una zona y sus mesas ya creadas, para las pruebas que no hablan de crear. */
    UUID zona(String nombre) {
        return servicio.crearZona(ctxA, todo, nombre, 0).id();
    }

    UUID mesa(UUID zona, String etiqueta, int capacidad) {
        return servicio.crearMesa(ctxA, todo, zona, etiqueta, capacidad).id();
    }

    List<SobreEvento> eventosDe(String tipo) {
        return eventos.stream().filter(e -> e.tipo().equals(tipo)).toList();
    }

    /** Cuentas no pagadas que cada prueba declara por mesa. */
    static final class OcupacionDePrueba implements OcupacionDeMesas {
        final Map<UUID, ResumenDeCuenta> porMesa = new HashMap<>();

        void ocupar(UUID mesaId) {
            porMesa.put(mesaId, new ResumenDeCuenta(UUID.randomUUID(), 1, "ABIERTA", java.math.BigDecimal.ZERO, AHORA,
                    false, false));
        }

        void liberar(UUID mesaId) {
            porMesa.remove(mesaId);
        }

        @Override
        public Map<UUID, ResumenDeCuenta> cuentasNoPagadas(UUID negocioId) {
            return Map.copyOf(porMesa);
        }
    }
}
