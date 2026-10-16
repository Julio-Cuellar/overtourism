package mx.negocio360.negocios.aplicacion;

import java.util.List;
import java.util.UUID;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;

/**
 * Altas de negocio (seccion 7.6). {@code registrarAlta} es una escritura del sistema (D-22): la dispara el listener de
 * {@code negocio.registro_solicitado} y es idempotente (NG-03).
 */
public interface AltasApi {

    /** Resultado de procesar un pedido de alta. */
    enum ResultadoAlta { REGISTRADO, RECHAZADO, YA_PROCESADO }

    /** Valida el pedido y crea el negocio con su sucursal principal, o lo rechaza. Publica el evento que corresponde. */
    ResultadoAlta registrarAlta(NegocioRegistroSolicitadoV1 pedido);

    /** Las altas que pidio la cuenta (AL-04). Se permite sin negocio activo: filtra por la cuenta. */
    List<AltaVista> misAltas(UUID cuentaId);
}
