package mx.negocio360.inventario.dominio;

import org.springframework.modulith.NamedInterface;

/** Motivo de una merma (comando {@code RegistrarMerma}, 6.3). */
@NamedInterface("api")
public enum MotivoMerma {
    CADUCIDAD,
    DANO,
    ROBO,
    OTRO
}
