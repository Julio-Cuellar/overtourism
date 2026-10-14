package mx.negocio360.identidad.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.identidad.CuentaVista;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.NegocioVista;
import mx.negocio360.identidad.PerfilVista;
import mx.negocio360.identidad.SesionIniciada;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de acceso (D-72): codigo, sesion, perfil, cierre y nombre. Solo traduce HTTP a {@link IdentidadApi}.
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
        identidad.solicitarCodigo(cuerpo.telefono(), peticion.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/sesion")
    public RespuestaSesion iniciarSesion(@RequestBody SolicitudSesion cuerpo,
                                         @RequestHeader(value = "User-Agent", required = false) String agente,
                                         HttpServletRequest peticion) {
        var sesion = identidad.iniciarSesion(cuerpo.telefono(), cuerpo.codigo(), cuerpo.nombre(), agente,
                peticion.getRemoteAddr());
        return RespuestaSesion.de(sesion);
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

    public record SolicitudCodigo(String telefono) {
        @Override
        public String toString() {
            return "SolicitudCodigo[protegido]";
        }
    }

    public record SolicitudSesion(String telefono, String codigo, String nombre) {
        @Override
        public String toString() {
            return "SolicitudSesion[protegido]";
        }
    }

    public record SolicitudNombre(String nombre) {
        @Override
        public String toString() {
            return "SolicitudNombre[protegido]";
        }
    }

    public record RespuestaCuenta(UUID id, String nombre, String telefono) {

        static RespuestaCuenta de(CuentaVista cuenta) {
            return new RespuestaCuenta(cuenta.id(), cuenta.nombre(), cuenta.telefono());
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

    public record RespuestaSesion(String token, Instant expiraEn, RespuestaCuenta cuenta, UUID negocioActivo,
                                  List<RespuestaNegocio> negocios) {

        static RespuestaSesion de(SesionIniciada sesion) {
            return new RespuestaSesion(sesion.token(), sesion.expiraEn(), RespuestaCuenta.de(sesion.cuenta()),
                    sesion.negocioActivo(), sesion.negocios().stream().map(RespuestaNegocio::de).toList());
        }

        @Override
        public String toString() {
            return "RespuestaSesion[protegido]";
        }
    }

    public record RespuestaPerfil(RespuestaCuenta cuenta, UUID negocioActivo, List<RespuestaNegocio> negocios) {

        static RespuestaPerfil de(PerfilVista perfil) {
            return new RespuestaPerfil(RespuestaCuenta.de(perfil.cuenta()), perfil.negocioActivo(),
                    perfil.negocios().stream().map(RespuestaNegocio::de).toList());
        }

        @Override
        public String toString() {
            return "RespuestaPerfil[protegido]";
        }
    }
}
