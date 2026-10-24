package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Disponibilidad;
import mx.negocio360.restaurantes.dominio.Mesa;
import mx.negocio360.restaurantes.dominio.ModoServicio;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.dominio.Zona;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * El salon del restaurante (MS-01 a MS-09, QR-01, QR-02, QR-17): zonas, mesas, servicio abierto o cerrado, la
 * disponibilidad que se publica para el turista y el QR de cada mesa. El negocio y el actor salen del contexto y cada
 * operacion exige su permiso, tambien las de lectura (AZ-04).
 *
 * <p>El estado de una mesa no se guarda: se deriva de si tiene una cuenta sin pagar (MS-02), que este servicio pregunta a
 * {@link OcupacionDeMesas}. La disponibilidad se recalcula despues de cada cambio y solo se publica
 * {@code negocio.disponibilidad_cambiada} cuando {@code (libres, total, abierto)} difiere de lo ultimo publicado (MS-06).
 */
public final class SalonServicio {

    /** Tipo del evento de disponibilidad (C-01). */
    public static final String TIPO_DISPONIBILIDAD = "negocio.disponibilidad_cambiada";
    /** Origen de todos los eventos de Restaurantes. */
    public static final String ORIGEN = "restaurantes";
    /** Cuanto dura la pausa del QR de una mesa (QR-17). */
    static final Duration PAUSA_DEL_QR = Duration.ofHours(1);

    private final RepositorioSalon salon;
    private final OcupacionDeMesas ocupacion;
    private final PublicadorEventos eventos;
    private final GeneradorDeTokens tokens;
    private final String baseDelQr;
    private final Clock reloj;
    private final Object cerrojo = new Object();

    /**
     * Arma el servicio.
     *
     * @param salon     almacen de zonas, mesas y servicio
     * @param ocupacion que mesas tienen una cuenta sin pagar
     * @param eventos   publicador de eventos
     * @param tokens    generador de tokens opacos del QR
     * @param baseDelQr direccion a la que se le pega el token para formar el enlace del QR (QR-01)
     * @param reloj     reloj de la plataforma
     */
    public SalonServicio(RepositorioSalon salon, OcupacionDeMesas ocupacion, PublicadorEventos eventos,
                         GeneradorDeTokens tokens, String baseDelQr, Clock reloj) {
        this.salon = Objects.requireNonNull(salon, "salon");
        this.ocupacion = Objects.requireNonNull(ocupacion, "ocupacion");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.baseDelQr = Objects.requireNonNull(baseDelQr, "baseDelQr");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ---- zonas ----

    /**
     * Crea una zona (MS-01). Exige {@code restaurantes.salon.configurar}.
     *
     * @throws RestaurantesException NOMBRE_INVALIDO, CONSULTA_INVALIDA (orden) o LIMITE_EXCEDIDO si ya hay 10
     */
    public ZonaVista crearZona(ContextoNegocio contexto, Autorizacion autorizacion, String nombre, int orden) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        synchronized (cerrojo) {
            var activas = salon.zonas(contexto.negocioId()).stream().filter(Zona::activa).count();
            if (activas >= Zona.MAX_POR_NEGOCIO) {
                throw new RestaurantesException(CodigoError.LIMITE_EXCEDIDO);
            }
            var nueva = Zona.nueva(UuidV7.generar(reloj.instant()), contexto.negocioId(), nombre, orden, reloj.instant());
            salon.insertar(nueva);
            return ZonaVista.de(nueva);
        }
    }

    /**
     * Da de baja una zona sin mesas activas (MS-07). Exige {@code restaurantes.salon.configurar}.
     *
     * @throws RestaurantesException ZONA_NO_ENCONTRADA, ZONA_CON_MESAS o CONCURRENCIA
     */
    public void darDeBajaZona(ContextoNegocio contexto, Autorizacion autorizacion, UUID zonaId) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        synchronized (cerrojo) {
            var zona = zonaActiva(contexto, zonaId);
            var conMesas = salon.mesas(contexto.negocioId()).stream()
                    .anyMatch(m -> m.activa() && m.zonaId().equals(zona.id()));
            if (conMesas) {
                throw new RestaurantesException(CodigoError.ZONA_CON_MESAS);
            }
            if (!salon.reemplazar(zona.conBaja(), zona.version())) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
        }
    }

    // ---- mesas ----

    /**
     * Crea una mesa con su QR (MS-01, QR-01). Exige {@code restaurantes.salon.configurar}.
     *
     * @throws RestaurantesException ZONA_NO_ENCONTRADA, NOMBRE_INVALIDO, CAPACIDAD_INVALIDA, MESA_DUPLICADA o LIMITE_EXCEDIDO
     *                               si ya hay 60
     */
    public MesaVista crearMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID zonaId, String etiqueta,
                               int capacidad) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        MesaVista vista;
        synchronized (cerrojo) {
            var zona = zonaActiva(contexto, zonaId);
            var activas = salon.mesas(contexto.negocioId()).stream().filter(Mesa::activa).toList();
            if (activas.size() >= Mesa.MAX_POR_NEGOCIO) {
                throw new RestaurantesException(CodigoError.LIMITE_EXCEDIDO);
            }
            var ahora = reloj.instant();
            var nueva = Mesa.nueva(UuidV7.generar(ahora), contexto.negocioId(), zona.id(), etiqueta, capacidad,
                    tokens.nuevo(), ahora);
            exigirEtiquetaLibre(activas, nueva.etiquetaNormalizada(), null);
            salon.insertar(nueva);
            vista = MesaVista.de(nueva, null, ahora);
        }
        recalcularDisponibilidad(contexto.negocioId());
        return vista;
    }

    /**
     * Cambia zona, etiqueta y capacidad con la version que se vio (MS-01). Exige {@code restaurantes.salon.configurar}.
     *
     * @throws RestaurantesException MESA_NO_ENCONTRADA, ZONA_NO_ENCONTRADA, NOMBRE_INVALIDO, CAPACIDAD_INVALIDA,
     *                               MESA_DUPLICADA o CONCURRENCIA
     */
    public MesaVista editarMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId, long version, UUID zonaId,
                                String etiqueta, int capacidad) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        synchronized (cerrojo) {
            var actual = mesaActiva(contexto, mesaId);
            if (actual.version() != version) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
            var zona = zonaActiva(contexto, zonaId);
            var ahora = reloj.instant();
            var nueva = actual.editada(zona.id(), etiqueta, capacidad, ahora);
            exigirEtiquetaLibre(salon.mesas(contexto.negocioId()).stream().filter(Mesa::activa).toList(),
                    nueva.etiquetaNormalizada(), actual.id());
            if (!salon.reemplazar(nueva, actual.version())) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
            return MesaVista.de(nueva, ocupacion.cuentasNoPagadas(contexto.negocioId()).get(nueva.id()), ahora);
        }
    }

    /**
     * Pone o quita la marca manual de bloqueo de una mesa (MS-02). Exige {@code restaurantes.servicio.operar}.
     *
     * @throws RestaurantesException MESA_NO_ENCONTRADA o CONCURRENCIA
     */
    public MesaVista bloquearMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId, boolean bloquear) {
        autorizacion.exigir(PermisosDeRestaurantes.SERVICIO_OPERAR);
        MesaVista vista;
        synchronized (cerrojo) {
            var actual = mesaActiva(contexto, mesaId);
            var ahora = reloj.instant();
            var nueva = actual;
            if (actual.bloqueada() != bloquear) {
                nueva = actual.conBloqueo(bloquear, ahora);
                if (!salon.reemplazar(nueva, actual.version())) {
                    throw new RestaurantesException(CodigoError.CONCURRENCIA);
                }
            }
            vista = MesaVista.de(nueva, ocupacion.cuentasNoPagadas(contexto.negocioId()).get(nueva.id()), ahora);
        }
        recalcularDisponibilidad(contexto.negocioId());
        return vista;
    }

    /**
     * Da de baja una mesa sin cuenta pendiente; su QR deja de servir (MS-07, MS-09). Exige
     * {@code restaurantes.salon.configurar}.
     *
     * @throws RestaurantesException MESA_NO_ENCONTRADA, MESA_OCUPADA o CONCURRENCIA
     */
    public void darDeBajaMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        synchronized (cerrojo) {
            var actual = mesaActiva(contexto, mesaId);
            if (ocupacion.cuentasNoPagadas(contexto.negocioId()).containsKey(actual.id())) {
                throw new RestaurantesException(CodigoError.MESA_OCUPADA);
            }
            if (!salon.reemplazar(actual.conBaja(reloj.instant()), actual.version())) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
        }
        recalcularDisponibilidad(contexto.negocioId());
    }

    // ---- servicio ----

    /** Abre el servicio (MS-05). Exige {@code restaurantes.servicio.operar}. */
    public DisponibilidadVista abrirServicio(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeRestaurantes.SERVICIO_OPERAR);
        synchronized (cerrojo) {
            var servicio = salon.servicio(contexto.negocioId());
            if (!servicio.abierto()) {
                salon.guardar(servicio.conAbierto(true));
            }
        }
        return DisponibilidadVista.de(recalcularDisponibilidad(contexto.negocioId()));
    }

    /**
     * Cierra el servicio (MS-05). Con cuentas abiertas exige ademas permiso de gerente
     * ({@code restaurantes.linea.anular_preparada}) y un motivo. Exige {@code restaurantes.servicio.operar}.
     *
     * @param motivo obligatorio solo si hay cuentas sin pagar
     * @throws RestaurantesException MOTIVO_REQUERIDO
     */
    public DisponibilidadVista cerrarServicio(ContextoNegocio contexto, Autorizacion autorizacion, String motivo) {
        autorizacion.exigir(PermisosDeRestaurantes.SERVICIO_OPERAR);
        if (!ocupacion.cuentasNoPagadas(contexto.negocioId()).isEmpty()) {
            autorizacion.exigir(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
            if (motivo == null || motivo.isBlank()) {
                throw new RestaurantesException(CodigoError.MOTIVO_REQUERIDO);
            }
        }
        synchronized (cerrojo) {
            var servicio = salon.servicio(contexto.negocioId());
            if (servicio.abierto()) {
                salon.guardar(servicio.conAbierto(false));
            }
        }
        return DisponibilidadVista.de(recalcularDisponibilidad(contexto.negocioId()));
    }

    /** Cambia el modo de servicio (MS-08). Exige {@code restaurantes.salon.configurar}. */
    public void configurarModo(ContextoNegocio contexto, Autorizacion autorizacion, ModoServicio modo) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        synchronized (cerrojo) {
            salon.guardar(salon.servicio(contexto.negocioId()).conModo(Objects.requireNonNull(modo, "modo")));
        }
    }

    /** Enciende o apaga los pedidos por QR de todo el local (QR-17). Exige {@code restaurantes.servicio.operar}. */
    public void habilitarQr(ContextoNegocio contexto, Autorizacion autorizacion, boolean habilitado) {
        autorizacion.exigir(PermisosDeRestaurantes.SERVICIO_OPERAR);
        synchronized (cerrojo) {
            salon.guardar(salon.servicio(contexto.negocioId()).conQrHabilitado(habilitado));
        }
    }

    // ---- consulta ----

    /** Todo el salon: servicio, disponibilidad, zonas y mesas con su estado derivado (MS-02). Exige {@code restaurantes.salon.ver}. */
    public SalonVista salon(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_VER);
        var negocio = contexto.negocioId();
        var ahora = reloj.instant();
        var servicio = salon.servicio(negocio);
        var cuentas = ocupacion.cuentasNoPagadas(negocio);
        var zonas = salon.zonas(negocio).stream().filter(Zona::activa)
                .sorted(Comparator.comparingInt(Zona::orden).thenComparing(Zona::nombre)).toList();
        var ordenDeZona = new LinkedHashMap<UUID, Integer>();
        zonas.forEach(z -> ordenDeZona.put(z.id(), ordenDeZona.size()));
        var mesas = salon.mesas(negocio).stream().filter(m -> m.activa() && ordenDeZona.containsKey(m.zonaId()))
                .sorted(Comparator.comparing((Mesa m) -> ordenDeZona.get(m.zonaId()))
                        .thenComparing((a, b) -> comparaNatural(a.etiqueta(), b.etiqueta())))
                .map(m -> MesaVista.de(m, cuentas.get(m.id()), ahora)).toList();
        return new SalonVista(servicio.abierto(), servicio.qrHabilitado(), servicio.modo(),
                DisponibilidadVista.de(calcular(negocio)), zonas.stream().map(ZonaVista::de).toList(), mesas);
    }

    // ---- disponibilidad (MS-04, MS-06) ----

    /**
     * Recalcula {@code (libres, total, abierto)} y, solo si cambio respecto de lo ultimo publicado, publica
     * {@code negocio.disponibilidad_cambiada} con el estado completo (MS-06). Lo llaman los cambios del salon y de las
     * cuentas.
     *
     * @return la disponibilidad actual
     */
    public Disponibilidad recalcularDisponibilidad(UUID negocioId) {
        synchronized (cerrojo) {
            var actual = calcular(negocioId);
            var servicio = salon.servicio(negocioId);
            if (!actual.equals(servicio.publicada())) {
                salon.guardar(servicio.conPublicada(actual));
                var ahora = reloj.instant();
                var datos = new LinkedHashMap<String, Object>();
                datos.put("tipo", "MESAS");
                datos.put("libres", actual.libres());
                datos.put("total", actual.total());
                datos.put("abierto", actual.abierto());
                datos.put("actualizado_en", ahora.toString());
                eventos.publicar(new SobreEvento(UuidV7.generar(ahora), TIPO_DISPONIBILIDAD, 1, negocioId, ORIGEN, ahora,
                        UuidV7.generar(ahora), null, datos));
            }
            return actual;
        }
    }

    private Disponibilidad calcular(UUID negocioId) {
        var cuentas = ocupacion.cuentasNoPagadas(negocioId);
        var contables = salon.mesas(negocioId).stream().filter(m -> m.activa() && !m.bloqueada()).toList();
        var libres = contables.stream().filter(m -> !cuentas.containsKey(m.id())).count();
        return new Disponibilidad((int) libres, contables.size(), salon.servicio(negocioId).abierto());
    }

    // ---- QR (QR-01, QR-02, QR-17) ----

    /** El enlace del QR de una mesa. Exige {@code restaurantes.salon.configurar}. */
    public String qrDeMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        return baseDelQr + mesaActiva(contexto, mesaId).qrToken();
    }

    /**
     * Cambia el token del QR de una mesa; el anterior deja de servir de inmediato (QR-02). Exige
     * {@code restaurantes.salon.configurar}.
     *
     * @return el enlace nuevo
     */
    public String regenerarQrDeMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId) {
        autorizacion.exigir(PermisosDeRestaurantes.SALON_CONFIGURAR);
        synchronized (cerrojo) {
            var actual = mesaActiva(contexto, mesaId);
            var nueva = actual.conToken(tokens.nuevo(), reloj.instant());
            if (!salon.reemplazar(nueva, actual.version())) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
            return baseDelQr + nueva.qrToken();
        }
    }

    /**
     * Pausa el QR de una mesa una hora o lo reanuda (QR-17). Exige {@code restaurantes.servicio.operar}.
     *
     * @throws RestaurantesException MESA_NO_ENCONTRADA o CONCURRENCIA
     */
    public MesaVista pausarQrDeMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId, boolean pausar) {
        autorizacion.exigir(PermisosDeRestaurantes.SERVICIO_OPERAR);
        synchronized (cerrojo) {
            var actual = mesaActiva(contexto, mesaId);
            var ahora = reloj.instant();
            var nueva = actual.conQrPausadoHasta(pausar ? ahora.plus(PAUSA_DEL_QR) : null, ahora);
            if (!salon.reemplazar(nueva, actual.version())) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
            return MesaVista.de(nueva, ocupacion.cuentasNoPagadas(contexto.negocioId()).get(nueva.id()), ahora);
        }
    }

    // ---- ayudas ----

    private Zona zonaActiva(ContextoNegocio contexto, UUID zonaId) {
        return salon.zona(contexto.negocioId(), zonaId).filter(Zona::activa)
                .orElseThrow(() -> new RestaurantesException(CodigoError.ZONA_NO_ENCONTRADA));
    }

    private Mesa mesaActiva(ContextoNegocio contexto, UUID mesaId) {
        return salon.mesa(contexto.negocioId(), mesaId).filter(Mesa::activa)
                .orElseThrow(() -> new RestaurantesException(CodigoError.MESA_NO_ENCONTRADA));
    }

    private static void exigirEtiquetaLibre(List<Mesa> activas, String normalizada, UUID salvo) {
        var repetida = activas.stream().anyMatch(m -> !m.id().equals(salvo) && m.etiquetaNormalizada().equals(normalizada));
        if (repetida) {
            throw new RestaurantesException(CodigoError.MESA_DUPLICADA);
        }
    }

    /** Compara etiquetas como las leeria una persona: «M2» antes que «M10». */
    static int comparaNatural(String a, String b) {
        var i = 0;
        var j = 0;
        while (i < a.length() && j < b.length()) {
            if (Character.isDigit(a.charAt(i)) && Character.isDigit(b.charAt(j))) {
                var finA = i;
                while (finA < a.length() && Character.isDigit(a.charAt(finA))) {
                    finA++;
                }
                var finB = j;
                while (finB < b.length() && Character.isDigit(b.charAt(finB))) {
                    finB++;
                }
                var numA = new java.math.BigInteger(a.substring(i, finA));
                var numB = new java.math.BigInteger(b.substring(j, finB));
                var comparacion = numA.compareTo(numB);
                if (comparacion != 0) {
                    return comparacion;
                }
                i = finA;
                j = finB;
            } else {
                var comparacion = Character.compare(Character.toLowerCase(a.charAt(i)), Character.toLowerCase(b.charAt(j)));
                if (comparacion != 0) {
                    return comparacion;
                }
                i++;
                j++;
            }
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    /** Un resumen para registros y mensajes, sin datos de la peticion. */
    @Override
    public String toString() {
        return "SalonServicio";
    }

    /** Las cuentas sin pagar del negocio por mesa, para quien arma vistas propias. */
    Map<UUID, ResumenDeCuenta> cuentas(UUID negocioId) {
        return ocupacion.cuentasNoPagadas(negocioId);
    }
}
