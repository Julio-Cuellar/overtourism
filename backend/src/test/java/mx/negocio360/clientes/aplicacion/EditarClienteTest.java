package mx.negocio360.clientes.aplicacion;

import static mx.negocio360.clientes.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.fakes.Escenario;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HTC-27 · I-01 a I-03, I-06 a I-08: EditarCliente con bloqueo optimista. */
class EditarClienteTest {

    private final Escenario e = new Escenario();
    private UUID ana;

    @BeforeEach
    void registrarAna() {
        e.conAviso(e.ctxA);
        ana = e.registrar(e.ctxA, new RegistrarClienteComando("Ana", "Pérez", "222 123 4567", "ana@correo.test",
                LocalDate.of(1990, 5, 1), "nota", 1, null));
    }

    private static EditarClienteComando editar(UUID id, long version, String nombre, String apellidos,
                                               String telefono, String correo, String notas) {
        return new EditarClienteComando(id, version, nombre, apellidos, telefono, correo, null, notas);
    }

    @Test
    @DisplayName("cambia solo los campos enviados y sube la version")
    void editar_cambiosParciales_conservaLoDemasYSubeLaVersion() {
        e.reloj.avanzar(Duration.ofHours(2));

        var resultado = e.confirmar(e.ctxA, editar(ana, 1, "Ana Maria", null, null, null, null));

        var cliente = e.clientes.porId(e.negocioA, ana).orElseThrow();
        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(resultado.recursoId()).isEqualTo(ana);
        assertThat(cliente.datos().nombre()).isEqualTo("Ana Maria");
        assertThat(cliente.datos().apellidos()).isEqualTo("Pérez");
        assertThat(cliente.datos().telefono()).isEqualTo("+522221234567");
        assertThat(cliente.datos().correo()).isEqualTo("ana@correo.test");
        assertThat(cliente.datos().fechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 1));
        assertThat(cliente.datos().notas()).isEqualTo("nota");
        assertThat(cliente.version()).isEqualTo(2L);
        assertThat(cliente.actualizadoEn()).isEqualTo(Fixtures.AHORA.plus(Duration.ofHours(2)));
        assertThat(cliente.creadoEn()).isEqualTo(Fixtures.AHORA);
    }

    @Test
    @DisplayName("un texto vacio en un campo opcional lo borra")
    void editar_textoVacioEnOpcional_loBorra() {
        e.confirmar(e.ctxA, editar(ana, 1, null, " ", "", "", ""));

        var datos = e.clientes.porId(e.negocioA, ana).orElseThrow().datos();
        assertThat(datos.apellidos()).isNull();
        assertThat(datos.telefono()).isNull();
        assertThat(datos.correo()).isNull();
        assertThat(datos.notas()).isNull();
        assertThat(datos.nombre()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("normaliza el telefono y el correo al editar")
    void editar_telefonoYCorreo_losNormaliza() {
        e.confirmar(e.ctxA, editar(ana, 1, null, null, "(999) 888-7766", "NUEVO@Correo.test", null));

        var datos = e.clientes.porId(e.negocioA, ana).orElseThrow().datos();
        assertThat(datos.telefono()).isEqualTo("+529998887766");
        assertThat(datos.correo()).isEqualTo("nuevo@correo.test");
    }

    @Test
    @DisplayName("editar con una version vieja falla con CONCURRENCIA")
    void preparar_versionVieja_fallaConConcurrencia() {
        e.confirmar(e.ctxA, editar(ana, 1, "Ana Maria", null, null, null, null));

        fallaCon(() -> e.api.preparar(e.ctxA, editar(ana, 1, "Otra", null, null, null, null)),
                CodigoError.CONCURRENCIA);
    }

    @Test
    @DisplayName("dos ediciones preparadas con la misma version: la segunda falla con CONCURRENCIA al confirmar")
    void confirmar_dosEdicionesConLaMismaVersion_laSegundaFalla() {
        var primera = e.api.preparar(e.ctxA, editar(ana, 1, "Primera", null, null, null, null));
        var segunda = e.api.preparar(e.ctxA, editar(ana, 1, "Segunda", null, null, null, null));
        e.api.confirmar(e.ctxA, primera.token());

        fallaCon(() -> e.api.confirmar(e.ctxA, segunda.token()), CodigoError.CONCURRENCIA);

        assertThat(e.clientes.porId(e.negocioA, ana).orElseThrow().datos().nombre()).isEqualTo("Primera");
    }

    @Test
    @DisplayName("un cliente anonimizado no se edita")
    void preparar_clienteAnonimizado_fallaConClienteAnonimizado() {
        var anonimizado = Fixtures.anonimizado(e.negocioA);
        e.clientes.insertar(anonimizado);

        fallaCon(() -> e.api.preparar(e.ctxA, editar(anonimizado.id(), 1, "Nuevo", null, null, null, null)),
                CodigoError.CLIENTE_ANONIMIZADO);
    }

    @Test
    @DisplayName("un cliente que no existe, o que es de otro negocio, falla con CLIENTE_NO_ENCONTRADO")
    void preparar_clienteInexistenteOAjeno_fallaConNoEncontrado() {
        fallaCon(() -> e.api.preparar(e.ctxA, editar(UUID.randomUUID(), 1, "X", null, null, null, null)),
                CodigoError.CLIENTE_NO_ENCONTRADO);
        e.conAviso(e.ctxB);
        fallaCon(() -> e.api.preparar(e.ctxB, editar(ana, 1, "X", null, null, null, null)),
                CodigoError.CLIENTE_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("el telefono de otro cliente activo falla con TELEFONO_DUPLICADO y devuelve su id")
    void preparar_telefonoDeOtroCliente_fallaConTelefonoDuplicado() {
        var beto = e.registrar(e.ctxA, Fixtures.registrar("Beto", null, "999 888 7766"));

        var error = fallaCon(() -> e.api.preparar(e.ctxA, editar(beto, 1, null, null, "222 123 4567", null, null)),
                CodigoError.TELEFONO_DUPLICADO);

        assertThat(error.recursoId()).contains(ana);
    }

    @Test
    @DisplayName("repetir el telefono propio no es un duplicado")
    void editar_mismoTelefonoPropio_sePermite() {
        e.confirmar(e.ctxA, editar(ana, 1, "Ana Maria", null, "2221234567", null, null));

        assertThat(e.clientes.porId(e.negocioA, ana).orElseThrow().datos().telefono()).isEqualTo("+522221234567");
    }

    @Test
    @DisplayName("el telefono que alguien libero queda disponible para otro cliente")
    void editar_telefonoLiberado_quedaDisponible() {
        e.confirmar(e.ctxA, editar(ana, 1, null, null, "", null, null));

        var beto = e.registrar(e.ctxA, Fixtures.registrar("Beto", null, "222 123 4567"));

        assertThat(beto).isNotEqualTo(ana);
    }

    @Test
    @DisplayName("aplica las mismas validaciones que el alta")
    void preparar_datosInvalidos_fallaConElCodigoDelAlta() {
        fallaCon(() -> e.api.preparar(e.ctxA, editar(ana, 1, "", null, null, null, null)), CodigoError.NOMBRE_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, editar(ana, 1, null, null, "123", null, null)),
                CodigoError.TELEFONO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, editar(ana, 1, null, null, null, "ana@", null)),
                CodigoError.CORREO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, editar(ana, 1, null, null, null, null, "n".repeat(501))),
                CodigoError.NOTAS_MUY_LARGAS);
        fallaCon(() -> e.api.preparar(e.ctxA, new EditarClienteComando(ana, 1, null, null, null, null,
                Fixtures.HOY.plusDays(1), null)), CodigoError.FECHA_NACIMIENTO_INVALIDA);
    }

    @Test
    @DisplayName("la fecha de nacimiento 'de hoy' es la de Mexico aunque el reloj este en UTC (I-08)")
    void validar_relojEnUtc_usaLaFechaDeMexico() {
        var reloj = java.time.Clock.fixed(java.time.Instant.parse("2026-10-15T01:00:00Z"), java.time.ZoneOffset.UTC);
        var manejador = new ManejadorEditarCliente(e.clientes, reloj);

        fallaCon(() -> manejador.preparar(e.ctxA, new EditarClienteComando(ana, 1, null, null, null, null,
                LocalDate.of(2026, 10, 15), null)), CodigoError.FECHA_NACIMIENTO_INVALIDA);
        assertThat(manejador.preparar(e.ctxA, new EditarClienteComando(ana, 1, null, null, null, null,
                LocalDate.of(2026, 10, 14), null))).isNotBlank();
    }

    @Test
    @DisplayName("preparar no cambia el cliente")
    void preparar_edicionValida_noEscribe() {
        e.api.preparar(e.ctxA, editar(ana, 1, "Otro Nombre", null, null, null, null));

        var cliente = e.clientes.porId(e.negocioA, ana).orElseThrow();
        assertThat(cliente.datos().nombre()).isEqualTo("Ana");
        assertThat(cliente.version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("el resumen nombra al cliente pero no muestra su telefono completo")
    void preparar_edicionValida_resumenSinTelefonoCompleto() {
        var preparacion = e.api.preparar(e.ctxA, editar(ana, 1, "Ana Maria", null, null, null, null));

        assertThat(preparacion.resumen()).contains("Ana").doesNotContain("2221234567");
    }

    @Test
    @DisplayName("editar no publica eventos")
    void editar_cliente_noPublicaEventos() {
        var antes = e.eventos.todos().size();

        e.confirmar(e.ctxA, editar(ana, 1, "Ana Maria", null, null, null, null));

        assertThat(e.eventos.todos()).hasSize(antes);
    }

    @Test
    @DisplayName("una edicion sin cambios sigue subiendo la version")
    void editar_sinCambios_igualSubeLaVersion() {
        e.confirmar(e.ctxA, editar(ana, 1, null, null, null, null, null));

        assertThat(e.clientes.porId(e.negocioA, ana).orElseThrow().version()).isEqualTo(2L);
    }
}
