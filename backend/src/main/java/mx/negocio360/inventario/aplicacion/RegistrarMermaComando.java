package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.inventario.dominio.MotivoMerma;

/** Merma con motivo. Se valua al CPP vigente. */
public record RegistrarMermaComando(UUID articuloId, BigDecimal cantidad, String unidad, MotivoMerma motivo)
        implements Comando {
}
