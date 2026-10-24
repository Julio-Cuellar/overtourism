package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.infraestructura.ContratoAlmacenDeCajaTest;
import mx.negocio360.caja.infraestructura.LibroDeCajaJdbc;
import mx.negocio360.caja.infraestructura.RepositorioCajaJdbc;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Los repositorios JDBC de Caja contra PostgreSQL de verdad, con el rol de la aplicacion (sin superusuario y sujeto a la
 * seguridad por fila): la misma bateria que el almacen en memoria ({@link ContratoAlmacenDeCajaTest}) y lo propio de la
 * base: un turno sin cerrar por caja, movimientos y cortes de solo insercion, y llaves foraneas que incluyen el negocio.
 */
@SpringBootTest
class AlmacenDeCajaJdbcPostgresTest extends ContratoAlmacenDeCajaTest {

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    private RepositorioCajaJdbc cajas;
    private LibroDeCajaJdbc libro;

    @Override
    protected RepositorioCaja cajas() {
        if (cajas == null) {
            cajas = new RepositorioCajaJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
        }
        return cajas;
    }

    @Override
    protected LibroDeCaja libro() {
        if (libro == null) {
            libro = new LibroDeCajaJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
        }
        return libro;
    }

    @Test
    @DisplayName("CJ-01: una caja no puede tener dos turnos sin cerrar (indice unico parcial)")
    void dosTurnosSinCerrar() {
        var negocio = UUID.randomUUID();
        var abierto = turnoAbierto(negocio);
        var otro = new Turno(UuidV7.generar(AHORA.plusSeconds(1)), negocio, abierto.cajaId(), "cajero-b",
                EstadoTurno.ABIERTO, BigDecimal.TEN.setScale(2), AHORA, null, null, 1);

        assertThatThrownBy(() -> cajas().guardarTurno(otro)).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_turno_no_cerrado");
    }

    @Test
    @DisplayName("el nombre de una caja es unico por negocio sin importar mayusculas, y libre en otro negocio")
    void nombreDeCajaUnico() {
        var negocio = UUID.randomUUID();
        cajas().guardarCaja(new Caja(UuidV7.generar(AHORA), negocio, "Mostrador", true));

        assertThatThrownBy(() -> cajas().guardarCaja(new Caja(UuidV7.generar(AHORA.plusSeconds(1)), negocio, "MOSTRADOR", true)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ux_caja_nombre");
        cajas().guardarCaja(new Caja(UuidV7.generar(AHORA), UUID.randomUUID(), "MOSTRADOR", true));
    }

    @Test
    @DisplayName("un movimiento no puede colgarse de un turno de otro negocio (la llave foranea incluye el negocio)")
    void movimientoDeOtroNegocio() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var ajeno = new MovimientoCaja(UuidV7.generar(AHORA), UUID.randomUUID(), turno.id(), TipoMovimiento.ENTRADA,
                MetodoPago.EFECTIVO, BigDecimal.ONE, null, null, null, AHORA, "x");

        assertThatThrownBy(() -> libro().agregar(ajeno)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CJ-09: un turno se cierra con un solo corte")
    void unCortePorTurno() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var montos = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
        montos.put(MetodoPago.EFECTIVO, new BigDecimal("1.00"));
        libro().guardarCorte(new Corte(UuidV7.generar(AHORA), negocio, turno.id(), turno.cajaId(), montos, montos,
                BigDecimal.ZERO.setScale(2), null, AHORA, AHORA));

        assertThatThrownBy(() -> libro().guardarCorte(new Corte(UuidV7.generar(AHORA.plusSeconds(1)), negocio, turno.id(),
                turno.cajaId(), montos, montos, BigDecimal.ZERO.setScale(2), null, AHORA, AHORA)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_corte_turno");
    }

    @Test
    @DisplayName("CJ-09: movimientos y cortes son de solo insercion, tambien para el dueño de la base")
    void soloInsercion() throws Exception {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var movimiento = new MovimientoCaja(UuidV7.generar(AHORA), negocio, turno.id(), TipoMovimiento.ENTRADA,
                MetodoPago.EFECTIVO, BigDecimal.ONE, null, null, null, AHORA, "x");
        libro().agregar(movimiento);
        var montos = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
        montos.put(MetodoPago.EFECTIVO, new BigDecimal("1.00"));
        var corte = new Corte(UuidV7.generar(AHORA), negocio, turno.id(), turno.cajaId(), montos, montos,
                BigDecimal.ZERO.setScale(2), null, AHORA, AHORA);
        libro().guardarCorte(corte);

        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.intentar("update caja.movimiento_caja set monto = 99 where id = ?", movimiento.id()))
                    .isEqualTo("23001");
            assertThat(sesion.intentar("delete from caja.movimiento_caja where id = ?", movimiento.id())).isEqualTo("23001");
            assertThat(sesion.intentar("truncate caja.movimiento_caja")).isEqualTo("23001");
            assertThat(sesion.intentar("delete from caja.corte where id = ?", corte.id())).isEqualTo("23001");
            assertThat(sesion.intentar("update caja.corte set diferencia_efectivo = 1 where id = ?", corte.id()))
                    .isEqualTo("23001");
        }
        try (var sesion = SesionSql.comoAplicacion().enNegocio(negocio)) {
            assertThat(sesion.intentar("update caja.movimiento_caja set monto = 99 where id = ?", movimiento.id()))
                    .isEqualTo("42501");
            assertThat(sesion.intentar("delete from caja.turno where id = ?", turno.id())).isEqualTo("42501");
            assertThat(sesion.intentar("update caja.corte set diferencia_efectivo = 1 where id = ?", corte.id()))
                    .isEqualTo("42501");
        }
    }

    @Test
    @DisplayName("el evento procesado no lleva negocio: el candado es del evento y no se puede editar")
    void eventoProcesado() throws Exception {
        var evento = UuidV7.generar(AHORA);
        assertThat(libro().registrarEvento("caja.ventas", evento)).isTrue();

        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.intentar("update caja.evento_procesado set consumidor = 'x'")).isEqualTo("42501");
        }
    }
}
