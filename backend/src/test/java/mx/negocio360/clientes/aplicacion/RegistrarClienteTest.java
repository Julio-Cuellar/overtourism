package mx.negocio360.clientes.aplicacion;

import static mx.negocio360.clientes.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.EstadoCliente;
import mx.negocio360.clientes.dominio.FormaConsentimiento;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.clientes.fakes.Escenario;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** HTC-26 · I-01 a I-03, I-07, I-08, P-01, P-04, P-06, L-01, L-03: RegistrarCliente. */
class RegistrarClienteTest {

    private static final List<ConsentimientoSolicitado> DOS_CONSENTIMIENTOS = List.of(
            new ConsentimientoSolicitado(Finalidad.CONTACTO_MENSAJERIA, MedioConsentimiento.VERBAL),
            new ConsentimientoSolicitado(Finalidad.PROMOCIONES, MedioConsentimiento.PANEL));

    private final Escenario e = new Escenario();

    @BeforeEach
    void publicarAviso() {
        e.conAviso(e.ctxA);
    }

    private static RegistrarClienteComando cmd(String nombre, String telefono, String correo, LocalDate nacimiento,
                                               String notas, Integer aviso, List<ConsentimientoSolicitado> cons) {
        return new RegistrarClienteComando(nombre, null, telefono, correo, nacimiento, notas, aviso, cons);
    }

    static Stream<Arguments> comandosInvalidos() {
        var vacio = List.<ConsentimientoSolicitado>of();
        return Stream.of(
                Arguments.of("nombre vacio", cmd("  ", null, null, null, null, 1, vacio), CodigoError.NOMBRE_INVALIDO),
                Arguments.of("nombre de 81", cmd("a".repeat(81), null, null, null, null, 1, vacio),
                        CodigoError.NOMBRE_INVALIDO),
                Arguments.of("telefono corto", cmd("Ana", "123", null, null, null, 1, vacio),
                        CodigoError.TELEFONO_INVALIDO),
                Arguments.of("correo sin dominio", cmd("Ana", null, "ana@", null, null, 1, vacio),
                        CodigoError.CORREO_INVALIDO),
                Arguments.of("nacimiento manana", cmd("Ana", null, null, Fixtures.HOY.plusDays(1), null, 1, vacio),
                        CodigoError.FECHA_NACIMIENTO_INVALIDA),
                Arguments.of("notas de 501", cmd("Ana", null, null, null, "n".repeat(501), 1, vacio),
                        CodigoError.NOTAS_MUY_LARGAS),
                Arguments.of("sin version de aviso", cmd("Ana", null, null, null, null, null, vacio),
                        CodigoError.AVISO_VERSION_REQUERIDA),
                Arguments.of("version de aviso cero", cmd("Ana", null, null, null, null, 0, vacio),
                        CodigoError.AVISO_VERSION_REQUERIDA),
                Arguments.of("version de aviso que no es la vigente", cmd("Ana", null, null, null, null, 7, vacio),
                        CodigoError.AVISO_NO_VIGENTE),
                Arguments.of("consentimiento sin medio", cmd("Ana", null, null, null, null, 1,
                        List.of(new ConsentimientoSolicitado(Finalidad.PROMOCIONES, null))),
                        CodigoError.CONSENTIMIENTO_INCOMPLETO),
                Arguments.of("consentimiento sin finalidad", cmd("Ana", null, null, null, null, 1,
                        List.of(new ConsentimientoSolicitado(null, MedioConsentimiento.VERBAL))),
                        CodigoError.FINALIDAD_INVALIDA),
                Arguments.of("finalidad repetida", cmd("Ana", null, null, null, null, 1, List.of(
                        new ConsentimientoSolicitado(Finalidad.PROMOCIONES, MedioConsentimiento.VERBAL),
                        new ConsentimientoSolicitado(Finalidad.PROMOCIONES, MedioConsentimiento.PANEL))),
                        CodigoError.FINALIDAD_INVALIDA));
    }

    @Test
    @DisplayName("guarda el telefono en E.164, el correo en minusculas y la version del aviso entregado")
    void registrar_datosLibres_guardaNormalizadosYConAviso() {
        var comando = new RegistrarClienteComando("  Ana ", "Pérez", "222 123 4567", "ANA@Correo.test",
                LocalDate.of(1990, 5, 1), " cliente frecuente ", 1, List.of());

        var resultado = e.confirmar(e.ctxA, comando);

        var cliente = e.clientes.porId(e.negocioA, resultado.recursoId()).orElseThrow();
        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(resultado.mensajePlantilla()).isNotBlank();
        assertThat(cliente.datos().nombre()).isEqualTo("Ana");
        assertThat(cliente.datos().telefono()).isEqualTo("+522221234567");
        assertThat(cliente.datos().correo()).isEqualTo("ana@correo.test");
        assertThat(cliente.datos().fechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 1));
        assertThat(cliente.datos().notas()).isEqualTo("cliente frecuente");
        assertThat(cliente.avisoVersion()).isEqualTo(1);
        assertThat(cliente.avisoEntregadoEn()).isEqualTo(Fixtures.AHORA);
        assertThat(cliente.creadoPor()).isEqualTo("locatario-a");
        assertThat(cliente.estado()).isEqualTo(EstadoCliente.ACTIVO);
        assertThat(cliente.origen()).isEqualTo(OrigenCliente.PANEL);
        assertThat(cliente.version()).isEqualTo(1L);
        assertThat(cliente.anonimizadoEn()).isNull();
        assertThat(cliente.negocioId()).isEqualTo(e.negocioA);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("comandosInvalidos")
    @DisplayName("un comando invalido falla al preparar con su codigo y no escribe nada")
    void preparar_comandoInvalido_fallaConSuCodigo(String caso, RegistrarClienteComando comando,
                                                   CodigoError esperado) {
        fallaCon(() -> e.api.preparar(e.ctxA, comando), esperado);

        assertThat(e.clientes.total()).isZero();
        assertThat(e.preparaciones.todas()).hasSize(1);
    }

    @Test
    @DisplayName("el segundo alta con el mismo telefono falla con TELEFONO_DUPLICADO y devuelve el id existente")
    void preparar_telefonoDuplicado_fallaConElIdDelExistente() {
        var primero = e.registrar(e.ctxA, Fixtures.registrar("Ana", null, "222 123 4567"));

        var error = fallaCon(() -> e.api.preparar(e.ctxA, Fixtures.registrar("Beto", null, "+52 222 123 4567")),
                CodigoError.TELEFONO_DUPLICADO);

        assertThat(error.recursoId()).contains(primero);
        assertThat(error.getMessage()).doesNotContain("2221234567").doesNotContain("Ana").doesNotContain("Beto");
    }

    @Test
    @DisplayName("si el telefono se ocupa entre preparar y confirmar, confirmar tambien falla con TELEFONO_DUPLICADO")
    void confirmar_telefonoOcupadoEntrePrepararYConfirmar_fallaConTelefonoDuplicado() {
        var uno = e.api.preparar(e.ctxA, Fixtures.registrar("Ana", null, "222 123 4567"));
        var dos = e.api.preparar(e.ctxA, Fixtures.registrar("Beto", null, "222 123 4567"));
        var primero = e.api.confirmar(e.ctxA, uno.token()).recursoId();

        var error = fallaCon(() -> e.api.confirmar(e.ctxA, dos.token()), CodigoError.TELEFONO_DUPLICADO);

        assertThat(error.recursoId()).contains(primero);
        assertThat(e.clientes.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("el mismo telefono en otro negocio es otro cliente")
    void registrar_mismoTelefonoEnOtroNegocio_sePermite() {
        e.conAviso(e.ctxB);
        var enA = e.registrar(e.ctxA, Fixtures.registrar("Ana", null, "222 123 4567"));

        var enB = e.registrar(e.ctxB, Fixtures.registrar("Ana", null, "222 123 4567"));

        assertThat(enB).isNotEqualTo(enA);
        assertThat(e.clientes.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("el telefono es opcional: dos clientes sin telefono conviven")
    void registrar_sinTelefono_sePermiteVariasVeces() {
        e.registrar(e.ctxA, Fixtures.registrar("Ana"));
        e.registrar(e.ctxA, Fixtures.registrar("Beto"));

        assertThat(e.clientes.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("preparar solo valida: no guarda cliente, consentimientos ni eventos")
    void preparar_comandoValido_noEscribe() {
        e.api.preparar(e.ctxA, new RegistrarClienteComando("Ana", null, "222 123 4567", null, null, null, 1,
                DOS_CONSENTIMIENTOS));

        assertThat(e.clientes.total()).isZero();
        assertThat(e.consentimientos.total()).isZero();
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("el resumen muestra el nombre y el telefono enmascarado, nunca el numero completo")
    void preparar_comandoValido_resumenConTelefonoEnmascarado() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana", "Pérez", "222 123 4567"));

        assertThat(preparacion.resumen()).contains("Ana Pérez").contains("•••• 4567")
                .doesNotContain("2221234567").doesNotContain("+52");
    }

    @Test
    @DisplayName("publica cliente.registrado y un consentimiento_cambiado por cada consentimiento")
    void confirmar_conConsentimientos_publicaUnEventoPorCadaUno() {
        var comando = new RegistrarClienteComando("Ana", null, "222 123 4567", null, null, null, 1,
                DOS_CONSENTIMIENTOS);

        var id = e.registrar(e.ctxA, comando);

        assertThat(e.eventos.todos()).extracting("tipo")
                .containsExactly("cliente.registrado", "cliente.consentimiento_cambiado",
                        "cliente.consentimiento_cambiado");
        var registrado = e.eventos.deTipo("cliente.registrado").get(0);
        assertThat(registrado.version()).isEqualTo(1);
        assertThat(registrado.negocioId()).isEqualTo(e.negocioA);
        assertThat(registrado.origen()).isEqualTo("clientes");
        assertThat(registrado.ocurridoEn()).isEqualTo(Fixtures.AHORA);
        assertThat(registrado.correlacionId()).isEqualTo(id);
        assertThat(registrado.causaId()).isNull();
        assertThat(registrado.id().version()).isEqualTo(7);
        assertThat(registrado.datos()).isEqualTo(Map.of("cliente_id", id.toString(), "origen", "PANEL"));
        assertThat(e.eventos.deTipo("cliente.consentimiento_cambiado").get(1).datos()).isEqualTo(Map.of(
                "cliente_id", id.toString(), "finalidad", "PROMOCIONES", "estado", "OTORGADO", "medio", "PANEL"));
    }

    @Test
    @DisplayName("guarda cada consentimiento como otorgado y expreso, con medio, aviso y actor (P-04)")
    void confirmar_conConsentimientos_losGuardaConEvidencia() {
        var id = e.registrar(e.ctxA, new RegistrarClienteComando("Ana", null, null, null, null, null, 1,
                DOS_CONSENTIMIENTOS));

        var filas = e.consentimientos.porCliente(e.negocioA, id);

        assertThat(filas).hasSize(2);
        var primero = filas.get(0);
        assertThat(primero.finalidad()).isEqualTo(Finalidad.CONTACTO_MENSAJERIA);
        assertThat(primero.estado()).isEqualTo(EstadoConsentimiento.OTORGADO);
        assertThat(primero.forma()).isEqualTo(FormaConsentimiento.EXPRESO);
        assertThat(primero.medio()).isEqualTo(MedioConsentimiento.VERBAL);
        assertThat(primero.avisoVersion()).isEqualTo(1);
        assertThat(primero.registradoPor()).isEqualTo("locatario-a");
        assertThat(primero.registradoEn()).isEqualTo(Fixtures.AHORA);
        assertThat(primero.negocioId()).isEqualTo(e.negocioA);
        assertThat(primero.clienteId()).isEqualTo(id);
    }

    @Test
    @DisplayName("sin consentimientos solo se publica cliente.registrado")
    void confirmar_sinConsentimientos_soloPublicaRegistrado() {
        e.registrar(e.ctxA, Fixtures.registrar("Ana"));

        assertThat(e.eventos.todos()).extracting("tipo").containsExactly("cliente.registrado");
        assertThat(e.consentimientos.total()).isZero();
    }

    @Test
    @DisplayName("la lista de consentimientos puede venir nula")
    void confirmar_consentimientosNulos_seTratanComoVacios() {
        var id = e.registrar(e.ctxA, new RegistrarClienteComando("Ana", null, null, null, null, null, 1, null));

        assertThat(e.clientes.porId(e.negocioA, id)).isPresent();
    }

    @Test
    @DisplayName("ningun evento lleva nombre, telefono, correo ni fecha de nacimiento (P-06)")
    void confirmar_conDatosPersonales_losEventosNoLosLlevan() {
        e.registrar(e.ctxA, new RegistrarClienteComando("Ana", "Pérez", "222 123 4567", "ana@correo.test",
                LocalDate.of(1990, 5, 1), "nota privada", 1, DOS_CONSENTIMIENTOS));

        var valores = e.eventos.todos().stream().flatMap(ev -> ev.datos().values().stream()).map(Object::toString)
                .collect(java.util.stream.Collectors.joining("|"));

        assertThat(valores).doesNotContain("Ana").doesNotContain("Pérez").doesNotContain("2221234567")
                .doesNotContain("correo.test").doesNotContain("1990-05-01").doesNotContain("nota privada");
    }

    @Test
    @DisplayName("la fecha de nacimiento 'de hoy' es la de Mexico aunque el reloj este en UTC (I-08)")
    void validar_relojEnUtc_usaLaFechaDeMexico() {
        var reloj = java.time.Clock.fixed(java.time.Instant.parse("2026-10-15T01:00:00Z"), java.time.ZoneOffset.UTC);
        var manejador = new ManejadorRegistrarCliente(e.clientes, e.avisos, e.consentimientos, e.eventos, reloj);

        fallaCon(() -> manejador.preparar(e.ctxA, cmd("Ana", null, null, LocalDate.of(2026, 10, 15), null, 1, null)),
                CodigoError.FECHA_NACIMIENTO_INVALIDA);
        assertThat(manejador.preparar(e.ctxA, cmd("Ana", null, null, LocalDate.of(2026, 10, 14), null, 1, null)))
                .isNotBlank();
    }

    @Test
    @DisplayName("el cliente de un negocio no aparece en otro")
    void registrar_enUnNegocio_otroNoLoVe() {
        var id = e.registrar(e.ctxA, Fixtures.registrar("Ana"));

        assertThat(e.clientes.porId(e.negocioB, id)).isEmpty();
    }
}
