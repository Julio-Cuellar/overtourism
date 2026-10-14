package mx.negocio360.identidad.aplicacion.puertos;

import java.util.Optional;
import mx.negocio360.identidad.dominio.CodigoAcceso;

/** Almacen de codigos de acceso. {@code guardar} inserta o reemplaza por id. */
public interface RepositorioCodigos {

    void guardar(CodigoAcceso codigo);

    /** El codigo emitido mas recientemente para ese telefono: un codigo nuevo reemplaza a los anteriores. */
    Optional<CodigoAcceso> ultimoDe(String telefono);
}
