package mx.negocio360.negocios;

import java.math.BigDecimal;

/**
 * Cambios a la direccion y las coordenadas de una sucursal (G14). Un campo nulo no cambia; un texto vacio borra el
 * opcional. La latitud y la longitud van juntas (SU-03).
 */
public record ActualizarSucursal(long version, String nombre, String calle, String numero, String colonia,
                                 String municipio, String entidadFederativa, String codigoPostal, String referencias,
                                 BigDecimal latitud, BigDecimal longitud, String telefono) {

    /** Sin la direccion en el texto (SN-06). */
    @Override
    public String toString() {
        return "ActualizarSucursal[version=" + version + "]";
    }
}
