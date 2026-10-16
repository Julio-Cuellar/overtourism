package mx.negocio360.clientes.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.ClienteListado;
import mx.negocio360.clientes.aplicacion.EditarClienteComando;
import mx.negocio360.clientes.aplicacion.PreparacionCreada;
import mx.negocio360.clientes.aplicacion.PublicarAvisoComando;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.clientes.aplicacion.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Los adaptadores de Clientes hacia GraphQL: variantes, permisos en las dos puntas y lo que aun no esta programado. */
class AdaptadoresDeClientesTest {

    private final ClientesApi api = mock(ClientesApi.class);
    private final ComandosDeClientes comandos = new ComandosDeClientes(api);
    private final ClientesGraphQlController consultas = new ClientesGraphQlController(api);
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
                Instant.parse("2026-10-16T12:00:00Z"), List.of("aviso")));
    }

    @Test
    @DisplayName("declara su prefijo, sus tres variantes y que exige sesion para confirmar")
    void identidad() {
        assertThat(comandos.prefijo()).isEqualTo("clientes.");
        assertThat(comandos.variantes()).containsExactlyInAnyOrder("registrarCliente", "editarCliente",
                "publicarAvisoPrivacidad");
        assertThat(comandos.confirmaSinSesion()).isFalse();
    }

    @Test
    @DisplayName("prepara cada variante con su comando y exige su permiso")
    void preparar_cadaVariante() {
        apiPreparaBien();
        var id = UUID.randomUUID().toString();
        var captor = ArgumentCaptor.forClass(mx.negocio360.clientes.aplicacion.Comando.class);

        comandos.preparar(con("clientes.cliente.registrar"), "registrarCliente",
                Map.of("nombre", "Ana", "avisoVersion", 1));
        comandos.preparar(con("clientes.cliente.editar"), "editarCliente",
                Map.of("clienteId", id, "version", 1, "nombre", "Ana"));
        var vista = comandos.preparar(con("clientes.aviso.publicar"), "publicarAvisoPrivacidad",
                Map.of("responsableNombre", "Fonda", "responsableDomicilio", "Calle 1", "finalidades",
                        List.of(Map.of("codigo", "VENTA", "requiereConsentimiento", false)),
                        "textoSimplificado", "a", "textoIntegral", "b", "contactoArco", "c@example.com"));

        verify(api, org.mockito.Mockito.times(3)).preparar(any(), captor.capture());
        assertThat(captor.getAllValues()).hasSize(3);
        assertThat(captor.getAllValues().get(0)).isInstanceOf(RegistrarClienteComando.class);
        assertThat(captor.getAllValues().get(1)).isInstanceOf(EditarClienteComando.class);
        assertThat(captor.getAllValues().get(2)).isInstanceOf(PublicarAvisoComando.class);
        assertThat(vista.token()).isEqualTo("tok");
        assertThat(vista.advertencias()).containsExactly("aviso");
    }

    @Test
    @DisplayName("sin el permiso de la variante no se llega a la API")
    void preparar_sinPermiso() {
        assertThatThrownBy(() -> comandos.preparar(con("clientes.sugerir"), "registrarCliente",
                Map.of("nombre", "Ana", "avisoVersion", 1))).isInstanceOf(PermisoDenegadoException.class);
        verify(api, never()).preparar(any(), any());
    }

    @Test
    @DisplayName("una variante sin dueno es COMANDO_NO_DISPONIBLE y un campo de mas, COMANDO_INVALIDO")
    void preparar_invalidos() {
        rechaza(() -> comandos.preparar(con(), "anonimizarCliente", Map.of()),
                ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        rechaza(() -> comandos.preparar(con("clientes.cliente.registrar"), "registrarCliente",
                Map.of("nombre", "Ana", "negocioId", "otro")), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> comandos.preparar(con("clientes.cliente.editar"), "editarCliente",
                Map.of("clienteId", "no-es-uuid", "version", 1)), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
    }

    @Test
    @DisplayName("al confirmar se exige el permiso del comando que guarda el token, antes de gastarlo")
    void confirmar_exigePermisoDelToken() {
        when(api.tipoDeComando(any(), any())).thenReturn(Optional.of("registrar_cliente"));
        when(api.confirmar(any(), any())).thenReturn(ResultadoComando.ok("Hecho.", UUID.randomUUID()));

        assertThatThrownBy(() -> comandos.confirmar(con("clientes.sugerir"), "tok"))
                .isInstanceOf(PermisoDenegadoException.class);
        verify(api, never()).confirmar(any(), any());

        var resultado = comandos.confirmar(con("clientes.cliente.registrar"), "tok");
        assertThat(resultado.ok()).isTrue();
        assertThat(resultado.datos()).isEmpty();
    }

    @Test
    @DisplayName("un token que no es vigente llega a confirmar, que da el error que corresponda")
    void confirmar_tokenSinTipo() {
        when(api.tipoDeComando(any(), any())).thenReturn(Optional.empty());
        when(api.confirmar(any(), any())).thenReturn(ResultadoComando.ok("Hecho.", null));

        comandos.confirmar(con(), "tok");

        verify(api).confirmar(any(), any());
    }

    @Test
    @DisplayName("sin sesion con negocio nada de esto responde")
    void sinNegocio() {
        var sinNegocio = EntradaGraphQl.de(UUID.randomUUID(), null, Autorizacion.de(Set.of()), null);

        assertThatThrownBy(() -> comandos.confirmar(sinNegocio, "tok")).isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> consultas.sugerirClientes("ana", sinNegocio)).isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> consultas.avisoPrivacidadVigente(sinNegocio)).isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> consultas.clientes(sinNegocio)).isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    @DisplayName("sugerirClientes pide su permiso, acota el texto y devuelve a la API")
    void sugerir() {
        var esperado = List.of(new ClienteListado(UUID.randomUUID(), "Ana Perez", "•••• 1234"));
        when(api.sugerir(any(), any())).thenReturn(esperado);

        assertThat(consultas.sugerirClientes("ana", con("clientes.sugerir"))).isEqualTo(esperado);
        assertThatThrownBy(() -> consultas.sugerirClientes("ana", con())).isInstanceOf(PermisoDenegadoException.class);
        rechaza(() -> consultas.sugerirClientes("a", con("clientes.sugerir")),
                ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
        rechaza(() -> consultas.sugerirClientes(null, con("clientes.sugerir")),
                ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
    }

    @Test
    @DisplayName("avisoPrivacidadVigente pide registrar clientes y devuelve nulo si no hay aviso")
    void aviso() {
        when(api.avisoVigente(any())).thenReturn(Optional.empty());

        assertThat(consultas.avisoPrivacidadVigente(con("clientes.cliente.registrar"))).isNull();
        assertThatThrownBy(() -> consultas.avisoPrivacidadVigente(con())).isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("lo que depende de historial, consentimiento y ARCO responde OPERACION_NO_DISPONIBLE")
    void pendientes() {
        var entrada = con();
        for (Runnable consulta : List.<Runnable>of(() -> consultas.clientes(entrada), () -> consultas.cliente(entrada),
                () -> consultas.historialCliente(entrada), () -> consultas.consentimientosCliente(entrada),
                () -> consultas.panoramaClientes(entrada), () -> consultas.solicitudesArco(entrada))) {
            rechaza(consulta, ComandoGraphQlException.Codigo.OPERACION_NO_DISPONIBLE);
        }
    }
}
