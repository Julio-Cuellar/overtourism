package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EstadoDeMesa;
import mx.negocio360.restaurantes.dominio.Mesa;

/**
 * Una mesa tal como la ve el personal, con su estado derivado (MS-02) y la cuenta resumida si la tiene.
 *
 * @param estado    libre, ocupada o bloqueada
 * @param qrPausado verdadero si su QR no acepta pedidos en este momento (QR-17)
 * @param cuenta    la cuenta abierta o por cobrar, o nulo
 */
public record MesaVista(UUID id, UUID zonaId, String etiqueta, int capacidad, EstadoDeMesa estado, boolean bloqueada,
                        boolean qrPausado, long version, ResumenDeCuenta cuenta) {

    /** La vista de una mesa en ese momento y con esa cuenta. */
    public static MesaVista de(Mesa mesa, ResumenDeCuenta cuenta, Instant ahora) {
        var estado = mesa.bloqueada() ? EstadoDeMesa.BLOQUEADA : cuenta != null ? EstadoDeMesa.OCUPADA : EstadoDeMesa.LIBRE;
        return new MesaVista(mesa.id(), mesa.zonaId(), mesa.etiqueta(), mesa.capacidad(), estado, mesa.bloqueada(),
                mesa.qrPausado(ahora), mesa.version(), cuenta);
    }
}
