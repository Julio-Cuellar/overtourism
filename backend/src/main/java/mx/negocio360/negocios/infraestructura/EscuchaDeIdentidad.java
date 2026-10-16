package mx.negocio360.negocios.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import org.springframework.modulith.ApplicationModuleListener;

/**
 * Recibe los eventos del bus y solo actua con {@code negocio.registro_solicitado} v1 de Identidad (B1, R2 y R5): da de
 * alta el negocio o lo rechaza. Lo que no entiende (otro tipo, version u origen, o datos mal formados) se descarta sin
 * error ni reintento, porque repetirlo no lo arreglaria (EV-03). La entrega es al menos una vez y asincrona, en su propia
 * transaccion, y el servicio es idempotente (R3, EV-02).
 */
public class EscuchaDeIdentidad {

    private final AltasApi altas;

    public EscuchaDeIdentidad(AltasApi altas) {
        this.altas = Objects.requireNonNull(altas, "altas");
    }

    @ApplicationModuleListener
    public void alRecibir(SobreEvento evento) {
        NegocioRegistroSolicitadoV1.de(evento).ifPresent(altas::registrarAlta);
    }
}
