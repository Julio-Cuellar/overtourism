package mx.negocio360.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.IdentidadServicio;
import mx.negocio360.identidad.PreregistroCreado;
import mx.negocio360.identidad.SesionIniciada;
import mx.negocio360.identidad.SolicitudDeRegistro;
import mx.negocio360.identidad.dominio.Membresia;
import mx.negocio360.identidad.dominio.Telefono;
import mx.negocio360.identidad.infraestructura.EscuchaDeNegocios;
import mx.negocio360.identidad.infraestructura.PublicadorEventosEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioMembresiasEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioNegociosEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioPreregistrosEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioRolesEnMemoria;
import mx.negocio360.identidad.infraestructura.SolicitudesDeNegocioEnMemoria;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.aplicacion.puertos.CanalNoDisponibleException;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.infraestructura.LimiteDeTasaEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioCodigosEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioCuentasEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioSesionesEnMemoria;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Servicio de Identidad armado con adaptadores en memoria, un reloj manual y un canal que graba los codigos. */
public final class Escenario {

    public static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");
    public static final String TEL_A = "222 111 0001";
    public static final String TEL_A_E164 = "+522221110001";
    public static final String TEL_B = "+52 222 111 0002";
    public static final String TEL_B_E164 = "+522221110002";
    public static final String IP = "189.203.10.57";
    public static final String DISPOSITIVO = "Chrome en Windows";
    /** Un caracter mas que el maximo del nombre visible; es literal porque las anotaciones exigen constantes. */
    public static final String NOMBRE_DE_81 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    /** Permisos de Identidad (reales) y los de Clientes (copiados: un modulo no ve el catalogo del otro en sus pruebas). */
    public static final List<PermisoDeclarado> CATALOGO = Stream.concat(new PermisosDeIdentidad().permisos().stream(),
            Stream.of("clientes.sugerir", "clientes.lista.ver", "clientes.panorama.ver", "clientes.ficha.ver",
                    "clientes.cliente.registrar", "clientes.cliente.editar", "clientes.consentimiento.registrar",
                    "clientes.aviso.publicar", "clientes.cliente.anonimizar", "clientes.arco.gestionar",
                    "clientes.datos.exportar", "negocios.perfil.ver", "negocios.perfil.editar", "negocios.sucursal.gestionar", "negocios.horario.editar", "negocios.negocio.publicar", "negocios.disponibilidad.editar", "negocios.negocio.pausar", "negocios.negocio.cerrar", "negocios.empleado.ver", "negocios.empleado.registrar", "negocios.empleado.editar", "negocios.empleado.baja", "negocios.resena.ver", "negocios.resena.responder", "restaurantes.menu.ver", "restaurantes.menu.editar", "restaurantes.menu.agotar", "restaurantes.receta.ver", "restaurantes.receta.editar", "restaurantes.costo.ver", "restaurantes.salon.ver", "restaurantes.salon.configurar", "restaurantes.servicio.operar", "restaurantes.cuenta.ver", "restaurantes.cuenta.abrir", "restaurantes.cuenta.cancelar", "restaurantes.linea.anular", "restaurantes.linea.anular_preparada", "restaurantes.comanda.ver", "restaurantes.comanda.preparar", "restaurantes.comanda.entregar", "restaurantes.consumo.ver")
                    .map(codigo -> new PermisoDeclarado(codigo, "Permiso de prueba", false))).toList();

    public final RelojMutable reloj = new RelojMutable(AHORA);
    public final CanalGrabador canal = new CanalGrabador();
    public final RepositorioCuentasEnMemoria cuentas = new RepositorioCuentasEnMemoria();
    public final RepositorioCodigosEnMemoria codigos = new RepositorioCodigosEnMemoria();
    public final RepositorioPreregistrosEnMemoria preregistros = new RepositorioPreregistrosEnMemoria();
    public final RepositorioSesionesEnMemoria sesiones = new RepositorioSesionesEnMemoria();
    public final LimiteDeTasaEnMemoria limite = new LimiteDeTasaEnMemoria();
    public final PoliticaDeAcceso politica = PoliticaDeAcceso.porDefecto();
    public final RepositorioMembresiasEnMemoria membresias = new RepositorioMembresiasEnMemoria();
    public final RepositorioNegociosEnMemoria negocios = new RepositorioNegociosEnMemoria();
    public final SolicitudesDeNegocioEnMemoria solicitudes = new SolicitudesDeNegocioEnMemoria();
    public final RepositorioRolesEnMemoria roles = RepositorioRolesEnMemoria.de(CATALOGO);
    public final PublicadorEventosEnMemoria eventos = new PublicadorEventosEnMemoria();
    public final EquipoServicio equipo = new EquipoServicio(membresias, negocios, solicitudes, roles, sesiones, cuentas, eventos,
            reloj);
    public final IdentidadServicio api = servicio(canal, politica);
    public final EscuchaDeNegocios escucha = new EscuchaDeNegocios(equipo);

    /** Servicio de Identidad con estos adaptadores y ese canal y esa politica (para probar fallos y limites). */
    public IdentidadServicio servicio(CanalDeCodigos canalElegido, PoliticaDeAcceso politicaElegida) {
        return new IdentidadServicio(cuentas, codigos, preregistros, sesiones, canalElegido, limite, reloj, politicaElegida,
                equipo);
    }

    /** Lo que el modulo de negocios publicaria al crear el negocio que se le pidio (D-78). */
    public SobreEvento negocioRegistrado(UUID negocioId, UUID solicitadoPor, String nombre, String giro) {
        return new SobreEvento(UuidV7.generar(reloj.instant()), "negocio.registrado", 1, negocioId, "negocios",
                reloj.instant(), UuidV7.generar(reloj.instant()), null,
                Map.of("solicitado_por", solicitadoPor.toString(), "nombre", nombre, "giro", giro));
    }

    /**
     * La cuenta del token pide un negocio y el modulo de negocios (simulado) responde: la cuenta queda como DUENO.
     * Devuelve el id del negocio.
     */
    public UUID crearNegocio(String token, String nombre, String giro) {
        var cuentaId = api.autenticar(token).orElseThrow().cuentaId();
        var negocioId = api.solicitarNegocio(token, nombre, giro).negocioId();
        escucha.alRecibir(negocioRegistrado(negocioId, cuentaId, nombre, giro));
        return negocioId;
    }

    /** Suma a una cuenta a un negocio con un rol, como lo haran las invitaciones (IDN-3). Devuelve la membresia. */
    public Membresia agregarMiembro(UUID negocioId, UUID cuentaId, String rol) {
        var membresia = Membresia.nueva(UuidV7.generar(reloj.instant()), cuentaId, negocioId, rol, null, reloj.instant());
        return membresias.guardar(membresia);
    }

    /** Pide un codigo de acceso con ese identificador y devuelve el que llego al correo de la cuenta. */
    public String pedirCodigo(String identificador) {
        api.solicitarCodigo(identificador, IP);
        return canal.ultimoCodigo();
    }

    /** Correo de prueba derivado del telefono, para que cada telefono tenga el suyo y nunca choquen. */
    public static String correoDe(String telefono) {
        return "usuario" + telefono.replaceAll("\\D", "") + "@example.com";
    }

    /** Formulario de registro completo y valido para ese telefono (D-84: sin codigo, la persona aun no tiene cuenta). */
    public static SolicitudDeRegistro solicitud(String telefono) {
        return solicitud(telefono, "Ana");
    }

    public static SolicitudDeRegistro solicitud(String telefono, String nombre) {
        return new SolicitudDeRegistro(nombre, "López Pérez", correoDe(telefono), telefono, true);
    }

    /** Formulario de registro que ademas pide un negocio (D-79). */
    public static SolicitudDeRegistro solicitudConNegocio(String telefono, String nombreDelNegocio, String giro) {
        return new SolicitudDeRegistro("Ana", "López Pérez", correoDe(telefono), telefono, true,
                new SolicitudDeRegistro.NegocioPedido(nombreDelNegocio, giro));
    }

    /** Primer paso del registro: guarda el formulario y manda el codigo al correo. */
    public PreregistroCreado preRegistrar(String telefono, String nombre) {
        return api.registrar(solicitud(telefono, nombre), IP);
    }

    /** Los dos pasos del registro: el formulario y la verificacion con el codigo que llego al correo. */
    public SesionIniciada registrar(String telefono, String nombre) {
        var creado = preRegistrar(telefono, nombre);
        return api.verificarRegistro(creado.correo(), canal.ultimoCodigo(), DISPOSITIVO, IP);
    }

    /** Pide el codigo e inicia sesion en una cuenta que ya existe. */
    public SesionIniciada iniciar(String identificador) {
        api.solicitarCodigo(identificador, IP);
        return api.iniciarSesion(identificador, canal.ultimoCodigo(), DISPOSITIVO, IP);
    }

    /** Entra a la cuenta de ese telefono; si todavia no existe, la registra con ese nombre. */
    public SesionIniciada entrar(String telefono, String nombre) {
        var numero = Telefono.normalizar(telefono).orElseThrow();
        return cuentas.porTelefono(numero).isPresent() ? iniciar(telefono) : registrar(telefono, nombre);
    }

    /** Exige que la accion falle con IdentidadException y el codigo esperado. */
    public static IdentidadException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(IdentidadException.class);
        var excepcion = (IdentidadException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }

    /** Reloj de pruebas que solo avanza cuando la prueba lo manda. */
    public static final class RelojMutable extends Clock {

        private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
        private Instant instante;

        public RelojMutable(Instant inicial) {
            this.instante = inicial;
        }

        public void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    /** Canal que guarda lo enviado y puede simular una caida. */
    public static final class CanalGrabador implements CanalDeCodigos {

        public record Envio(String destino, String codigo) {
        }

        private final List<Envio> envios = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean caido;

        public void caido(boolean valor) {
            this.caido = valor;
        }

        public List<Envio> envios() {
            synchronized (envios) {
                return List.copyOf(envios);
            }
        }

        public String ultimoCodigo() {
            synchronized (envios) {
                return envios.get(envios.size() - 1).codigo();
            }
        }

        @Override
        public void enviar(String destino, String codigo) {
            if (caido) {
                throw new CanalNoDisponibleException("El canal de pruebas esta caido");
            }
            envios.add(new Envio(destino, codigo));
        }
    }
}
