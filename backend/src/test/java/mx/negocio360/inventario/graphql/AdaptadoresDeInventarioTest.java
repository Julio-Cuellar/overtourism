package mx.negocio360.inventario.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.ArticuloVista;
import mx.negocio360.inventario.aplicacion.Comando;
import mx.negocio360.inventario.aplicacion.CrearArticuloComando;
import mx.negocio360.inventario.aplicacion.Pagina;
import mx.negocio360.inventario.aplicacion.PreparacionCreada;
import mx.negocio360.inventario.aplicacion.RegistrarEntradaComando;
import mx.negocio360.inventario.aplicacion.ResultadoComando;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Los adaptadores de Inventario hacia GraphQL: ocho variantes, permisos en las dos puntas y consultas acotadas. */
class AdaptadoresDeInventarioTest {

    private final InventarioApi api = mock(InventarioApi.class);
    private final ComandosDeInventario comandos = new ComandosDeInventario(api);
    private final InventarioGraphQlController consultas = new InventarioGraphQlController(api);
    private final ContextoNegocio contexto = new ContextoNegocio(UUID.randomUUID(), "actor");

    private EntradaGraphQl con(String... permisos) {
        return EntradaGraphQl.de(UUID.randomUUID(), contexto, Autorizacion.de(Set.of(permisos)), null);
    }

    private static void rechaza(Runnable accion, ComandoGraphQlException.Codigo esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ComandoGraphQlException.class,
                e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    private void apiPreparaBien() {
        when(api.preparar(any(), any())).thenReturn(new PreparacionCreada("tok", "resumen",
                Instant.parse("2026-10-16T12:00:00Z"), List.of()));
    }

    @Test
    @DisplayName("declara su prefijo y sus ocho variantes, y exige sesion para confirmar")
    void identidad() {
        assertThat(comandos.prefijo()).isEqualTo("inventario.");
        assertThat(comandos.variantes()).hasSize(8).contains("crearArticulo", "ajustarPorConteo");
        assertThat(comandos.confirmaSinSesion()).isFalse();
    }

    @Test
    @DisplayName("cada variante se convierte a su comando y pide el permiso que le toca")
    void preparar_conPermiso() {
        apiPreparaBien();
        var articulo = UUID.randomUUID().toString();
        var captor = ArgumentCaptor.forClass(Comando.class);

        comandos.preparar(con("inventario.articulo.gestionar"), "crearArticulo", Map.of("nombre", "Harina",
                "unidadBase", "g", "unidadesPropias", List.of(Map.of("nombre", "costal", "factor",
                        new BigDecimal("25000")))));
        comandos.preparar(con("inventario.movimiento.registrar"), "registrarEntradaInventario", Map.of(
                "articuloId", articulo, "cantidad", new BigDecimal("2"), "unidad", "kg", "costoUnitario",
                new BigDecimal("20.00"), "ocurridoEn", Instant.parse("2026-10-15T12:00:00Z")));
        comandos.preparar(con("inventario.movimiento.registrar"), "registrarSalidaInventario", Map.of(
                "articuloId", articulo, "cantidad", new BigDecimal("1"), "unidad", "kg"));
        comandos.preparar(con("inventario.movimiento.registrar"), "registrarMermaInventario", Map.of(
                "articuloId", articulo, "cantidad", new BigDecimal("1"), "unidad", "kg", "motivo", "DANO"));
        comandos.preparar(con("inventario.conteo.ajustar"), "ajustarPorConteo", Map.of(
                "articuloId", articulo, "cantidadContada", new BigDecimal("5"), "unidad", "kg"));
        comandos.preparar(con("inventario.articulo.gestionar"), "editarArticulo", Map.of("articuloId", articulo,
                "nombre", "Harina fina"));
        comandos.preparar(con("inventario.articulo.gestionar"), "definirUnidadPropia", Map.of("articuloId", articulo,
                "nombre", "bulto", "factor", new BigDecimal("50000")));
        comandos.preparar(con("inventario.articulo.gestionar"), "bajaArticulo", Map.of("articuloId", articulo));

        verify(api, org.mockito.Mockito.times(8)).preparar(any(), captor.capture());
        assertThat(captor.getAllValues().get(0)).isInstanceOf(CrearArticuloComando.class);
        assertThat(captor.getAllValues().get(1)).isInstanceOf(RegistrarEntradaComando.class);
    }

    @Test
    @DisplayName("sin el permiso de la variante no se llega a la API; una variante desconocida o un campo de mas se rechazan")
    void preparar_invalidos() {
        assertThatThrownBy(() -> comandos.preparar(con("inventario.articulo.ver"), "crearArticulo",
                Map.of("nombre", "Sal", "unidadBase", "g"))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> comandos.preparar(con(), "ajustarPorConteo", Map.of("articuloId",
                UUID.randomUUID().toString(), "cantidadContada", new BigDecimal("1"), "unidad", "kg")))
                .isInstanceOf(PermisoDenegadoException.class);
        verify(api, never()).preparar(any(), any());
        rechaza(() -> comandos.preparar(con(), "registrarVenta", Map.of()),
                ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        rechaza(() -> comandos.preparar(con("inventario.articulo.gestionar"), "crearArticulo",
                Map.of("nombre", "Sal", "unidadBase", "g", "negocioId", "otro")),
                ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
    }

    @Test
    @DisplayName("al confirmar se exige el permiso del comando que guarda el token, antes de gastarlo")
    void confirmar() {
        when(api.tipoDeComando(any(), any())).thenReturn(Optional.of("ajuste_por_conteo"));
        when(api.confirmar(any(), any())).thenReturn(ResultadoComando.ok("Hecho.", UUID.randomUUID()));

        assertThatThrownBy(() -> comandos.confirmar(con("inventario.movimiento.registrar"), "tok"))
                .isInstanceOf(PermisoDenegadoException.class);
        verify(api, never()).confirmar(any(), any());
        assertThat(comandos.confirmar(con("inventario.conteo.ajustar"), "tok").ok()).isTrue();

        when(api.tipoDeComando(any(), any())).thenReturn(Optional.empty());
        comandos.confirmar(con(), "otro");
        verify(api).confirmar(any(), org.mockito.ArgumentMatchers.eq("otro"));
    }

    @Test
    @DisplayName("las consultas piden inventario.articulo.ver y traducen un argumento invalido a CONSULTA_INVALIDA")
    void consultas() {
        var id = UUID.randomUUID();
        when(api.articulos(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(new Pagina<>(List.of(), true, "c"));
        when(api.articulo(any(), any())).thenReturn(Optional.empty());
        when(api.kardex(any(), any(), anyInt(), any())).thenReturn(new Pagina<>(List.of(), false, null));

        assertThat(consultas.inventario(null, false, 5, null, con("inventario.articulo.ver")).pageInfo())
                .containsEntry("hayMas", true).containsEntry("cursorFinal", "c");
        assertThat(consultas.articulo(id, con("inventario.articulo.ver"))).isNull();
        assertThat(consultas.kardex(id, 5, null, con("inventario.articulo.ver")).nodos()).isEmpty();
        assertThatThrownBy(() -> consultas.inventario(null, false, 5, null, con()))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> consultas.articulo(id, con())).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> consultas.kardex(id, 5, null, con())).isInstanceOf(PermisoDenegadoException.class);

        when(api.articulos(any(), any(), anyBoolean(), anyInt(), any())).thenThrow(new IllegalArgumentException("x"));
        when(api.kardex(any(), any(), anyInt(), any())).thenThrow(new IllegalArgumentException("x"));
        rechaza(() -> consultas.inventario("x", false, 5, null, con("inventario.articulo.ver")),
                ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
        rechaza(() -> consultas.kardex(id, 0, null, con("inventario.articulo.ver")),
                ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
    }

    @Test
    @DisplayName("las unidades propias salen como lista ordenada por nombre")
    void unidadesPropias() {
        var propias = new TreeMap<String, BigDecimal>(Map.of("costal", new BigDecimal("25000"),
                "bulto", new BigDecimal("50000")));
        var articulo = new ArticuloVista(UUID.randomUUID(), "Harina", null, "g", null, propias, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, true, false, 1);

        assertThat(consultas.unidadesPropias(articulo)).extracting(InventarioGraphQlController.UnidadPropia::nombre)
                .containsExactly("bulto", "costal");
    }
}
