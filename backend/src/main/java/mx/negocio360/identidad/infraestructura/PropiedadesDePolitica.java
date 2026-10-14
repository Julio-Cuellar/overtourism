package mx.negocio360.identidad.infraestructura;

import java.time.Duration;
import mx.negocio360.identidad.aplicacion.PoliticaDeAcceso;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Valores de la politica de acceso tomados de la configuracion, con el prefijo {@code identidad.politica}. Cada
 * valor tiene el inicial del diseño, asi que sin configurar nada la politica es {@link PoliticaDeAcceso#porDefecto()}.
 * Duraciones en formato de Spring: {@code 5m}, {@code 8h}, {@code 30d}.
 */
@ConfigurationProperties(prefix = "identidad.politica")
public record PropiedadesDePolitica(
        @DefaultValue("5m") Duration codigoVigencia,
        @DefaultValue("5") int intentosMaximos,
        @DefaultValue("3") int solicitudesPorTelefono,
        @DefaultValue("20") int solicitudesPorIp,
        @DefaultValue("15m") Duration ventanaDeSolicitudes,
        @DefaultValue("8h") Duration inactividad,
        @DefaultValue("30d") Duration topeAbsoluto,
        @DefaultValue("5") int sesionesMaximas) {

    public PoliticaDeAcceso comoPolitica() {
        return new PoliticaDeAcceso(codigoVigencia, intentosMaximos, solicitudesPorTelefono, solicitudesPorIp,
                ventanaDeSolicitudes, inactividad, topeAbsoluto, sesionesMaximas);
    }
}
