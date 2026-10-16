package mx.negocio360.identidad.aplicacion.puertos;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Preregistro;

/**
 * Almacen de pre-registros (D-84, D-86): hay uno por correo. Con Postgres lo implementa un repositorio JDBC; la tabla
 * guarda datos personales solo mientras dura el codigo, asi que {@link #borrarVencidos} se ejecuta de forma periodica.
 */
public interface RepositorioPreregistros {

    /** Inserta el pre-registro o, si ese correo ya tenia uno, lo reemplaza. */
    void guardar(Preregistro preregistro);

    Optional<Preregistro> porCorreo(String correo);

    /**
     * Borra el pre-registro y dice si lo borro. De dos verificaciones simultaneas solo una recibe {@code true}: la que
     * lo consume. Con Postgres es un {@code DELETE} y se mira cuantas filas borro.
     */
    boolean eliminar(UUID id);

    /** Borra los que ya vencieron a esa hora (el que vence justo ahora, tambien) y devuelve cuantos eran. */
    int borrarVencidos(Instant ahora);
}
