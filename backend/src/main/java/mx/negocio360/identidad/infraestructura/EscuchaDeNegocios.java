package mx.negocio360.identidad.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.identidad.aplicacion.EquipoServicio;
import mx.negocio360.identidad.contratos.NegocioRegistradoV1;
import org.springframework.modulith.ApplicationModuleListener;

/**
 * Recibe los eventos del bus y solo actua con {@code negocio.registrado} v1 (D-78, R2 y R5): deja a quien pidio el
 * negocio como DUENO. Los demas eventos y los mal formados se ignoran sin error, porque reintentarlos no los arreglaria.
 * La entrega es al menos una vez y asincrona (Spring Modulith activa {@code @Async} por si solo: corre en otro hilo y en
 * su propia transaccion), y el servicio es idempotente (R3). Solo acepta el aviso de un negocio que Identidad
 * pidio, con el origen {@code negocios} y a nombre de quien lo pidio.
 */
public class EscuchaDeNegocios {

    private final EquipoServicio equipo;

    public EscuchaDeNegocios(EquipoServicio equipo) {
        this.equipo = Objects.requireNonNull(equipo, "equipo");
    }

    @ApplicationModuleListener
    public void alRecibir(SobreEvento evento) {
        NegocioRegistradoV1.de(evento).ifPresent(equipo::alNegocioRegistrado);
    }
}
