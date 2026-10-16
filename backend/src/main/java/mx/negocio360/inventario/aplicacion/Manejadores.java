package mx.negocio360.inventario.aplicacion;

import java.time.Clock;
import java.util.List;
import mx.negocio360.inventario.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;

/** Arma los ocho manejadores del nucleo de Inventario con sus puertos. */
public final class Manejadores {

    private Manejadores() {
    }

    public static List<ManejadorComando<?>> todos(RepositorioArticulos articulos, RepositorioSaldos saldos,
                                                  RepositorioMovimientos movimientos, PublicadorEventos eventos,
                                                  Clock reloj) {
        var ids = new SecuenciaDeIds();
        return List.of(
                new ManejadoresDeArticulo.Crear(articulos, saldos, reloj),
                new ManejadoresDeArticulo.Editar(articulos, reloj),
                new ManejadoresDeArticulo.DefinirUnidad(articulos, reloj),
                new ManejadoresDeArticulo.Baja(articulos, saldos, reloj),
                new ManejadoresDeMovimiento.Entrada(articulos, saldos, movimientos, eventos, reloj, ids),
                new ManejadoresDeMovimiento.Salida(articulos, saldos, movimientos, eventos, reloj, ids),
                new ManejadoresDeMovimiento.Merma(articulos, saldos, movimientos, eventos, reloj, ids),
                new ManejadoresDeMovimiento.Conteo(articulos, saldos, movimientos, eventos, reloj, ids));
    }
}
