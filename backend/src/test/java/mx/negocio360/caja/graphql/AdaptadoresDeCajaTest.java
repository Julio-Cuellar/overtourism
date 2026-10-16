package mx.negocio360.caja.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.SaldoTurno;
import mx.negocio360.caja.aplicacion.CerrarTurnoComando;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.seguridad.Autorizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.graphql.execution.ErrorType;

/** Los adaptadores de Caja hacia GraphQL: cinco variantes, el conteo del corte y la forma de las respuestas. */
class AdaptadoresDeCajaTest {

    private final CajaApi api = mock(CajaApi.class);
    private final ComandosDeCaja comandos = new ComandosDeCaja(api);
    private final CajaGraphQlController consultas = new CajaGraphQlController(api);
    private final ContextoNegocio contexto = new ContextoNegocio(UUID.randomUUID(), "actor");
    private final Autorizacion permisos = Autorizacion.de(Set.of("caja.turno.ver"));
    private final EntradaGraphQl entrada = EntradaGraphQl.de(UUID.randomUUID(), contexto, permisos, null);

    private static void rechaza(Runnable accion, ComandoGraphQlException.Codigo esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ComandoGraphQlException.class,
                e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    private void apiPreparaBien() {
        when(api.preparar(any(), any(), any())).thenReturn(new PreparacionCreada("tok", "resumen",
                Instant.parse("2026-10-16T12:00:00Z")));
    }

    @Test
    @DisplayName("declara su prefijo y sus cinco variantes, y exige sesion para confirmar")
    void identidad() {
        assertThat(comandos.prefijo()).isEqualTo("caja.");
        assertThat(comandos.variantes()).containsExactlyInAnyOrder("crearCaja", "abrirTurno", "registrarMovimientoCaja",
                "cerrarTurno", "descartarCorte");
        assertThat(comandos.confirmaSinSesion()).isFalse();
    }

    @Test
    @DisplayName("convierte cada variante y entrega la sesion a la API")
    void preparar_cadaVariante() {
        apiPreparaBien();
        var turno = UUID.randomUUID().toString();

        comandos.preparar(entrada, "crearCaja", Map.of("nombre", "Mostrador"));
        comandos.preparar(entrada, "abrirTurno", Map.of("cajaId", turno, "fondoInicial", new BigDecimal("300.00")));
        comandos.preparar(entrada, "registrarMovimientoCaja", Map.of("turnoId", turno, "tipo", "ENTRADA",
                "monto", new BigDecimal("10.00"), "nota", "Cambio"));
        comandos.preparar(entrada, "descartarCorte", Map.of("turnoId", turno));

        verify(api, org.mockito.Mockito.times(4)).preparar(eq(contexto), eq(permisos), any());
    }

    @Test
    @DisplayName("el conteo del corte llega como lista de montos por metodo y se convierte en un mapa")
    void cerrarTurno_conteo() {
        apiPreparaBien();
        var captor = ArgumentCaptor.forClass(mx.negocio360.compartido.comandos.Comando.class);
        var turno = UUID.randomUUID().toString();

        comandos.preparar(entrada, "cerrarTurno", Map.of("turnoId", turno, "autorizar", true, "contado",
                List.of(Map.of("metodo", "EFECTIVO", "monto", new BigDecimal("400.00")),
                        Map.of("metodo", "TARJETA", "monto", new BigDecimal("10.00")))));

        verify(api).preparar(eq(contexto), eq(permisos), captor.capture());
        var comando = (CerrarTurnoComando) captor.getValue();
        assertThat(comando.autorizar()).isTrue();
        assertThat(comando.contadoPorMetodo()).containsEntry(MetodoPago.EFECTIVO, new BigDecimal("400.00"))
                .containsEntry(MetodoPago.TARJETA, new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("un metodo repetido en el conteo, un metodo desconocido o un campo de mas invalidan el comando")
    void preparar_invalidos() {
        var turno = UUID.randomUUID().toString();
        var repetido = List.of(Map.of("metodo", "EFECTIVO", "monto", new BigDecimal("1.00")),
                Map.of("metodo", "EFECTIVO", "monto", new BigDecimal("2.00")));

        rechaza(() -> comandos.preparar(entrada, "cerrarTurno", Map.of("turnoId", turno, "contado", repetido)),
                ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> comandos.preparar(entrada, "cerrarTurno", Map.of("turnoId", turno, "contado",
                List.of(Map.of("metodo", "BITCOIN", "monto", new BigDecimal("1.00"))))),
                ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> comandos.preparar(entrada, "crearCaja", Map.of("nombre", "X", "negocioId", "otro")),
                ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> comandos.preparar(entrada, "registrarVenta", Map.of()),
                ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
    }

    @Test
    @DisplayName("confirmar entrega el token sin prefijo")
    void confirmar() {
        when(api.confirmar(any(), any(), any())).thenReturn(ResultadoComando.ok("Hecho.", UUID.randomUUID(),
                Map.of("caja_id", "x")));

        var resultado = comandos.confirmar(entrada, "abc");

        verify(api).confirmar(contexto, permisos, "abc");
        assertThat(resultado.datos()).hasSize(1);
    }

    @Test
    @DisplayName("las consultas devuelven lo de la API y los montos por metodo como lista ordenada")
    void consultas() {
        var turnoId = UUID.randomUUID();
        var cajaId = UUID.randomUUID();
        var ahora = Instant.parse("2026-10-16T12:00:00Z");
        var turno = new Turno(turnoId, contexto.negocioId(), cajaId, "cuenta-1", EstadoTurno.ABIERTO,
                new BigDecimal("300.00"), ahora, null, null, 1);
        var montos = Map.of(MetodoPago.TARJETA, new BigDecimal("1.00"), MetodoPago.EFECTIVO, new BigDecimal("2.00"));
        var corte = new Corte(UUID.randomUUID(), contexto.negocioId(), turnoId, cajaId, montos, montos,
                new BigDecimal("0.00"), null, ahora, ahora);
        when(api.cajas(any(), any())).thenReturn(List.of());
        when(api.turnoActivo(any(), any(), any())).thenReturn(Optional.of(turno));
        when(api.saldoTurno(any(), any(), any())).thenReturn(new SaldoTurno(turnoId, cajaId, EstadoTurno.ABIERTO,
                montos, List.of(), 0));
        when(api.cortes(any(), any(), any(), eq(5), any())).thenReturn(new Pagina<>(List.of(corte), false, null));
        when(api.movimientos(any(), any(), any(), eq(5), any())).thenReturn(new Pagina<>(List.of(), true, "c"));

        assertThat(consultas.cajas(entrada)).isEmpty();
        assertThat(consultas.turnoActivo(cajaId, entrada)).isEqualTo(turno);
        assertThat(consultas.abiertoPor(turno)).isEqualTo("cuenta-1");
        var saldo = consultas.saldoTurno(turnoId, entrada);
        assertThat(consultas.esperadoDelSaldo(saldo)).extracting(m -> m.get("metodo").toString())
                .containsExactly("EFECTIVO", "TARJETA");
        var cortes = consultas.cortes(new FiltroCortes(cajaId, null, null), 5, null, entrada);
        assertThat(cortes.cortes()).containsExactly(corte);
        assertThat(consultas.esperadoDelCorte(corte)).hasSize(2);
        assertThat(consultas.contadoDelCorte(corte)).hasSize(2);
        assertThat(consultas.movimientos(saldo, 5, null, entrada).pageInfo()).containsEntry("hayMas", true);
    }

    @Test
    @DisplayName("el error de dominio sale con su codigo; no encontrado es NOT_FOUND y concurrencia es reintentable")
    void traductor() {
        var traductor = new TraductorDeErroresDeCaja();
        var id = UUID.randomUUID();

        var noEncontrado = traductor.traducir(new CajaException(CodigoError.TURNO_NO_ENCONTRADO, id)).orElseThrow();
        var reintento = traductor.traducir(new CajaException(CodigoError.ERROR_TEMPORAL)).orElseThrow();
        var peticion = traductor.traducir(new CajaException(CodigoError.TURNO_YA_ABIERTO)).orElseThrow();

        assertThat(noEncontrado.clasificacion()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(noEncontrado.extensiones()).containsEntry("recursoId", id.toString());
        assertThat(reintento.extensiones()).containsEntry("reintentable", true);
        assertThat(peticion.clasificacion()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(traductor.traducir(new RuntimeException("otro"))).isEmpty();
    }
}
