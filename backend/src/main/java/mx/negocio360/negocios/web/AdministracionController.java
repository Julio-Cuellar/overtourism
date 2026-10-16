package mx.negocio360.negocios.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.util.UUID;
import mx.negocio360.negocios.AdministracionApi;
import mx.negocio360.negocios.ResenaVista;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * R6 (SN-15): la moderacion de reseñas de la plataforma. Controlador aparte del panel, con su propia autenticacion: un
 * token de servicio en {@code X-Servicio-Admin}, que se compara en tiempo constante. La plataforma se gestiona en otro
 * servicio (D-108), asi que no hay cuentas de operador: la ruta SOLO EXISTE si hay un token configurado
 * ({@code negocios.admin.token}) y nunca con el perfil {@code produccion}. Sin token configurado responde 404; con uno
 * equivocado, 401. Por ahora nadie modera de forma activa (D-110).
 */
@RestController
@Profile("!produccion")
@ConditionalOnExpression("!'${negocios.admin.token:}'.isBlank()")
public class AdministracionController {

    private static final String OPERADOR = "servicio-de-administracion";

    /** El cuerpo de ocultar: un codigo de motivo ({@code A-Z0-9_}); no se guarda texto libre. */
    public record Ocultar(String motivo) {
    }

    /** Lo que se devuelve: la reseña y su estado, sin autor ni comentario. */
    public record Moderada(UUID id, String estado) {
    }

    private final AdministracionApi administracion;
    private final byte[] token;

    public AdministracionController(AdministracionApi administracion, @Value("${negocios.admin.token}") String token) {
        this.administracion = administracion;
        this.token = token.getBytes(UTF_8);
    }

    @PostMapping("/admin/negocios/{negocioId}/resenas/{resenaId}/ocultar")
    public ResponseEntity<Moderada> ocultar(@RequestHeader(value = "X-Servicio-Admin", required = false) String enviado,
                                            @PathVariable UUID negocioId, @PathVariable UUID resenaId,
                                            @RequestBody Ocultar cuerpo) {
        if (!autenticado(enviado)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(moderada(administracion.ocultarResena(negocioId, resenaId, cuerpo.motivo(), OPERADOR)));
    }

    @PostMapping("/admin/negocios/{negocioId}/resenas/{resenaId}/restituir")
    public ResponseEntity<Moderada> restituir(@RequestHeader(value = "X-Servicio-Admin", required = false) String enviado,
                                              @PathVariable UUID negocioId, @PathVariable UUID resenaId) {
        if (!autenticado(enviado)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(moderada(administracion.restituirResena(negocioId, resenaId, OPERADOR)));
    }

    private boolean autenticado(String enviado) {
        return enviado != null && MessageDigest.isEqual(token, enviado.getBytes(UTF_8));
    }

    private static Moderada moderada(ResenaVista vista) {
        return new Moderada(vista.id(), vista.estado());
    }
}
