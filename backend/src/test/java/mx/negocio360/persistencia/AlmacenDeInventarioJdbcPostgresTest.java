package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.infraestructura.ContratoAlmacenDeInventarioTest;
import mx.negocio360.inventario.infraestructura.InventarioJdbc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Los repositorios JDBC de Inventario contra PostgreSQL de verdad, con el rol de la aplicacion (sin superusuario y sujeto
 * a la seguridad por fila): la misma bateria que el almacen en memoria ({@link ContratoAlmacenDeInventarioTest}) y lo
 * propio de la base: nombre unico entre los activos, kardex de solo insercion y llaves foraneas con el negocio.
 */
@SpringBootTest
class AlmacenDeInventarioJdbcPostgresTest extends ContratoAlmacenDeInventarioTest {

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    private InventarioJdbc almacen;

    private InventarioJdbc almacen() {
        if (almacen == null) {
            almacen = new InventarioJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
        }
        return almacen;
    }

    @Override
    protected RepositorioArticulos articulos() {
        return almacen();
    }

    @Override
    protected RepositorioSaldos saldos() {
        return almacen();
    }

    @Override
    protected RepositorioMovimientos movimientos() {
        return almacen();
    }

    @Override
    protected RepositorioConsumos consumos() {
        return almacen();
    }

    @Test
    @DisplayName("U-02: dos articulos activos con el mismo nombre no caben aunque la carrera pase la revision del servicio")
    void nombreActivoRepetido() {
        var negocio = UUID.randomUUID();
        articulos().insertar(articulo(negocio, "Frijol", "0"));

        assertThatThrownBy(() -> articulos().insertar(articulo(negocio, "FRIJOL", "0")))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.NOMBRE_DUPLICADO));
        articulos().insertar(articulo(UUID.randomUUID(), "Frijol", "0"));
    }

    @Test
    @DisplayName("el nombre de un articulo dado de baja se puede volver a usar")
    void nombreDeBajaSeLibera() {
        var negocio = UUID.randomUUID();
        var viejo = articulo(negocio, "Frijol", "0");
        articulos().insertar(viejo);
        articulos().actualizar(viejo.dadoDeBaja(AHORA), viejo.version());

        articulos().insertar(articulo(negocio, "Frijol", "0"));

        assertThat(articulos().activoPorNombre(negocio, "Frijol")).isPresent();
    }

    @Test
    @DisplayName("renombrar a un nombre ya usado por otro activo falla con NOMBRE_DUPLICADO")
    void renombrarARepetido() {
        var negocio = UUID.randomUUID();
        var a = articulo(negocio, "Frijol", "0");
        var b = articulo(negocio, "Arroz", "0");
        articulos().insertar(a);
        articulos().insertar(b);

        assertThatThrownBy(() -> articulos().actualizar(b.conDatos("frijol", null, BigDecimal.ZERO, AHORA), b.version()))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.NOMBRE_DUPLICADO));
    }

    @Test
    @DisplayName("un saldo y un movimiento no pueden colgarse de un articulo de otro negocio")
    void llavesForaneasConElNegocio() {
        var negocio = UUID.randomUUID();
        var articulo = articulo(negocio, "Frijol", "0");
        articulos().insertar(articulo);
        var otroNegocio = UUID.randomUUID();

        assertThatThrownBy(() -> saldos().insertar(Saldo.inicial(articulo.id(), otroNegocio)))
                .isInstanceOf(DataIntegrityViolationException.class);
        var cuando = AHORA.plusSeconds(1);
        assertThatThrownBy(() -> movimientos().insertar(new Movimiento(UuidV7.generar(cuando), otroNegocio, articulo.id(),
                TipoMovimiento.ENTRADA, BigDecimal.ONE.setScale(4), "g", BigDecimal.ONE.setScale(8),
                BigDecimal.ONE.setScale(4), BigDecimal.ONE.setScale(4), BigDecimal.ONE.setScale(2),
                BigDecimal.ONE.setScale(4), BigDecimal.ONE.setScale(4), CausaTipo.MANUAL, null, null, cuando, cuando, "x")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("el kardex es de solo insercion, tambien para el dueño de la base")
    void kardexSoloInsercion() throws Exception {
        var negocio = UUID.randomUUID();
        var articulo = articulo(negocio, "Frijol", "0");
        articulos().insertar(articulo);
        var cuando = AHORA.plusSeconds(1);
        var movimiento = new Movimiento(UuidV7.generar(cuando), negocio, articulo.id(), TipoMovimiento.ENTRADA,
                BigDecimal.ONE.setScale(4), "g", BigDecimal.ONE.setScale(8), BigDecimal.ONE.setScale(4),
                BigDecimal.ONE.setScale(4), BigDecimal.ONE.setScale(2), BigDecimal.ONE.setScale(4),
                BigDecimal.ONE.setScale(4), CausaTipo.MANUAL, null, null, cuando, cuando, "x");
        movimientos().insertar(movimiento);

        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.intentar("update inventario.movimiento set costo_total = 9 where id = ?", movimiento.id()))
                    .isEqualTo("23001");
            assertThat(sesion.intentar("delete from inventario.movimiento where id = ?", movimiento.id())).isEqualTo("23001");
            assertThat(sesion.intentar("truncate inventario.movimiento")).isEqualTo("23001");
        }
        try (var sesion = SesionSql.comoAplicacion().enNegocio(negocio)) {
            assertThat(sesion.intentar("update inventario.movimiento set costo_total = 9 where id = ?", movimiento.id()))
                    .isEqualTo("42501");
            assertThat(sesion.intentar("delete from inventario.articulo where id = ?", articulo.id())).isEqualTo("42501");
        }
    }

    @Test
    @DisplayName("el signo de la cantidad tiene que corresponder al tipo de movimiento (la base lo vuelve a comprobar)")
    void signoDelMovimiento() throws Exception {
        var negocio = UUID.randomUUID();
        var articulo = articulo(negocio, "Frijol", "0");
        articulos().insertar(articulo);

        try (var sesion = SesionSql.comoAplicacion().enNegocio(negocio)) {
            var error = sesion.intentar("insert into inventario.movimiento (id, negocio_id, articulo_id, tipo, "
                    + "cantidad_capturada, unidad_capturada, factor_aplicado, cantidad_base, costo_unitario, costo_total, "
                    + "cpp_resultante, saldo_resultante, causa_tipo, ocurrido_en, registrado_en, registrado_por) "
                    + "values (gen_random_uuid(), ?, ?, 'ENTRADA', 1, 'g', 1, -1, 1, 1, 1, 1, 'MANUAL', now(), now(), 'x')",
                    negocio, articulo.id());
            assertThat(error).isEqualTo("23514");
        }
    }
}
