package mx.negocio360.identidad.web;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.aplicacion.EquipoServicio;
import mx.negocio360.identidad.contratos.NegocioRegistradoV1;
import mx.negocio360.identidad.eventos.NegocioRegistroSolicitado;
import mx.negocio360.identidad.infraestructura.CanalSimulado;
import mx.negocio360.identidad.infraestructura.PublicadorEventosEnMemoria;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hace las veces del modulo de negocios, que todavia no existe (D-78): toma el pedido {@code negocio.registro_solicitado}
 * de ese negocio y responde como lo haria el modulo real, con {@code negocio.registrado}. SOLO DESARROLLO Y DEMO,
 * con las mismas guardas que el canal simulado (AS-10, SI-13): existe unicamente con
 * {@code identidad.canal-simulado.habilitado=true} y nunca con el perfil {@code produccion}.
 *
 * <p>Aplica el aviso con el servicio, en el mismo hilo, y NO a traves de {@code EscuchaDeNegocios}: Spring Modulith
 * activa {@code @Async} por si solo y ese escucha corre en otro hilo, asi que responder 204 antes de aplicarlo hacia
 * fallar la primera peticion siguiente. Con el bus real la entrega si es asincrona y la app debe tratar el negocio
 * como pendiente hasta que aparezca.
 */
@RestController
@Profile("!produccion")
@ConditionalOnProperty(name = CanalSimulado.PROPIEDAD_DE_HABILITACION, havingValue = "true")
@RequestMapping("/dev/negocios")
public class DevNegociosController {

    private final PublicadorEventosEnMemoria publicados;
    private final EquipoServicio equipo;
    private final Clock reloj;

    public DevNegociosController(PublicadorEventosEnMemoria publicados, EquipoServicio equipo, Clock reloj) {
        this.publicados = publicados;
        this.equipo = equipo;
        this.reloj = reloj;
    }

    @PostMapping("/{negocioId}/registrado")
    public ResponseEntity<Void> registrado(@PathVariable("negocioId") UUID negocioId) {
        var pedido = publicados.eventos().stream().filter(e -> NegocioRegistroSolicitado.TIPO.equals(e.tipo())
                && negocioId.equals(e.negocioId())).findFirst();
        if (pedido.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var solicitud = pedido.get();
        var ahora = reloj.instant();
        var respuesta = new SobreEvento(UuidV7.generar(ahora), NegocioRegistradoV1.TIPO, NegocioRegistradoV1.VERSION,
                negocioId, "negocios", ahora, solicitud.correlacionId(), solicitud.id(),
                Map.of("solicitado_por", solicitud.datos().get("cuenta_id"), "nombre", solicitud.datos().get("nombre"),
                        "giro", solicitud.datos().get("giro")));
        NegocioRegistradoV1.de(respuesta).ifPresent(equipo::alNegocioRegistrado);
        return ResponseEntity.noContent().build();
    }
}
