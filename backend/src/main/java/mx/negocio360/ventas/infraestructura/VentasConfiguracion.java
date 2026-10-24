package mx.negocio360.ventas.infraestructura;

import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lo que Ventas necesita sin importar donde guarde sus datos: se enciende con {@code ventas.almacen} (memoria o
 * postgres) y la API la arma {@link VentasEnMemoriaConfiguracion} o {@link VentasPostgresConfiguracion}. Los productos
 * salen de las {@link FuenteDeProductos} que ofrezcan otros modulos (D-128); los cupones, de Pasaporte cuando exista y,
 * mientras no, ninguno es valido (D-134). Con el perfil {@code produccion} el almacen debe ser {@code postgres}
 * ({@code AlmacenesObligatoriosEnProduccion}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ventas.almacen")
public class VentasConfiguracion {

    /**
     * Productos que se pueden vender: las fuentes que haya, en orden.
     *
     * @param fuentes fuentes de productos de los modulos que las ofrecen; puede no haber ninguna
     * @return el catalogo compuesto
     */
    @Bean
    public CatalogoDeVentas catalogoDeVentas(ObjectProvider<FuenteDeProductos> fuentes) {
        return new CatalogoCompuesto(fuentes.orderedStream().toList());
    }

    /**
     * Cupones: ninguno es valido hasta que exista Pasaporte (D-134).
     *
     * @return el adaptador que rechaza todo codigo
     */
    @Bean
    @ConditionalOnMissingBean(CuponesDePasaporte.class)
    public CuponesDePasaporte cuponesDePasaporte() {
        return new CuponesNoDisponibles();
    }
}
