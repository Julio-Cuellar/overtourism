package mx.negocio360.negocios.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.dominio.ExcepcionHorario;
import mx.negocio360.negocios.dominio.SemanaHorario;
import mx.negocio360.negocios.dominio.Sucursal;

/** Sucursales y su horario (semana y excepciones), con seguridad por fila. */
public interface RepositorioSucursales {

    List<Sucursal> sucursales(UUID negocioId);

    Optional<Sucursal> sucursal(UUID negocioId, UUID sucursalId);

    /**
     * Guarda la direccion y las coordenadas si la version de la sucursal es {@code versionEsperada}.
     * @throws mx.negocio360.negocios.dominio.NegociosException CONCURRENCIA
     */
    void actualizar(Sucursal nueva, long versionEsperada);

    SemanaHorario semana(UUID negocioId, UUID sucursalId);

    List<ExcepcionHorario> excepciones(UUID negocioId, UUID sucursalId);

    /** El control de concurrencia del horario semanal, aparte del de la direccion. */
    long versionDelHorario(UUID negocioId, UUID sucursalId);

    /**
     * Reemplaza la semana completa en la transaccion si la version del horario es {@code versionEsperada} (HO-05).
     * @throws mx.negocio360.negocios.dominio.NegociosException CONCURRENCIA
     */
    void reemplazarSemana(UUID negocioId, UUID sucursalId, long versionEsperada, SemanaHorario semana, Instant ahora);

    /** {@code false} si ya habia una excepcion en esa fecha (HO-03). */
    boolean agregarExcepcion(UUID negocioId, UUID sucursalId, ExcepcionHorario excepcion);

    /** Quita una excepcion y devuelve la sucursal a la que pertenecia, o vacio si no existe en este negocio. */
    Optional<UUID> quitarExcepcion(UUID negocioId, UUID excepcionId);
}
