package mx.negocio360.identidad.infraestructura;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.dominio.Cuenta;

/** Cuentas en memoria. Solo para desarrollo, demo y pruebas: se pierden al reiniciar. Reemplaza IDN-1.1 (Postgres). */
public final class RepositorioCuentasEnMemoria implements RepositorioCuentas {

    private final Map<UUID, Cuenta> porId = new LinkedHashMap<>();

    @Override
    public synchronized Optional<Cuenta> porId(UUID id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public synchronized Optional<Cuenta> porTelefono(String telefono) {
        return porId.values().stream().filter(c -> c.telefono().equals(telefono)).findFirst();
    }

    @Override
    public synchronized Optional<Cuenta> porCorreo(String correo) {
        return porId.values().stream().filter(c -> c.correo().equals(correo)).findFirst();
    }

    @Override
    public synchronized Cuenta guardar(Cuenta cuenta) {
        porId.put(cuenta.id(), cuenta);
        return cuenta;
    }

    /** Todas las cuentas, para las pruebas. */
    public synchronized List<Cuenta> todas() {
        return new ArrayList<>(porId.values());
    }
}
