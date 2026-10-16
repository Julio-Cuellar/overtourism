package mx.negocio360.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una cuenta dentro de un negocio, con un solo rol (AM-01, AM-04). Es unica por cuenta y negocio. Cada cambio devuelve
 * otra membresia con la version siguiente (bloqueo optimista). Una revocada no vuelve.
 *
 * @param agregadaPor  la cuenta que la creo, o nulo en el dueño inicial
 */
public record Membresia(UUID id, UUID cuentaId, UUID negocioId, String rolCodigo, EstadoMembresia estado,
                        UUID agregadaPor, Instant creadaEn, Instant actualizadaEn, Instant revocadaEn, long version) {

    public Membresia {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(rolCodigo, "rolCodigo");
        if (rolCodigo.isBlank()) {
            throw new IllegalArgumentException("El rol de la membresia no puede estar vacio");
        }
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(creadaEn, "creadaEn");
        Objects.requireNonNull(actualizadaEn, "actualizadaEn");
    }

    public static Membresia nueva(UUID id, UUID cuentaId, UUID negocioId, String rolCodigo, UUID agregadaPor,
                                  Instant ahora) {
        return new Membresia(id, cuentaId, negocioId, rolCodigo, EstadoMembresia.ACTIVA, agregadaPor, ahora, ahora,
                null, 0);
    }

    public boolean activa() {
        return estado == EstadoMembresia.ACTIVA;
    }

    public Membresia conRol(String nuevoRol, Instant ahora) {
        exigirNoRevocada();
        return new Membresia(id, cuentaId, negocioId, nuevoRol, estado, agregadaPor, creadaEn, ahora, revocadaEn,
                version + 1);
    }

    public Membresia suspendida(Instant ahora) {
        exigirEstado(EstadoMembresia.ACTIVA);
        return conEstado(EstadoMembresia.SUSPENDIDA, ahora, null);
    }

    public Membresia reactivada(Instant ahora) {
        exigirEstado(EstadoMembresia.SUSPENDIDA);
        return conEstado(EstadoMembresia.ACTIVA, ahora, null);
    }

    public Membresia revocada(Instant ahora) {
        exigirNoRevocada();
        return conEstado(EstadoMembresia.REVOCADA, ahora, ahora);
    }

    private Membresia conEstado(EstadoMembresia nuevo, Instant ahora, Instant baja) {
        return new Membresia(id, cuentaId, negocioId, rolCodigo, nuevo, agregadaPor, creadaEn, ahora, baja, version + 1);
    }

    private void exigirNoRevocada() {
        if (estado == EstadoMembresia.REVOCADA) {
            throw new IllegalStateException("Una membresia revocada no vuelve: hay que invitar de nuevo");
        }
    }

    private void exigirEstado(EstadoMembresia esperado) {
        if (estado != esperado) {
            throw new IllegalStateException("Transicion invalida desde " + estado);
        }
    }

    @Override
    public String toString() {
        return "Membresia[cuentaId=" + cuentaId + ", negocioId=" + negocioId + ", rol=" + rolCodigo + ", estado=" + estado + "]";
    }
}
