package mx.negocio360.ventas.graphql;

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
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.ResumenTurno;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.aplicacion.AnularVentaComando;
import mx.negocio360.ventas.aplicacion.RegistrarVentaComando;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Los adaptadores de Ventas hacia GraphQL: conversion de comandos, entrega de la sesion y forma de las respuestas. */
class AdaptadoresDeVentasTest {

    private final VentasApi api = mock(VentasApi.class);
    private final ComandosDeVentas comandos = new ComandosDeVentas(api);
    private final VentasGraphQlController consultas = new VentasGraphQlController(api);
    private final ContextoNegocio contexto = new ContextoNegocio(UUID.randomUUID(), "actor");
    private final Autorizacion permisos = Autorizacion.de(Set.of("ventas.venta.ver"));
    private final EntradaGraphQl entrada = EntradaGraphQl.de(UUID.randomUUID(), contexto, permisos, null);

    private static void rechaza(Runnable accion, ComandoGraphQlException.Codigo esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ComandoGraphQlException.class,
                e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    @Test
    @DisplayName("declara su prefijo y sus dos variantes, y exige sesion para confirmar")
    void identidad() {
        assertThat(comandos.prefijo()).isEqualTo("ventas.");
        assertThat(comandos.variantes()).containsExactlyInAnyOrder("registrarVenta", "anularVenta");
        assertThat(comandos.confirmaSinSesion()).isFalse();
    }

    @Test
    @DisplayName("convierte los campos al comando y entrega contexto y permisos de la sesion a la API")
    void preparar_convierteYEntregaLaSesion() {
        when(api.preparar(any(), any(), any())).thenReturn(new PreparacionCreada("tok", "resumen",
                Instant.parse("2026-10-16T12:00:00Z")));
        var turno = UUID.randomUUID();
        var producto = UUID.randomUUID();
        var captor = ArgumentCaptor.forClass(mx.negocio360.compartido.comandos.Comando.class);

        var vista = comandos.preparar(entrada, "registrarVenta", Map.of("turnoId", turno.toString(),
                "lineas", List.of(Map.of("productoId", producto.toString(), "cantidad", new BigDecimal("2"))),
                "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", new BigDecimal("24.00")))));
        comandos.preparar(entrada, "anularVenta", Map.of("ventaId", producto.toString(), "motivo", "Error"));

        verify(api, org.mockito.Mockito.times(2)).preparar(eq(contexto), eq(permisos), captor.capture());
        assertThat(captor.getAllValues().get(0)).isInstanceOf(RegistrarVentaComando.class);
        assertThat(((RegistrarVentaComando) captor.getAllValues().get(0)).turnoId()).isEqualTo(turno);
        assertThat(captor.getAllValues().get(1)).isInstanceOf(AnularVentaComando.class);
        assertThat(vista.token()).isEqualTo("tok");
        assertThat(vista.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("una variante desconocida o un campo que el comando no tiene se rechazan")
    void preparar_invalidos() {
        rechaza(() -> comandos.preparar(entrada, "crearCaja", Map.of()),
                ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        rechaza(() -> comandos.preparar(entrada, "anularVenta", Map.of("ventaId", UUID.randomUUID().toString(),
                "motivo", "x", "negocioId", "otro")), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
    }

    @Test
    @DisplayName("confirmar entrega el token sin prefijo y devuelve los datos ordenados")
    void confirmar() {
        var id = UUID.randomUUID();
        when(api.confirmar(any(), any(), any())).thenReturn(ResultadoComando.ok("Venta registrada.", id,
                Map.of("total", "66.00", "folio", "1")));

        var resultado = comandos.confirmar(entrada, "abc");

        verify(api).confirmar(contexto, permisos, "abc");
        assertThat(resultado.recursoId()).isEqualTo(id);
        assertThat(resultado.datos()).extracting(d -> d.clave()).containsExactly("folio", "total");
    }

    @Test
    @DisplayName("las consultas toman el negocio de la sesion y devuelven lo que la API entrega")
    void consultas() {
        var id = UUID.randomUUID();
        when(api.venta(any(), any(), any())).thenReturn(Optional.empty());
        when(api.ventas(any(), any(), any(), eq(5), any())).thenReturn(new Pagina<>(List.of(), true, "cursor"));
        when(api.resumenTurno(any(), any(), any())).thenReturn(new ResumenTurno(id, 1, 0,
                Map.of("TARJETA", new BigDecimal("10.00"), "EFECTIVO", new BigDecimal("56.00")), 0));
        when(api.productosParaVenta(any(), any(), any(), eq(3))).thenReturn(List.of());

        assertThat(consultas.venta(id, entrada)).isNull();
        var pagina = consultas.ventas(new FiltroVentas(id, null, null, null, null, null), 5, null, entrada);
        assertThat(pagina.pageInfo()).containsEntry("hayMas", true).containsEntry("cursorFinal", "cursor");
        var resumen = consultas.resumenTurno(id, entrada);
        assertThat(consultas.totalesPorMetodo(resumen)).extracting(m -> m.get("metodo").toString())
                .containsExactly("EFECTIVO", "TARJETA");
        assertThat(consultas.productosParaVenta("taco", 3, entrada)).isEmpty();
    }

    @Test
    @DisplayName("el error de dominio sale con su codigo y texto fijos, con la regla y si es reintentable")
    void traductor() {
        var traductor = new TraductorDeErroresDeVentas();
        var id = UUID.randomUUID();

        var normal = traductor.traducir(new VentasException(CodigoError.VENTA_NO_ENCONTRADA, id)).orElseThrow();
        var reintento = traductor.traducir(new VentasException(CodigoError.CONCURRENCIA)).orElseThrow();
        var peticion = traductor.traducir(new VentasException(CodigoError.PAGOS_NO_CUADRAN)).orElseThrow();

        assertThat(normal.codigo()).isEqualTo("VENTA_NO_ENCONTRADA");
        assertThat(normal.clasificacion()).isEqualTo(org.springframework.graphql.execution.ErrorType.NOT_FOUND);
        assertThat(normal.extensiones()).containsEntry("recursoId", id.toString()).containsKey("regla");
        assertThat(reintento.extensiones()).containsEntry("reintentable", true);
        assertThat(peticion.clasificacion()).isEqualTo(org.springframework.graphql.execution.ErrorType.BAD_REQUEST);
        assertThat(peticion.mensaje()).isNotBlank();
        assertThat(traductor.traducir(new RuntimeException("otro"))).isEmpty();
    }
}
