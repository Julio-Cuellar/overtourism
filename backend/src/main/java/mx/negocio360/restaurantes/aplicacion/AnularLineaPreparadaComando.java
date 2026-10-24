package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;

/**
 * Anular una linea ya lista o entregada (CT-03): preparar -&gt; confirmar, porque ya hay merma.
 *
 * @param lineaId la linea a anular
 * @param motivo  por que se anula (QR-10)
 * @param texto   texto libre de hasta 80 caracteres, solo con {@code OTRO}
 */
public record AnularLineaPreparadaComando(UUID lineaId, MotivoDeAnulacion motivo, String texto) implements Comando {
}
