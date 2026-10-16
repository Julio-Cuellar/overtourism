package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.ConsentimientoSolicitado;
import mx.negocio360.clientes.aplicacion.PublicarAvisoComando;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.FinalidadAviso;
import mx.negocio360.clientes.eventos.ClienteConsentimientoCambiado;
import mx.negocio360.clientes.eventos.ClienteRegistrado;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Clientes de punta a punta con la API publica, PostgreSQL y el outbox: preparar y confirmar con token de un solo uso,
 * el alta con sus consentimientos en una sola transaccion con sus eventos, y la busqueda. Cada prueba usa su negocio.
 */
@SpringBootTest
class ClientesDePuntaAPuntaPostgresTest {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    @Autowired
    private ClientesApi api;

    private static PublicarAvisoComando aviso() {
        return new PublicarAvisoComando("Taller Mixcoatl", "Calle 5 de Mayo 10, Puebla",
                List.of(new FinalidadAviso("VENTA", false), new FinalidadAviso("PROMOCIONES", true)),
                "Usamos tu nombre y telefono para atenderte.", "Aviso integral de ejemplo.", "privacidad@taller.test");
    }

    private static RegistrarClienteComando alta(String nombre, String telefono, int avisoVersion) {
        return new RegistrarClienteComando(nombre, "Pérez", telefono, null, null, null, avisoVersion, List.of(
                new ConsentimientoSolicitado(Finalidad.CONTACTO_MENSAJERIA, MedioConsentimiento.VERBAL),
                new ConsentimientoSolicitado(Finalidad.PROMOCIONES, MedioConsentimiento.PANEL)));
    }

    private ContextoNegocio negocioConAviso() {
        var contexto = new ContextoNegocio(UUID.randomUUID(), "locatario-prueba");
        api.confirmar(contexto, api.preparar(contexto, aviso()).token());
        return contexto;
    }

    private static Boolean completado(String tipo, UUID negocioId) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var filas = sesion.columna("select bool_and(completion_date is not null) from plataforma.event_publication "
                    + "where serialized_event like '%\"tipo\":\"' || ? || '\"%' and serialized_event like '%' || ? || '%'",
                    Boolean.class, tipo, negocioId.toString());
            return filas.isEmpty() ? null : filas.get(0);
        }
    }

    private static int filasDelTipo(String tipo, UUID negocioId) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            // Cada modulo que escucha el bus deja su propia fila por evento: se cuentan los eventos, no las filas.
            return sesion.consultar("select count(distinct serialized_event) from plataforma.event_publication "
                    + "where serialized_event like '%\"tipo\":\"' || ? || '\"%' and serialized_event like '%' || ? "
                    + "|| '%'", Long.class, tipo, negocioId.toString()).intValue();
        }
    }

    @Test
    @DisplayName("publicar el aviso por preparar y confirmar lo deja vigente en la base")
    void aviso_publicarPorPrepararYConfirmar() {
        var contexto = negocioConAviso();

        var vigente = api.avisoVigente(contexto).orElseThrow();

        assertThat(vigente.version()).isEqualTo(1);
        assertThat(vigente.responsableNombre()).isEqualTo("Taller Mixcoatl");
    }

    @Test
    @DisplayName("registrar un cliente lo guarda, deja sus eventos en el outbox y el cliente se encuentra por nombre sin acentos")
    void alta_guardaYPublicaEnElOutbox() throws Exception {
        var contexto = negocioConAviso();
        var preparacion = api.preparar(contexto, alta("José", FilasClientes.telefono(), 1));

        var resultado = api.confirmar(contexto, preparacion.token());

        assertThat(resultado.recursoId()).isNotNull();
        var encontrados = api.buscar(contexto, "jose perez", 10, null);
        assertThat(encontrados.nodos()).extracting(n -> n.id()).containsExactly(resultado.recursoId());
        await().atMost(ESPERA).untilAsserted(() -> {
            assertThat(completado(ClienteRegistrado.TIPO, contexto.negocioId())).isTrue();
            assertThat(filasDelTipo(ClienteConsentimientoCambiado.TIPO, contexto.negocioId())).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("un token se usa una sola vez: la segunda confirmacion falla con TOKEN_USADO y no duplica al cliente")
    void token_seUsaUnaVez() {
        var contexto = negocioConAviso();
        var preparacion = api.preparar(contexto, alta("Ana", FilasClientes.telefono(), 1));
        api.confirmar(contexto, preparacion.token());

        assertThatThrownBy(() -> api.confirmar(contexto, preparacion.token()))
                .isInstanceOfSatisfying(ClientesException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.TOKEN_USADO));
        assertThat(api.buscar(contexto, "ana", 10, null).nodos()).hasSize(1);
    }

    @Test
    @DisplayName("el token de un negocio no sirve en otro")
    void token_deOtroNegocio_noSirve() {
        var contexto = negocioConAviso();
        var otro = negocioConAviso();
        var preparacion = api.preparar(contexto, alta("Ana", FilasClientes.telefono(), 1));

        assertThatThrownBy(() -> api.confirmar(otro, preparacion.token()))
                .isInstanceOfSatisfying(ClientesException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("si el alta falla en la confirmacion (telefono repetido) no queda ni cliente ni evento")
    void alta_fallida_noDejaClienteNiEvento() throws Exception {
        var contexto = negocioConAviso();
        var telefono = FilasClientes.telefono();
        var primera = api.preparar(contexto, alta("Ana", telefono, 1));
        var segunda = api.preparar(contexto, alta("Beto", telefono, 1));
        api.confirmar(contexto, primera.token());
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(completado(ClienteRegistrado.TIPO, contexto.negocioId())).isTrue());

        assertThatThrownBy(() -> api.confirmar(contexto, segunda.token()))
                .isInstanceOfSatisfying(ClientesException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.TELEFONO_DUPLICADO));

        assertThat(api.buscar(contexto, "beto", 10, null).nodos()).isEmpty();
        assertThat(filasDelTipo(ClienteRegistrado.TIPO, contexto.negocioId())).isEqualTo(1);
    }

    @Test
    @DisplayName("tras confirmar, la fila de preparacion ya no conserva el comando ni el resumen (SC-10)")
    void preparacion_trasConfirmar_quedaVacia() throws Exception {
        var contexto = negocioConAviso();
        var preparacion = api.preparar(contexto, alta("Ana", FilasClientes.telefono(), 1));
        api.confirmar(contexto, preparacion.token());

        try (var sesion = SesionSql.comoDueno()) {
            var conComando = sesion.columna("select 1 from clientes.preparacion where negocio_id = ?::uuid "
                    + "and comando is not null", Integer.class, contexto.negocioId().toString());
            assertThat(conComando).isEmpty();
        }
    }
}
