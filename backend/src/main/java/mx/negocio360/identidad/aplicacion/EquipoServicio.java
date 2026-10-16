package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.PermisosDeIdentidad.EQUIPO_VER;
import static mx.negocio360.identidad.aplicacion.PermisosDeIdentidad.MIEMBRO_CAMBIAR_ROL;
import static mx.negocio360.identidad.aplicacion.PermisosDeIdentidad.MIEMBRO_REVOCAR;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.MiembroVista;
import mx.negocio360.identidad.NegocioPendienteVista;
import mx.negocio360.identidad.NegocioSolicitado;
import mx.negocio360.identidad.NegocioVista;
import mx.negocio360.identidad.PermisoVista;
import mx.negocio360.identidad.RolVista;
import mx.negocio360.identidad.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioMembresias;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioRoles;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioSesiones;
import mx.negocio360.identidad.aplicacion.puertos.SolicitudesDeNegocio;
import mx.negocio360.identidad.contratos.NegocioRegistradoV1;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.EstadoMembresia;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.dominio.Membresia;
import mx.negocio360.identidad.dominio.NegocioReferencia;
import mx.negocio360.identidad.dominio.NombreVisible;
import mx.negocio360.identidad.dominio.SolicitudDeNegocio;
import mx.negocio360.identidad.dominio.Rol;
import mx.negocio360.identidad.dominio.Telefono;
import mx.negocio360.identidad.eventos.MembresiaAgregada;
import mx.negocio360.identidad.eventos.MembresiaCambiada;
import mx.negocio360.identidad.eventos.MembresiaRevocada;
import mx.negocio360.identidad.eventos.NegocioRegistroSolicitado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Negocios y equipo (IDN-2, D-78). Pide negocios por evento (no los registra), reacciona al aviso de que se
 * registraron, y administra las membresias: quien actua, con que permiso y sobre quien. Los metodos son
 * {@code synchronized} porque con adaptadores en memoria la atomicidad la da este bloqueo; con Postgres la daran la
 * transaccion y el bloqueo optimista de la membresia. Cambiar o quitar a alguien y revocar sus sesiones del negocio
 * ocurre en una sola operacion (AM-06).
 *
 * <p>LIMITES QUE EXIGE LA PERSISTENCIA (IDN-1.1): con Postgres, «siempre queda un dueño activo» (AM-02) es una regla
 * entre filas y el bloqueo optimista de una fila no la protege; cada operacion que quite o degrade a un dueño debe
 * empezar con {@code SELECT ... FROM identidad.negocio_ref WHERE negocio_id = ? FOR UPDATE} y repetir el conteo.
 * Igualmente, guardar el negocio y la membresia de {@link #alNegocioRegistrado} debe ser una sola transaccion.
 */
public final class EquipoServicio {

    private static final Logger REGISTRO = LoggerFactory.getLogger(EquipoServicio.class);
    private static final String DUENO = "DUENO";

    /** La membresia activa de una cuenta en un negocio activo, con su rol. */
    public record Vinculo(Membresia membresia, Rol rol, NegocioReferencia negocio) {
    }

    /** Quien actua: su cuenta, el negocio activo de su sesion y su rol ahi. */
    public record Actor(UUID cuentaId, UUID negocioId, Rol rol) {
    }

    private final RepositorioMembresias membresias;
    private final RepositorioNegocios negocios;
    private final SolicitudesDeNegocio solicitudes;
    private final RepositorioRoles roles;
    private final RepositorioSesiones sesiones;
    private final RepositorioCuentas cuentas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public EquipoServicio(RepositorioMembresias membresias, RepositorioNegocios negocios,
                          SolicitudesDeNegocio solicitudes, RepositorioRoles roles, RepositorioSesiones sesiones,
                          RepositorioCuentas cuentas, PublicadorEventos eventos, Clock reloj) {
        this.membresias = Objects.requireNonNull(membresias, "membresias");
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.solicitudes = Objects.requireNonNull(solicitudes, "solicitudes");
        this.roles = Objects.requireNonNull(roles, "roles");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ---- negocios de una cuenta ----

    /** Negocios donde la cuenta tiene una membresia activa, con su rol en cada uno, por nombre. */
    public synchronized List<NegocioVista> negociosDe(UUID cuentaId) {
        return membresias.deCuenta(cuentaId).stream().filter(Membresia::activa)
                .map(m -> negocios.porId(m.negocioId()).filter(n -> n.estado() == NegocioReferencia.Estado.ACTIVO)
                        .map(n -> new NegocioVista(n.negocioId(), n.nombre(), m.rolCodigo())))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(NegocioVista::nombre).thenComparing(NegocioVista::id)).toList();
    }

    /** Negocios que la cuenta pidio y el modulo de negocios aun no registra, del mas antiguo al mas reciente. */
    public synchronized List<NegocioPendienteVista> pendientesDe(UUID cuentaId) {
        return solicitudes.deCuenta(cuentaId).stream().map(s -> new NegocioPendienteVista(s.negocioId(), s.nombre(),
                s.giro().name(), NegocioPendienteVista.PENDIENTE)).toList();
    }

    /** La membresia activa de la cuenta en ese negocio, si el negocio sigue activo y el rol existe. */
    public synchronized Optional<Vinculo> vinculo(UUID cuentaId, UUID negocioId) {
        if (cuentaId == null || negocioId == null) {
            return Optional.empty();
        }
        return membresias.porCuentaYNegocio(cuentaId, negocioId).filter(Membresia::activa).flatMap(m ->
                negocios.porId(negocioId).filter(n -> n.estado() == NegocioReferencia.Estado.ACTIVO).flatMap(n ->
                        roles.porCodigo(m.rolCodigo()).map(r -> new Vinculo(m, r, n))));
    }

    // ---- alta de negocio por eventos (D-78) ----

    /**
     * Pide al modulo de negocios que registre un negocio: publica {@code negocio.registro_solicitado}. No crea el negocio
     * ni la membresia; el id lo asigna Identidad y viaja en el sobre.
     */
    public synchronized NegocioSolicitado solicitarNegocio(UUID cuentaId, String nombre, String giro) {
        var tipo = Giro.de(giro).orElseThrow(() -> new IdentidadException(CodigoError.GIRO_INVALIDO));
        var limpio = NombreVisible.normalizar(nombre)
                .orElseThrow(() -> new IdentidadException(CodigoError.NOMBRE_INVALIDO));
        var ahora = reloj.instant();
        var negocioId = UuidV7.generar(ahora);
        var solicitud = new NegocioRegistroSolicitado(cuentaId, limpio, tipo);
        solicitudes.guardar(new SolicitudDeNegocio(negocioId, cuentaId, limpio, tipo, ahora));
        eventos.publicar(FabricaSobres.de(negocioId, ahora, UuidV7.generar(ahora), null, NegocioRegistroSolicitado.TIPO,
                NegocioRegistroSolicitado.VERSION, solicitud.datos()));
        return new NegocioSolicitado(negocioId);
    }

    /**
     * Reacciona a {@code negocio.registrado}: guarda el negocio de referencia y deja a quien lo pidio como DUENO. Solo
     * acepta el aviso de un negocio que Identidad pidio y a nombre de quien lo pidio (la solicitud pendiente), y se
     * consume al aplicarlo. Es idempotente (R3): una entrega repetida no cambia nada. Lo que no puede aplicar lo ignora
     * sin error, pero deja un aviso en el registro con el negocio y el evento (nunca datos personales) para reconciliar.
     */
    public synchronized void alNegocioRegistrado(NegocioRegistradoV1 aviso) {
        var yaAplicado = membresias.porCuentaYNegocio(aviso.solicitadoPor(), aviso.negocioId())
                .filter(m -> m.estado() != EstadoMembresia.REVOCADA).isPresent()
                || negocios.porId(aviso.negocioId()).isPresent();
        if (yaAplicado) {
            return;
        }
        var pedido = solicitudes.porNegocio(aviso.negocioId());
        if (pedido.isEmpty() || !pedido.get().cuentaId().equals(aviso.solicitadoPor())) {
            ignorar("Identidad no pidio ese negocio a nombre de esa cuenta", aviso);
            return;
        }
        var giro = Giro.de(aviso.giro());
        var nombre = NombreVisible.normalizar(aviso.nombre());
        var cuenta = cuentas.porId(aviso.solicitadoPor()).filter(c -> c.estado() == EstadoCuenta.ACTIVA);
        if (giro.isEmpty() || nombre.isEmpty() || cuenta.isEmpty()) {
            ignorar("el giro, el nombre o la cuenta no se pueden aplicar", aviso);
            return;
        }
        var ahora = reloj.instant();
        negocios.guardar(new NegocioReferencia(aviso.negocioId(), nombre.get(), giro.get(),
                NegocioReferencia.Estado.ACTIVO));
        membresias.guardar(Membresia.nueva(UuidV7.generar(ahora), aviso.solicitadoPor(), aviso.negocioId(), DUENO, null,
                ahora));
        solicitudes.eliminar(aviso.negocioId());
        eventos.publicar(FabricaSobres.de(aviso.negocioId(), ahora, aviso.correlacionId(), aviso.eventoId(),
                MembresiaAgregada.TIPO, MembresiaAgregada.VERSION,
                new MembresiaAgregada(aviso.solicitadoPor(), DUENO, MembresiaAgregada.Origen.DUENO_INICIAL).datos()));
    }

    private static void ignorar(String motivo, NegocioRegistradoV1 aviso) {
        REGISTRO.warn("Aviso negocio.registrado ignorado ({}): negocio {} evento {}", motivo, aviso.negocioId(),
                aviso.eventoId());
    }

    // ---- consultas del negocio activo ----

    public synchronized List<MiembroVista> equipo(Actor actor) {
        exigir(actor, EQUIPO_VER);
        return membresias.deNegocio(actor.negocioId()).stream().filter(m -> m.estado() != EstadoMembresia.REVOCADA)
                .sorted(Comparator.comparing(Membresia::creadaEn).thenComparing(Membresia::id))
                .map(this::miembro).flatMap(Optional::stream).toList();
    }

    public synchronized List<RolVista> roles(Actor actor) {
        exigir(actor, EQUIPO_VER);
        var giro = negocios.porId(actor.negocioId())
                .orElseThrow(() -> new IdentidadException(CodigoError.NEGOCIO_NO_DISPONIBLE)).giro();
        return roles.plantilla(giro).stream()
                .map(r -> new RolVista(r.codigo(), r.nombre(), r.permisos().stream().sorted().toList())).toList();
    }

    public synchronized List<PermisoVista> permisos(Actor actor) {
        exigir(actor, EQUIPO_VER);
        return roles.permisos().stream().sorted(Comparator.comparing(p -> p.codigo()))
                .map(p -> new PermisoVista(p.codigo(), p.modulo(), p.descripcion(), p.sensible())).toList();
    }

    // ---- administrar al equipo ----

    public synchronized MiembroVista cambiarRol(Actor actor, UUID destinoId, String rolCodigo) {
        exigir(actor, MIEMBRO_CAMBIAR_ROL);
        exigirOtraPersona(actor, destinoId);
        var destino = miembroDe(actor, destinoId);
        exigirPoderTocar(actor, destino);
        var giro = negocios.porId(actor.negocioId())
                .orElseThrow(() -> new IdentidadException(CodigoError.NEGOCIO_NO_DISPONIBLE)).giro();
        var nuevo = roles.plantilla(giro).stream().filter(r -> r.codigo().equals(rolCodigo)).findFirst()
                .orElseThrow(() -> new IdentidadException(CodigoError.ROL_NO_PERMITIDO));
        if (!actor.rol().permisos().containsAll(nuevo.permisos())) {
            throw new IdentidadException(CodigoError.ROL_NO_PERMITIDO);
        }
        if (destino.rolCodigo().equals(nuevo.codigo())) {
            return vista(destino);
        }
        if (DUENO.equals(destino.rolCodigo())) {
            exigirOtroDueno(destino);
        }
        var ahora = reloj.instant();
        var cambiada = membresias.guardar(destino.conRol(nuevo.codigo(), ahora));
        publicar(actor.negocioId(), ahora, null, MembresiaCambiada.TIPO, MembresiaCambiada.VERSION,
                new MembresiaCambiada(destinoId, destino.rolCodigo(), nuevo.codigo(), cambiada.estado()).datos());
        return vista(cambiada);
    }

    public synchronized MiembroVista suspender(Actor actor, UUID destinoId) {
        exigir(actor, MIEMBRO_REVOCAR);
        exigirOtraPersona(actor, destinoId);
        var destino = miembroDe(actor, destinoId);
        exigirPoderTocar(actor, destino);
        if (destino.estado() == EstadoMembresia.SUSPENDIDA) {
            return vista(destino);
        }
        exigirOtroDueno(destino);
        var ahora = reloj.instant();
        var suspendida = membresias.guardar(destino.suspendida(ahora));
        revocarSesiones(destinoId, actor.negocioId(), ahora);
        publicar(actor.negocioId(), ahora, null, MembresiaCambiada.TIPO, MembresiaCambiada.VERSION,
                new MembresiaCambiada(destinoId, destino.rolCodigo(), destino.rolCodigo(), suspendida.estado()).datos());
        return vista(suspendida);
    }

    public synchronized MiembroVista reactivar(Actor actor, UUID destinoId) {
        exigir(actor, MIEMBRO_REVOCAR);
        exigirOtraPersona(actor, destinoId);
        var destino = miembroDe(actor, destinoId);
        exigirPoderTocar(actor, destino);
        if (destino.activa()) {
            return vista(destino);
        }
        var ahora = reloj.instant();
        var reactivada = membresias.guardar(destino.reactivada(ahora));
        publicar(actor.negocioId(), ahora, null, MembresiaCambiada.TIPO, MembresiaCambiada.VERSION,
                new MembresiaCambiada(destinoId, destino.rolCodigo(), destino.rolCodigo(), reactivada.estado()).datos());
        return vista(reactivada);
    }

    public synchronized void revocar(Actor actor, UUID destinoId) {
        exigir(actor, MIEMBRO_REVOCAR);
        exigirOtraPersona(actor, destinoId);
        var destino = miembroDe(actor, destinoId);
        exigirPoderTocar(actor, destino);
        exigirOtroDueno(destino);
        quitar(destino, MembresiaRevocada.Motivo.REVOCADA);
    }

    public synchronized void salir(Actor actor) {
        var propia = membresias.porCuentaYNegocio(actor.cuentaId(), actor.negocioId())
                .filter(Membresia::activa).orElseThrow(() -> new IdentidadException(CodigoError.NO_ES_MIEMBRO));
        exigirOtroDueno(propia);
        quitar(propia, MembresiaRevocada.Motivo.SALIO);
    }

    // ---- reglas ----

    private static void exigir(Actor actor, String permiso) {
        if (!actor.rol().permisos().contains(permiso)) {
            throw new IdentidadException(CodigoError.PERMISO_DENEGADO);
        }
    }

    /** AM-07: nadie administra su propia membresia desde aqui; salir es una accion aparte. */
    private static void exigirOtraPersona(Actor actor, UUID destinoId) {
        if (actor.cuentaId().equals(destinoId)) {
            throw new IdentidadException(CodigoError.OPERACION_SOBRE_SI_MISMO);
        }
    }

    private Membresia miembroDe(Actor actor, UUID destinoId) {
        if (destinoId == null) {
            throw new IdentidadException(CodigoError.NO_ES_MIEMBRO);
        }
        return membresias.porCuentaYNegocio(destinoId, actor.negocioId())
                .filter(m -> m.estado() != EstadoMembresia.REVOCADA)
                .orElseThrow(() -> new IdentidadException(CodigoError.NO_ES_MIEMBRO));
    }

    /** AM-05: un administrador no cambia, suspende ni revoca a un dueño. */
    private static void exigirPoderTocar(Actor actor, Membresia destino) {
        if (DUENO.equals(destino.rolCodigo()) && !DUENO.equals(actor.rol().codigo())) {
            throw new IdentidadException(CodigoError.ROL_NO_PERMITIDO);
        }
    }

    /** AM-02: un negocio conserva al menos un dueño activo. Solo importa si quien se va o baja es un dueño activo. */
    private void exigirOtroDueno(Membresia destino) {
        if (!DUENO.equals(destino.rolCodigo()) || !destino.activa()) {
            return;
        }
        var duenosActivos = membresias.deNegocio(destino.negocioId()).stream()
                .filter(m -> DUENO.equals(m.rolCodigo()) && m.activa()).count();
        if (duenosActivos <= 1) {
            throw new IdentidadException(CodigoError.ULTIMO_DUENO);
        }
    }

    private void quitar(Membresia destino, MembresiaRevocada.Motivo motivo) {
        var ahora = reloj.instant();
        membresias.guardar(destino.revocada(ahora));
        revocarSesiones(destino.cuentaId(), destino.negocioId(), ahora);
        publicar(destino.negocioId(), ahora, null, MembresiaRevocada.TIPO, MembresiaRevocada.VERSION,
                new MembresiaRevocada(destino.cuentaId(), motivo).datos());
    }

    /** AM-06: las sesiones de esa persona en ese negocio caen en la misma operacion; las de otros negocios no. */
    private void revocarSesiones(UUID cuentaId, UUID negocioId, Instant ahora) {
        sesiones.vigentesDe(cuentaId, ahora).stream().filter(s -> negocioId.equals(s.negocioId()))
                .forEach(s -> sesiones.guardar(s.revocada(ahora)));
    }

    private void publicar(UUID negocioId, Instant ahora, UUID causaId, String tipo, int version,
                          Map<String, Object> datos) {
        eventos.publicar(FabricaSobres.de(negocioId, ahora, UuidV7.generar(ahora), causaId, tipo, version, datos));
    }

    private Optional<MiembroVista> miembro(Membresia membresia) {
        return cuentas.porId(membresia.cuentaId()).map(c -> new MiembroVista(c.id(), c.nombre(), c.apellidos(),
                Telefono.enmascarar(c.telefono()), membresia.rolCodigo(), membresia.estado().name()));
    }

    private MiembroVista vista(Membresia membresia) {
        return miembro(membresia).orElseThrow(() -> new IdentidadException(CodigoError.NO_ES_MIEMBRO));
    }
}
