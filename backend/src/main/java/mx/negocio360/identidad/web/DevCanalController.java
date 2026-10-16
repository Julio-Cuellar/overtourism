package mx.negocio360.identidad.web;

import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.dominio.Correo;
import mx.negocio360.identidad.infraestructura.CanalSimulado;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega el ultimo codigo que "envio" el canal simulado a un correo, para poder probar el registro y el acceso con
 * Postman o a mano sin un servicio de mensajeria. SOLO DESARROLLO Y DEMO (AS-10, SI-13): existe unicamente si se habilita expresamente con
 * {@code identidad.canal-simulado.habilitado=true} (el perfil {@code desarrollo} lo hace) y nunca con el perfil
 * {@code produccion}. Sin la propiedad no hay endpoint aunque se olvide poner el perfil: falla cerrado.
 */
@RestController
@Profile("!produccion")
@ConditionalOnProperty(name = CanalSimulado.PROPIEDAD_DE_HABILITACION, havingValue = "true")
@RequestMapping("/dev/canal")
public class DevCanalController {

    private final CanalSimulado canal;

    public DevCanalController(CanalSimulado canal) {
        this.canal = canal;
    }

    @GetMapping("/codigo")
    public ResponseEntity<RespuestaCodigo> ultimoCodigo(@RequestParam("correo") String correo) {
        var normalizado = Correo.normalizar(correo)
                .orElseThrow(() -> new IdentidadException(CodigoError.CORREO_INVALIDO));
        return canal.ultimoCodigoDe(normalizado).map(codigo -> ResponseEntity.ok(new RespuestaCodigo(codigo)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RespuestaCodigo(String codigo) {
        @Override
        public String toString() {
            return "RespuestaCodigo[protegido]";
        }
    }
}
