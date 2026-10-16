package mx.negocio360.compartido.seguridad;

import java.util.Optional;

/**
 * Convierte el token de una peticion en la sesion de quien la hace. Identidad lo implementa con su propia sesion y la
 * plataforma solo conoce esta interfaz (R1 y R4), igual que {@link CatalogoDePermisos}. Un token desconocido, vencido
 * o revocado devuelve lo mismo: vacio (AS-08).
 */
@FunctionalInterface
public interface Autenticador {

    Optional<SesionDePeticion> autenticar(String token);
}
