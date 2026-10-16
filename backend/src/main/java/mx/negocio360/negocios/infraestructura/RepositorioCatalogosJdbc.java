package mx.negocio360.negocios.infraestructura;

import java.util.List;
import java.util.Objects;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioCatalogos;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Catalogos globales de solo lectura: {@code giro}, {@code subcategoria} y {@code catalogo_atributo}. */
public final class RepositorioCatalogosJdbc implements RepositorioCatalogos {

    private final JdbcClient jdbc;

    public RepositorioCatalogosJdbc(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public List<Giro> giros() {
        return jdbc.sql("SELECT codigo, nombre FROM negocios.giro WHERE activo ORDER BY orden, codigo")
                .query((r, n) -> new Giro(r.getString("codigo"), r.getString("nombre"))).list();
    }

    @Override
    public List<Subcategoria> subcategorias() {
        return jdbc.sql("SELECT codigo, giro, nombre FROM negocios.subcategoria WHERE activa ORDER BY giro, orden, codigo")
                .query((r, n) -> new Subcategoria(r.getString("codigo"), r.getString("giro"), r.getString("nombre")))
                .list();
    }

    @Override
    public List<Atributo> atributos() {
        return jdbc.sql("SELECT tipo, codigo, nombre FROM negocios.catalogo_atributo WHERE activa "
                        + "ORDER BY tipo, orden, codigo")
                .query((r, n) -> new Atributo(r.getString("tipo"), r.getString("codigo"), r.getString("nombre")))
                .list();
    }
}
