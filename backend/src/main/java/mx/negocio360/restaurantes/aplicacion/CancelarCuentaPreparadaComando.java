package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;

/**
 * Cancelar una cuenta con platillos ya listos o entregados (CT-11): preparar -&gt; confirmar, porque ya hay merma.
 *
 * @param cuentaId la cuenta a cancelar
 * @param motivo   por que se cancela (QR-10)
 * @param texto    texto libre de hasta 80 caracteres, solo con {@code OTRO}
 */
public record CancelarCuentaPreparadaComando(UUID cuentaId, MotivoDeAnulacion motivo, String texto) implements Comando {
}
