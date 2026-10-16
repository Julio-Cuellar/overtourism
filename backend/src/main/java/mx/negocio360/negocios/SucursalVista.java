package mx.negocio360.negocios;

import java.math.BigDecimal;
import java.util.UUID;

/** Una sucursal para el panel del negocio. */
public record SucursalVista(UUID id, String nombre, boolean esPrincipal, boolean activa, String calle, String numero,
                            String colonia, String municipio, String entidadFederativa, String codigoPostal,
                            String referencias, BigDecimal latitud, BigDecimal longitud, String telefono,
                            long version) {

    /** Sin la direccion ni el telefono en el texto: no deben acabar en un registro (SN-06). */
    @Override
    public String toString() {
        return "SucursalVista[id=" + id + ", principal=" + esPrincipal + ", version=" + version + "]";
    }
}
