package mx.negocio360.identidad;

import java.util.UUID;

/** Respuesta de pedir un negocio: el id que Identidad le asigno. El negocio aun no existe hasta que lo registre el modulo de negocios. */
public record NegocioSolicitado(UUID negocioId) {
}
