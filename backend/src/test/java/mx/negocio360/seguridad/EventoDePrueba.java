package mx.negocio360.seguridad;

import java.util.UUID;

/** Solo pruebas: un evento de un negocio, como los que publicarian los modulos por el bus. */
public record EventoDePrueba(UUID negocioId, String texto) {
}
