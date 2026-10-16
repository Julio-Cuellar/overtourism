package mx.negocio360.identidad.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.identidad.CuentaVista;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.NegocioPendienteVista;
import mx.negocio360.identidad.NegocioSolicitado;
import mx.negocio360.identidad.NegocioVista;
import mx.negocio360.identidad.PermisoVista;
import mx.negocio360.identidad.PreregistroCreado;
import mx.negocio360.identidad.RolVista;
import mx.negocio360.identidad.PerfilVista;
import mx.negocio360.identidad.SesionIniciada;
import mx.negocio360.identidad.SolicitudDeRegistro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de acceso (D-72): codigo, registro en dos pasos (D-84), sesion, perfil, negocio activo, roles, cierre y nombre. Solo traduce HTTP a {@link IdentidadApi}.
 * El token viaja unicamente en {@code Authorization: Bearer} (SI-05): no hay cookies. Ninguna ruta recibe el negocio
 * ni el actor en el cuerpo (AZ-05). Un cuerpo que no sea JSON se rechaza al leerlo (415) y los errores salen en
 * {@link ErroresIdentidadAdvice}. La IP es la de la conexion: no se confia en encabezados reenviados.
 *
 * <p>Los DTO ocultan sus campos en {@code toString} porque Spring MVC escribe el contenido de peticiones y
 * respuestas en los registros de nivel DEBUG (SI-15).
 */
@RestController
public class AuthController {

    private static final Pattern BEARER = Pattern.compile("(?i)^Bearer\\s+(\\S+)$");

    private final IdentidadApi identidad;

    public AuthController(IdentidadApi identidad) {
        this.identidad = identidad;
    }

    @PostMapping("/auth/codigo")
    public ResponseEntity<Void> solicitarCodigo(@RequestBody SolicitudCodigo cuerpo, HttpServletRequest peticion) {
        identidad.solicitarCodigo(cuerpo.identificador(), peticion.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    /** D-84, primer paso: guarda el formulario como pre-registro y manda un codigo al correo. La cuenta aun no existe. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RespuestaPreregistro registrar(@RequestBody SolicitudRegistro cuerpo, HttpServletRequest peticion) {
        var negocio = cuerpo.negocio() == null ? null
                : new SolicitudDeRegistro.NegocioPedido(cuerpo.negocio().nombre(), cuerpo.negocio().giro());
        var solicitud = new SolicitudDeRegistro(cuerpo.nombre(), cuerpo.apellidos(), cuerpo.correo(), cuerpo.telefono(),
                cuerpo.aceptaAviso(), negocio);
        return RespuestaPreregistro.de(identidad.registrar(solicitud, peticion.getRemoteAddr()));
    }

    /** D-84, segundo paso: con el codigo del correo se crea la cuenta, con el correo verificado, y se abre la sesion. */
    @PostMapping("/registro/verificacion")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaSesion verificarRegistro(@RequestBody SolicitudVerificacion cuerpo,
                                             @RequestHeader(value = "User-Agent", required = false) String agente,
                                             HttpServletRequest peticion) {
        return RespuestaSesion.de(identidad.verificarRegistro(cuerpo.correo(), cuerpo.codigo(), agente,
                peticion.getRemoteAddr()));
    }

    /** Otro codigo para el pre-registro de ese correo. Responde igual haya o no pre-registro (SI-02). */
    @PostMapping("/registro/reenvio")
    public ResponseEntity<Void> reenviarCodigoDeRegistro(@RequestBody SolicitudReenvio cuerpo,
                                                         HttpServletRequest peticion) {
        identidad.reenviarCodigoDeRegistro(cuerpo.correo(), peticion.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/sesion")
    public RespuestaSesion iniciarSesion(@RequestBody SolicitudSesion cuerpo,
                                         @RequestHeader(value = "User-Agent", required = false) String agente,
                                         HttpServletRequest peticion) {
        var sesion = identidad.iniciarSesion(cuerpo.identificador(), cuerpo.codigo(), agente,
                peticion.getRemoteAddr());
        return RespuestaSesion.de(sesion);
    }

    /** D-78: pide el negocio por evento; el negocio existe hasta que el modulo de negocios responda. */
    @PostMapping("/registro/negocio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RespuestaNegocioSolicitado pedirNegocio(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                                   @RequestBody SolicitudNegocio cuerpo) {
        return RespuestaNegocioSolicitado.de(identidad.solicitarNegocio(token(autorizacion), cuerpo.nombre(), cuerpo.giro()));
    }

    @PutMapping("/auth/negocio-activo")
    public RespuestaSesion elegirNegocio(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                         @RequestBody SolicitudNegocioActivo cuerpo) {
        return RespuestaSesion.de(identidad.elegirNegocio(token(autorizacion), cuerpo.negocioId()));
    }

    @GetMapping("/roles")
    public List<RespuestaRol> roles(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        return identidad.roles(token(autorizacion)).stream().map(RespuestaRol::de).toList();
    }

    @GetMapping("/permisos")
    public List<RespuestaPermiso> permisos(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        return identidad.permisos(token(autorizacion)).stream().map(RespuestaPermiso::de).toList();
    }

    @GetMapping("/auth/yo")
    public RespuestaPerfil yo(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        return RespuestaPerfil.de(identidad.yo(token(autorizacion)));
    }

    @DeleteMapping("/auth/sesion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        identidad.cerrarSesion(token(autorizacion));
    }

    @DeleteMapping("/auth/sesiones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesiones(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        identidad.cerrarTodasLasSesiones(token(autorizacion));
    }

    @PatchMapping("/cuenta")
    public RespuestaCuenta actualizarNombre(@RequestHeader(value = "Authorization", required = false) String autorizacion,
                                            @RequestBody SolicitudNombre cuerpo) {
        return RespuestaCuenta.de(identidad.actualizarNombre(token(autorizacion), cuerpo.nombre()));
    }

    /** Devuelve el token del encabezado, o nulo si falta o no es del esquema Bearer (el esquema no distingue mayusculas). */
    private static String token(String autorizacion) {
        if (autorizacion == null) {
            return null;
        }
        var coincide = BEARER.matcher(autorizacion.strip());
        return coincide.matches() ? coincide.group(1) : null;
    }

    public record SolicitudCodigo(String identificador) {
        @Override
        public String toString() {
            return "SolicitudCodigo[protegido]";
        }
    }

    public record SolicitudSesion(String identificador, String codigo) {
        @Override
        public String toString() {
            return "SolicitudSesion[protegido]";
        }
    }

    public record SolicitudRegistro(String nombre, String apellidos, String correo, String telefono,
                                    boolean aceptaAviso, SolicitudNegocio negocio) {
        @Override
        public String toString() {
            return "SolicitudRegistro[protegido]";
        }
    }

    public record SolicitudVerificacion(String correo, String codigo) {
        @Override
        public String toString() {
            return "SolicitudVerificacion[protegido]";
        }
    }

    public record SolicitudReenvio(String correo) {
        @Override
        public String toString() {
            return "SolicitudReenvio[protegido]";
        }
    }

    public record RespuestaPreregistro(String correo, Instant venceEn) {

        static RespuestaPreregistro de(PreregistroCreado creado) {
            return new RespuestaPreregistro(creado.correo(), creado.venceEn());
        }

        @Override
        public String toString() {
            return "RespuestaPreregistro[protegido]";
        }
    }

    public record SolicitudNegocio(String nombre, String giro) {
        @Override
        public String toString() {
            return "SolicitudNegocio[protegido]";
        }
    }

    public record SolicitudNegocioActivo(UUID negocioId) {
        @Override
        public String toString() {
            return "SolicitudNegocioActivo[protegido]";
        }
    }

    public record RespuestaNegocioSolicitado(UUID negocioId) {

        static RespuestaNegocioSolicitado de(NegocioSolicitado solicitado) {
            return new RespuestaNegocioSolicitado(solicitado.negocioId());
        }
    }

    public record RespuestaRol(String codigo, String nombre, List<String> permisos) {

        static RespuestaRol de(RolVista rol) {
            return new RespuestaRol(rol.codigo(), rol.nombre(), rol.permisos());
        }
    }

    public record RespuestaPermiso(String codigo, String modulo, String descripcion, boolean sensible) {

        static RespuestaPermiso de(PermisoVista permiso) {
            return new RespuestaPermiso(permiso.codigo(), permiso.modulo(), permiso.descripcion(), permiso.sensible());
        }
    }

    public record SolicitudNombre(String nombre) {
        @Override
        public String toString() {
            return "SolicitudNombre[protegido]";
        }
    }

    public record RespuestaCuenta(UUID id, String nombre, String apellidos, String telefono, String correo) {

        static RespuestaCuenta de(CuentaVista cuenta) {
            return new RespuestaCuenta(cuenta.id(), cuenta.nombre(), cuenta.apellidos(), cuenta.telefono(),
                    cuenta.correo());
        }

        @Override
        public String toString() {
            return "RespuestaCuenta[protegido]";
        }
    }

    public record RespuestaNegocio(UUID id, String nombre, String rol) {

        static RespuestaNegocio de(NegocioVista negocio) {
            return new RespuestaNegocio(negocio.id(), negocio.nombre(), negocio.rol());
        }

        @Override
        public String toString() {
            return "RespuestaNegocio[protegido]";
        }
    }

    public record RespuestaNegocioPendiente(UUID id, String nombre, String giro, String estado) {

        static RespuestaNegocioPendiente de(NegocioPendienteVista pendiente) {
            return new RespuestaNegocioPendiente(pendiente.id(), pendiente.nombre(), pendiente.giro(), pendiente.estado());
        }

        @Override
        public String toString() {
            return "RespuestaNegocioPendiente[protegido]";
        }
    }

    public record RespuestaSesion(String token, Instant expiraEn, RespuestaCuenta cuenta, UUID negocioActivo,
                                  List<RespuestaNegocio> negocios, List<RespuestaNegocioPendiente> negociosPendientes) {

        static RespuestaSesion de(SesionIniciada sesion) {
            return new RespuestaSesion(sesion.token(), sesion.expiraEn(), RespuestaCuenta.de(sesion.cuenta()),
                    sesion.negocioActivo(), sesion.negocios().stream().map(RespuestaNegocio::de).toList(),
                    sesion.negociosPendientes().stream().map(RespuestaNegocioPendiente::de).toList());
        }

        @Override
        public String toString() {
            return "RespuestaSesion[protegido]";
        }
    }

    public record RespuestaPerfil(RespuestaCuenta cuenta, UUID negocioActivo, String rol, List<String> permisos,
                                  List<RespuestaNegocio> negocios, List<RespuestaNegocioPendiente> negociosPendientes) {

        static RespuestaPerfil de(PerfilVista perfil) {
            return new RespuestaPerfil(RespuestaCuenta.de(perfil.cuenta()), perfil.negocioActivo(), perfil.rol(),
                    perfil.permisos(), perfil.negocios().stream().map(RespuestaNegocio::de).toList(),
                    perfil.negociosPendientes().stream().map(RespuestaNegocioPendiente::de).toList());
        }

        @Override
        public String toString() {
            return "RespuestaPerfil[protegido]";
        }
    }
}
