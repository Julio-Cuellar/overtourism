package mx.negocio360.negocios.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un local del negocio. En P1 hay una, la principal, que se crea sola con el alta (NG-08, SU-02). Las coordenadas van
 * ambas o ninguna (SU-03) y el codigo postal tiene cinco digitos (SU-04); la validacion de los datos que escribe una
 * persona esta en {@code DireccionSucursal}.
 */
public record Sucursal(UUID id, UUID negocioId, String nombre, boolean esPrincipal, boolean activa, String calle,
                       String numero, String colonia, String municipio, String entidadFederativa, String codigoPostal,
                       String referencias, BigDecimal latitud, BigDecimal longitud, String telefono, Instant creadaEn,
                       Instant actualizadaEn, long version) {

    public static final String NOMBRE_PRINCIPAL = "Principal";

    public Sucursal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(creadaEn, "creadaEn");
        Objects.requireNonNull(actualizadaEn, "actualizadaEn");
    }

    /** La sucursal principal que nace con el negocio: sin direccion y sin coordenadas (NG-08). */
    public static Sucursal principal(UUID id, UUID negocioId, Instant ahora) {
        return new Sucursal(id, negocioId, NOMBRE_PRINCIPAL, true, true, null, null, null, null, null, null, null, null,
                null, null, ahora, ahora, 0);
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public Sucursal conCambios(String nuevoNombre, String nuevaCalle, String nuevoNumero, String nuevaColonia,
                               String nuevoMunicipio, String nuevaEntidad, String nuevoCodigoPostal,
                               String nuevasReferencias, BigDecimal nuevaLatitud, BigDecimal nuevaLongitud,
                               String nuevoTelefono, Instant ahora) {
        return new Sucursal(id, negocioId, nuevoNombre, esPrincipal, activa, nuevaCalle, nuevoNumero, nuevaColonia,
                nuevoMunicipio, nuevaEntidad, nuevoCodigoPostal, nuevasReferencias, nuevaLatitud, nuevaLongitud,
                nuevoTelefono, creadaEn, ahora, version);
    }

    /** Sin la direccion ni el telefono en el texto (SN-06). */
    @Override
    public String toString() {
        return "Sucursal[id=" + id + ", principal=" + esPrincipal + ", version=" + version + "]";
    }
}
