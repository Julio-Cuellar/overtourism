package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.dominio.VentaAplicada;
import mx.negocio360.caja.infraestructura.LibroDeCajaJdbc;
import mx.negocio360.caja.infraestructura.RepositorioCajaJdbc;
import mx.negocio360.compartido.comandos.Preparacion;
import mx.negocio360.compartido.comandos.RepositorioPreparacionesJdbc;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.infraestructura.ContratoAlmacenDeInventarioTest;
import mx.negocio360.inventario.infraestructura.InventarioJdbc;
import mx.negocio360.inventario.infraestructura.PreparacionesJdbc;
import mx.negocio360.ventas.infraestructura.ContratoRepositorioVentasTest;
import mx.negocio360.ventas.infraestructura.RepositorioVentasJdbc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * SG-03 y SC-04 para Ventas, Caja e Inventario: la seguridad por fila se prueba tabla por tabla con dos negocios y con el
 * rol de la aplicacion (que no es superusuario). Cada tabla con {@code negocio_id} tiene la seguridad activada y FORZADA,
 * y la lista de abajo se compara con las tablas que existen en la base: si alguien agrega una y olvida la seguridad, esta
 * prueba falla. Cada negocio ve solo lo suyo, sin negocio no se ve nada, y desde un negocio no se escribe en otro.
 */
@SpringBootTest
class AislamientoVentasCajaInventarioPostgresTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");
    private static final List<String> TABLAS = List.of("ventas.serie_folio", "ventas.venta", "ventas.linea_venta",
            "ventas.pago_venta", "ventas.preparacion", "caja.caja", "caja.turno", "caja.movimiento_caja", "caja.corte",
            "caja.venta_aplicada", "caja.preparacion", "inventario.articulo", "inventario.saldo", "inventario.movimiento",
            "inventario.preparacion", "inventario.consumo");

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    static Stream<String> tablas() {
        return TABLAS.stream();
    }

    /** Siembra una fila en cada tabla de los tres modulos, para el negocio dado. */
    private void sembrar(UUID negocio) {
        var ejecutor = new EjecutorEnNegocio(jdbc, transacciones);
        var ventas = new RepositorioVentasJdbc(jdbc, ejecutor);
        var cajas = new RepositorioCajaJdbc(jdbc, ejecutor);
        var libro = new LibroDeCajaJdbc(jdbc, ejecutor);
        var inventario = new InventarioJdbc(jdbc, ejecutor);

        var turnoId = UUID.randomUUID();
        ventas.siguienteFolio(negocio, "A");
        ventas.guardar(ContratoRepositorioVentasTest.venta(negocio, turnoId, "A", 1, AHORA));
        var token = TokenOpaco.hash(TokenOpaco.nuevo());
        new RepositorioPreparacionesJdbc(jdbc, ejecutor, "ventas").guardar(new Preparacion(token, negocio, "a", "t", "h",
                "{}", "r", null, AHORA.plusSeconds(60), null));

        var caja = new Caja(UuidV7.generar(AHORA), negocio, "Mostrador", true);
        cajas.guardarCaja(caja);
        var turno = new Turno(UuidV7.generar(AHORA), negocio, caja.id(), "cajero", EstadoTurno.ABIERTO,
                new BigDecimal("500.00"), AHORA, null, null, 1);
        cajas.guardarTurno(turno);
        libro.agregar(new MovimientoCaja(UuidV7.generar(AHORA), negocio, turno.id(), TipoMovimiento.FONDO,
                MetodoPago.EFECTIVO, new BigDecimal("500.00"), null, null, null, AHORA, "cajero"));
        var montos = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
        montos.put(MetodoPago.EFECTIVO, new BigDecimal("500.00"));
        libro.guardarCorte(new Corte(UuidV7.generar(AHORA), negocio, turno.id(), caja.id(), montos, montos,
                BigDecimal.ZERO.setScale(2), null, AHORA, AHORA));
        libro.guardarVentaAplicada(new VentaAplicada(negocio, UUID.randomUUID(), turno.id(), EstadoVentaAplicada.APLICADA));
        new RepositorioPreparacionesJdbc(jdbc, ejecutor, "caja").guardar(new Preparacion(
                TokenOpaco.hash(TokenOpaco.nuevo()), negocio, "a", "t", "h", "{}", "r", null, AHORA.plusSeconds(60), null));

        var articulo = ContratoAlmacenDeInventarioTest.articulo(negocio, "Frijol", "0");
        inventario.insertar(articulo);
        inventario.insertar(Saldo.inicial(articulo.id(), negocio));
        inventario.insertar(new Movimiento(UuidV7.generar(AHORA), negocio, articulo.id(),
                mx.negocio360.inventario.dominio.TipoMovimiento.ENTRADA, BigDecimal.ONE.setScale(4), "g",
                BigDecimal.ONE.setScale(8), BigDecimal.ONE.setScale(4), BigDecimal.ONE.setScale(4),
                BigDecimal.ONE.setScale(2), BigDecimal.ONE.setScale(4), BigDecimal.ONE.setScale(4), CausaTipo.MANUAL,
                null, null, AHORA, AHORA, "x"));
        inventario.insertar(mx.negocio360.inventario.dominio.Consumo.marcaDeAnulacion(negocio, UUID.randomUUID(),
                "restaurantes", AHORA));
        new PreparacionesJdbc(jdbc, ejecutor).guardar(new mx.negocio360.inventario.aplicacion.Preparacion(
                TokenOpaco.hash(TokenOpaco.nuevo()), negocio, "a", "t", "h", "{}", "r", AHORA.plusSeconds(60), null));
    }

    private static long contar(SesionSql sesion, String tabla) throws Exception {
        return sesion.consultar("select count(*) from " + tabla, Long.class);
    }

    @Test
    @DisplayName("la lista de tablas de esta prueba es la de las tablas con negocio_id de Ventas, Caja e Inventario")
    void laListaEsCompleta() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var existentes = sesion.columna("select table_schema || '.' || table_name from information_schema.columns "
                    + "where table_schema in ('ventas', 'caja', 'inventario') and column_name = 'negocio_id'", String.class);
            assertThat(existentes).containsExactlyInAnyOrderElementsOf(TABLAS);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    @DisplayName("la seguridad por fila esta activada y forzada, con su politica de aislamiento por negocio")
    void seguridadForzada(String tabla) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var banderas = sesion.consultar("select relrowsecurity::text || '/' || relforcerowsecurity::text from pg_class "
                    + "where oid = ?::regclass", String.class, tabla);
            var politicas = sesion.consultar("select count(*) from pg_policies where schemaname || '.' || tablename = ? "
                    + "and policyname = 'aislamiento_por_negocio'", Long.class, tabla);
            assertThat(banderas).isEqualTo("true/true");
            assertThat(politicas).isEqualTo(1L);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    @DisplayName("SG-03: cada negocio ve solo sus filas, y sin negocio en el contexto no ve ninguna")
    void lecturaAislada(String tabla) throws Exception {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        sembrar(a);
        sembrar(b);

        try (var sesion = SesionSql.comoAplicacion()) {
            var deA = contar(sesion.enNegocio(a), tabla);
            var ajenasParaA = sesion.consultar("select count(*) from " + tabla + " where negocio_id <> ?", Long.class, a);
            assertThat(deA).as("negocio A").isPositive();
            assertThat(ajenasParaA).as("filas de otro negocio que A ve").isZero();
            assertThat(contar(sesion.enNegocio(b), tabla)).as("negocio B").isEqualTo(deA);
            assertThat(contar(sesion.sinNegocio(), tabla)).as("sin negocio").isZero();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    @DisplayName("SG-03: desde un negocio no se modifica ni se borra ninguna fila de otro, ni se escribe en su nombre")
    void escrituraAislada(String tabla) throws Exception {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        sembrar(a);
        sembrar(b);

        try (var sesion = SesionSql.comoAplicacion()) {
            var antes = contar(sesion.enNegocio(a), tabla);
            sesion.enNegocio(b);
            var actualizar = sesion.intentar("update " + tabla + " set negocio_id = negocio_id where negocio_id = ?", a);
            var borrar = sesion.intentar("delete from " + tabla + " where negocio_id = ?", a);

            // Con el permiso o sin el, la sentencia no toca filas de A: falla por permiso o por disparador, o afecta 0 filas.
            assertThat(actualizar).isIn(null, "42501", "23001");
            assertThat(borrar).isIn(null, "42501", "23001");
            assertThat(contar(sesion.enNegocio(a), tabla)).as("A conserva sus filas").isEqualTo(antes);
        }
    }

    @Test
    @DisplayName("SC-04: el negocio de una transaccion no se filtra a la siguiente de la misma conexion")
    void negocioNoSeFiltra() throws Exception {
        var negocio = UUID.randomUUID();
        sembrar(negocio);

        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(contar(sesion.enNegocio(negocio), "ventas.venta")).isPositive();
            sesion.confirmar();
            assertThat(contar(sesion, "ventas.venta")).as("la transaccion siguiente no ve nada").isZero();
        }
    }
}
