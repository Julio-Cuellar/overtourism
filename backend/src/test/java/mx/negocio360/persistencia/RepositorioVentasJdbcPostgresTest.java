package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.Venta;
import mx.negocio360.ventas.infraestructura.ContratoRepositorioVentasTest;
import mx.negocio360.ventas.infraestructura.RepositorioVentasJdbc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El repositorio JDBC de Ventas contra PostgreSQL de verdad, con el rol de la aplicacion (sin superusuario y sujeto a la
 * seguridad por fila): la misma bateria que el almacen en memoria ({@link ContratoRepositorioVentasTest}) y lo propio de
 * la base: folio sin huecos con dos confirmaciones simultaneas, inmutabilidad y restricciones.
 */
@SpringBootTest
class RepositorioVentasJdbcPostgresTest extends ContratoRepositorioVentasTest {

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    private RepositorioVentasJdbc repositorio;

    @Override
    protected RepositorioVentas almacen() {
        if (repositorio == null) {
            repositorio = new RepositorioVentasJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
        }
        return repositorio;
    }

    @Test
    @DisplayName("VT-06: dos ventas con el mismo folio en la misma serie no caben (llave unica)")
    void folioRepetidoNoCabe() {
        var negocio = UUID.randomUUID();
        almacen().guardar(venta(negocio, UUID.randomUUID(), "A", 1, AHORA));

        assertThatThrownBy(() -> almacen().guardar(venta(negocio, UUID.randomUUID(), "A", 1, AHORA)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_venta_folio");
        almacen().guardar(venta(negocio, UUID.randomUUID(), "B", 1, AHORA));
        almacen().guardar(venta(UUID.randomUUID(), UUID.randomUUID(), "A", 1, AHORA));
    }

    @Test
    @DisplayName("VT-06: el mismo id de venta no se guarda dos veces")
    void idRepetidoNoCabe() {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(original);

        assertThatThrownBy(() -> almacen().guardar(original)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("VT-06: dos confirmaciones simultaneas no reciben el mismo folio y no quedan huecos")
    void foliosSimultaneos() throws Exception {
        var negocio = UUID.randomUUID();
        var transaccion = new TransactionTemplate(transacciones);
        var hilos = 8;
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(hilos);
        try {
            var tareas = new ArrayList<java.util.concurrent.Future<Long>>();
            for (int i = 0; i < hilos; i++) {
                tareas.add(pool.submit(() -> {
                    salida.await();
                    return transaccion.execute(estado -> almacen().siguienteFolio(negocio, "A"));
                }));
            }
            salida.countDown();
            var folios = new ArrayList<Long>();
            for (var tarea : tareas) {
                folios.add(tarea.get(30, TimeUnit.SECONDS));
            }
            assertThat(folios).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("VT-06: si la venta se deshace, el folio tambien: el siguiente no deja hueco")
    void folioSeDeshaceConLaVenta() {
        var negocio = UUID.randomUUID();
        var transaccion = new TransactionTemplate(transacciones);

        transaccion.executeWithoutResult(estado -> {
            assertThat(almacen().siguienteFolio(negocio, "A")).isEqualTo(1);
            estado.setRollbackOnly();
        });

        assertThat(almacen().siguienteFolio(negocio, "A")).isEqualTo(1);
    }

    @Test
    @DisplayName("una venta con lineas y pagos se guarda entera o no se guarda: si falla un pago no queda la venta")
    void guardarEsAtomico() {
        var negocio = UUID.randomUUID();
        var buena = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        var conPagoRepetido = new Venta(buena.id(), negocio, "A", 2, buena.estado(), buena.turnoId(), null, null,
                buena.subtotal(), buena.descuento(), buena.total(), buena.lineas(),
                java.util.List.of(buena.pagos().get(0), buena.pagos().get(0)), AHORA, "cajero-a", null, null, null, 1);

        // Dos pagos iguales caben (el orden los distingue); lo que no cabe es un monto que no sea positivo.
        var sinMonto = new Venta(buena.id(), negocio, "A", 2, buena.estado(), buena.turnoId(), null, null,
                buena.subtotal(), buena.descuento(), buena.total(), buena.lineas(),
                java.util.List.of(new mx.negocio360.ventas.dominio.PagoVenta(mx.negocio360.ventas.dominio.MetodoPago.EFECTIVO,
                        java.math.BigDecimal.ZERO, null, null, null)), AHORA, "cajero-a", null, null, null, 1);

        assertThatThrownBy(() -> almacen().guardar(sinMonto)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(almacen().porId(negocio, buena.id())).isEmpty();
        almacen().guardar(conPagoRepetido);
        assertThat(almacen().porId(negocio, buena.id())).contains(conPagoRepetido);
    }

    @Test
    @DisplayName("la base no deja editar ni borrar lineas, pagos ni ventas, ni reabrir una venta anulada (VT-11)")
    void inmutable() throws Exception {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(original);
        almacen().actualizar(original.anulada(AHORA, "gerente-a", "Prueba"), 1);

        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.intentar("update ventas.linea_venta set descripcion = 'otra' where venta_id = ?", original.id()))
                    .isEqualTo("23001");
            assertThat(sesion.intentar("delete from ventas.pago_venta where venta_id = ?", original.id())).isEqualTo("23001");
            assertThat(sesion.intentar("delete from ventas.venta where id = ?", original.id())).isEqualTo("23001");
            assertThat(sesion.intentar("update ventas.venta set total = 1, subtotal = 1 where id = ?", original.id()))
                    .isEqualTo("23001");
            assertThat(sesion.intentar("update ventas.venta set estado = 'REGISTRADA', anulada_en = null, anulada_por = null, "
                    + "motivo_anulacion = null where id = ?", original.id())).isEqualTo("23001");
            assertThat(sesion.intentar("truncate ventas.linea_venta")).isEqualTo("23001");
        }
    }

    @Test
    @DisplayName("la aplicacion solo puede actualizar el estado, la anulacion y la version de una venta")
    void laAplicacionSoloAnula() throws Exception {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(original);

        try (var sesion = SesionSql.comoAplicacion().enNegocio(negocio)) {
            assertThat(sesion.intentar("update ventas.venta set total = 1 where id = ?", original.id())).isEqualTo("42501");
            assertThat(sesion.intentar("delete from ventas.venta where id = ?", original.id())).isEqualTo("42501");
            assertThat(sesion.intentar("update ventas.linea_venta set cantidad = 9 where venta_id = ?", original.id()))
                    .isEqualTo("42501");
        }
    }

    @Test
    @DisplayName("las restricciones de la base rechazan una venta incoherente: total distinto de subtotal menos descuento")
    void totalIncoherente() throws Exception {
        var negocio = UUID.randomUUID();
        try (var sesion = SesionSql.comoAplicacion().enNegocio(negocio)) {
            var error = sesion.intentar("insert into ventas.venta (id, negocio_id, serie, folio, estado, turno_id, subtotal, "
                    + "descuento, total, ocurrido_en, registrada_por, version) values (gen_random_uuid(), ?, 'A', 1, "
                    + "'REGISTRADA', gen_random_uuid(), 100, 10, 100, now(), 'x', 1)", negocio);
            assertThat(error).isEqualTo("23514");
        }
    }
}
