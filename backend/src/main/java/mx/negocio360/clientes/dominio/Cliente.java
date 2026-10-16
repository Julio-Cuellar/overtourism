package mx.negocio360.clientes.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.clientes.eventos.OrigenCliente;

/**
 * Cliente de un negocio (tabla {@code cliente}). Inmutable: una edicion produce otra instancia con la version
 * siguiente, que es la base del bloqueo optimista (I-06).
 *
 * @param avisoVersion       version del aviso de privacidad que se puso a su disposicion (L-03)
 * @param anonimizadoEn      nulo mientras el cliente este activo
 * @param version            contador de bloqueo optimista, desde 1
 */
public record Cliente(UUID id, UUID negocioId, DatosCliente datos, OrigenCliente origen, EstadoCliente estado,
                      int avisoVersion, Instant avisoEntregadoEn, Instant anonimizadoEn, long version,
                      Instant creadoEn, Instant actualizadoEn, String creadoPor) {

    public static final long VERSION_INICIAL = 1L;

    public Cliente {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(datos, "datos");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(avisoEntregadoEn, "avisoEntregadoEn");
        Objects.requireNonNull(creadoEn, "creadoEn");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn");
        Objects.requireNonNull(creadoPor, "creadoPor");
        if (version < VERSION_INICIAL) {
            throw new IllegalArgumentException("La version del cliente debe ser 1 o mayor: " + version);
        }
        if (avisoVersion < 1) {
            throw new IllegalArgumentException("La version del aviso debe ser 1 o mayor: " + avisoVersion);
        }
    }

    /** Sin datos personales: un registro o un mensaje de error no debe poder copiarlos (P-06). */
    @Override
    public String toString() {
        return "Cliente[id=" + id + ", estado=" + estado + ", version=" + version + "]";
    }

    public static Cliente nuevo(UUID id, UUID negocioId, DatosCliente datos, OrigenCliente origen, int avisoVersion,
                                String creadoPor, Instant ahora) {
        return new Cliente(id, negocioId, datos, origen, EstadoCliente.ACTIVO, avisoVersion, ahora, null,
                VERSION_INICIAL, ahora, ahora, creadoPor);
    }

    /** El mismo cliente con otros datos y la version siguiente. */
    public Cliente conDatos(DatosCliente nuevosDatos, Instant ahora) {
        return new Cliente(id, negocioId, nuevosDatos, origen, estado, avisoVersion, avisoEntregadoEn, anonimizadoEn,
                version + 1, creadoEn, ahora, creadoPor);
    }
}
