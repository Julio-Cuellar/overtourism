package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Mesa;
import mx.negocio360.restaurantes.dominio.Servicio;
import mx.negocio360.restaurantes.dominio.Zona;

/**
 * Almacen del salon: zonas, mesas y el servicio. Toda consulta lleva el negocio, salvo la busqueda por el token opaco del
 * QR, que es global y unica (QR-01). Las escrituras de reemplazo comparan la version anterior y devuelven falso si otra
 * persona se adelanto.
 */
public interface RepositorioSalon {

    /** Zonas del negocio, activas o no. */
    List<Zona> zonas(UUID negocioId);

    /** Una zona del negocio. */
    Optional<Zona> zona(UUID negocioId, UUID id);

    /** Guarda una zona nueva. */
    void insertar(Zona zona);

    /** Reemplaza la zona si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Zona nueva, long versionAnterior);

    /** Mesas del negocio, activas o no. */
    List<Mesa> mesas(UUID negocioId);

    /** Una mesa del negocio, activa o no. */
    Optional<Mesa> mesa(UUID negocioId, UUID id);

    /** Guarda una mesa nueva. */
    void insertar(Mesa mesa);

    /** Reemplaza la mesa si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Mesa nueva, long versionAnterior);

    /** La mesa <b>activa</b> con ese token de QR; un token rotado, inexistente o de una mesa dada de baja no resuelve (MS-09). */
    Optional<Mesa> mesaPorToken(String token);

    /** El servicio del negocio; si nunca se toco, el inicial. */
    Servicio servicio(UUID negocioId);

    /** Guarda el servicio del negocio. */
    void guardar(Servicio servicio);
}
