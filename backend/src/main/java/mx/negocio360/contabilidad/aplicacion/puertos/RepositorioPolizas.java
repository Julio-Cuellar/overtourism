package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Poliza;

/**
 * Puerto de las tablas {@code poliza} y {@code movimiento}. Solo inserta: no hay operacion para cambiar ni borrar
 * una poliza (A-03, F-04).
 */
public interface RepositorioPolizas {

    /**
     * Inserta una poliza con sus renglones.
     *
     * @param poliza poliza nueva
     */
    void insertar(Poliza poliza);

    /**
     * Busca una poliza del negocio.
     *
     * @param negocioId negocio
     * @param polizaId  poliza
     * @return la poliza, o vacio si no existe o es de otro negocio
     */
    Optional<Poliza> porId(UUID negocioId, UUID polizaId);

    /**
     * Polizas del negocio en un periodo, en orden de registro.
     *
     * @param negocioId negocio
     * @param periodoId periodo
     * @return las polizas
     */
    List<Poliza> delPeriodo(UUID negocioId, UUID periodoId);
}
