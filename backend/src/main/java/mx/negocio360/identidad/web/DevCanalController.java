package mx.negocio360.identidad.web;

import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.dominio.Telefono;
import mx.negocio360.identidad.infraestructura.CanalSimulado;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega el ultimo codigo que "envio" el canal simulado, para poder probar el acceso con Postman o a mano sin un
 * SMS real. SOLO DESARROLLO Y DEMO (AS-10, SI-13): existe unicamente si se habilita expresamente con
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
    public ResponseEntity<RespuestaCodigo> ultimoCodigo(@RequestParam("telefono") String telefono) {
        var numero = Telefono.normalizar(telefono)
                .orElseThrow(() -> new IdentidadException(CodigoError.TELEFONO_INVALIDO));
        return canal.ultimoCodigoDe(numero).map(codigo -> ResponseEntity.ok(new RespuestaCodigo(codigo)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RespuestaCodigo(String codigo) {
        @Override
        public String toString() {
            return "RespuestaCodigo[protegido]";
        }
    }
}
