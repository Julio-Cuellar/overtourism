package mx.negocio360.inventario.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.ArticuloConSaldo;
import mx.negocio360.inventario.dominio.Articulo;

/**
 * Puerto de persistencia de articulos. Toda operacion se limita al negocio indicado: la implementacion real lo
 * refuerza ademas con seguridad por fila en la base de datos.
 */
public interface RepositorioArticulos {

    Optional<Articulo> porId(UUID negocioId, UUID id);

    /** El articulo activo del negocio con ese nombre, sin importar mayusculas. */
    Optional<Articulo> activoPorNombre(UUID negocioId, String nombre);

    void insertar(Articulo articulo);

    /** Reemplaza al articulo solo si la version guardada es {@code versionEsperada}; si no, falla con CONCURRENCIA. */
    void actualizar(Articulo nuevo, long versionEsperada);

    /**
     * Articulos activos cuyo nombre contiene el texto, con su saldo, ordenados por id a partir del siguiente a
     * {@code despuesDe} cuando no es nulo. Con {@code soloAlertas}, solo los que estan en alerta.
     */
    List<ArticuloConSaldo> buscar(UUID negocioId, String texto, boolean soloAlertas, UUID despuesDe, int limite);
}
