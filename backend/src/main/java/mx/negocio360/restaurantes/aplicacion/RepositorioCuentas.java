package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;

/**
 * Almacen de cuentas y comandas. Toda consulta lleva el negocio: un repositorio nunca devuelve algo de otro negocio
 * (R1, SG-02). Los reemplazos comparan la version anterior y devuelven falso si otra persona se adelanto. Una mesa tiene a
 * lo mas una cuenta sin pagar y {@link #insertar(Cuenta)} lo garantiza de forma atomica (MS-03).
 */
public interface RepositorioCuentas {

    /** Una cuenta del negocio, en cualquier estado. */
    Optional<Cuenta> cuenta(UUID negocioId, UUID id);

    /** La cuenta que tiene esa linea. */
    Optional<Cuenta> cuentaDeLinea(UUID negocioId, UUID lineaId);

    /** Todas las cuentas del negocio. */
    List<Cuenta> cuentas(UUID negocioId);

    /** La cuenta abierta o por cobrar de una mesa, si la tiene. */
    Optional<Cuenta> cuentaNoPagadaDeMesa(UUID negocioId, UUID mesaId);

    /**
     * Guarda una cuenta nueva. Si trae mesa y esa mesa ya tiene una cuenta sin pagar, no guarda y devuelve falso (MS-03),
     * tambien con dos peticiones simultaneas.
     */
    boolean insertar(Cuenta cuenta);

    /** Reemplaza la cuenta si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Cuenta nueva, long versionAnterior);

    /**
     * Las cuentas de cualquier negocio que tienen alguna linea creada antes de {@code limite} con nota o hash de dispositivo
     * todavia guardados (CT-12). Es lo unico que cruza negocios, y solo para la tarea que los borra.
     */
    List<Cuenta> cuentasConDatosAnterioresA(Instant limite);

    /** El siguiente numero consecutivo de cuenta del negocio (CT-01). */
    int siguienteNumero(UUID negocioId);

    /** Una comanda del negocio. */
    Optional<Comanda> comanda(UUID negocioId, UUID id);

    /** Todas las comandas del negocio. */
    List<Comanda> comandas(UUID negocioId);

    /** Guarda una comanda nueva. */
    void insertar(Comanda comanda);

    /** Reemplaza la comanda si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Comanda nueva, long versionAnterior);
}
