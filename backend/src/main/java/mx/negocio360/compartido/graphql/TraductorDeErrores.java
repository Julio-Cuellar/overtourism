package mx.negocio360.compartido.graphql;

import java.util.Optional;

/**
 * Cada modulo dice como se traducen sus excepciones de negocio (SG-13), igual que declara sus permisos con
 * {@code CatalogoDePermisos}: la plataforma no importa a ningun modulo (R1 y R4) y recibe los traductores como beans.
 * Devuelve vacio si la excepcion no es de ese modulo.
 */
@FunctionalInterface
public interface TraductorDeErrores {

    Optional<ErrorTraducido> traducir(Throwable error);
}
