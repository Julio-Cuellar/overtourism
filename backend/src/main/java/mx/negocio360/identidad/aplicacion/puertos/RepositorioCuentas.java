package mx.negocio360.identidad.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Cuenta;

/** Almacen de cuentas. {@code guardar} inserta o reemplaza por id. */
public interface RepositorioCuentas {

    Optional<Cuenta> porId(UUID id);

    /** Busca por telefono E.164 ya normalizado. */
    Optional<Cuenta> porTelefono(String telefono);

    /** Busca por correo ya normalizado (minusculas). */
    Optional<Cuenta> porCorreo(String correo);

    Cuenta guardar(Cuenta cuenta);
}
