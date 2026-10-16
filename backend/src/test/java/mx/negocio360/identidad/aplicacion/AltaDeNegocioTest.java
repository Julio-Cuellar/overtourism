package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.Map;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.EstadoMembresia;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.NegocioReferencia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

/**
 * D-78 · Identidad no registra negocios: publica {@code negocio.registro_solicitado} para que lo consuma el modulo de
 * negocios, y consume {@code negocio.registrado} para dejar a quien lo pidio como DUENO. Aqui el modulo de negocios es
 * el propio escenario de pruebas. Solo se acepta el aviso de un negocio que Identidad pidio, con el origen
 * {@code negocios} y a nombre de quien lo pidio (revision de seguridad de la ronda del 2026-10-15).
 */
class AltaDeNegocioTest {

    private final Escenario e = new Escenario();
    private Logger registro;
    private ListAppender<ILoggingEvent> avisos;

    @BeforeEach
    void escucharElRegistro() {
        registro = (Logger) LoggerFactory.getLogger(EquipoServicio.class);
        avisos = new ListAppender<>();
        avisos.start();
        registro.addAppender(avisos);
    }

    @AfterEach
    void dejarDeEscuchar() {
        registro.detachAppender(avisos);
    }

    // ---- pedir el negocio ----

    @Test
    @DisplayName("pedir un negocio publica un solo evento negocio.registro_solicitado y no crea nada en Identidad")
    void solicitar_publicaElEventoYNoCreaNada() {
        var token = e.registrar(TEL_A, "Ana").token();

        var solicitado = e.api.solicitarNegocio(token, "  Fonda   Doña Chela ", " restaurante ");

        assertThat(e.eventos.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo("negocio.registro_solicitado");
            assertThat(evento.version()).isEqualTo(1);
            assertThat(evento.origen()).isEqualTo("identidad");
            assertThat(evento.negocioId()).isEqualTo(solicitado.negocioId());
            assertThat(evento.id().version()).isEqualTo(7);
            assertThat(evento.correlacionId()).isNotNull();
            assertThat(evento.causaId()).isNull();
            assertThat(evento.datos()).containsEntry("nombre", "Fonda Doña Chela").containsEntry("giro", "RESTAURANTE")
                    .containsEntry("cuenta_id", e.api.autenticar(token).orElseThrow().cuentaId().toString());
        });
        assertThat(solicitado.negocioId().version()).isEqualTo(7);
        assertThat(e.negocios.porId(solicitado.negocioId())).as("Identidad no registra el negocio").isEmpty();
        assertThat(e.membresias.deNegocio(solicitado.negocioId())).isEmpty();
    }

    @Test
    @DisplayName("el pedido queda anotado para reconocer despues la respuesta del modulo de negocios")
    void solicitar_anotaElPedidoPendiente() {
        var ana = e.registrar(TEL_A, "Ana");

        var solicitado = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE");

        assertThat(e.solicitudes.porNegocio(solicitado.negocioId())).hasValueSatisfying(pendiente -> {
            assertThat(pendiente.cuentaId()).isEqualTo(ana.cuenta().id());
            assertThat(pendiente.nombre()).isEqualTo("Fonda");
            assertThat(pendiente.giro()).isEqualTo(Giro.RESTAURANTE);
            assertThat(pendiente.creadaEn()).isEqualTo(e.reloj.instant());
        });
    }

    @Test
    @DisplayName("el evento no lleva el nombre, el correo ni el celular de la persona (AP-02)")
    void solicitar_elEventoNoLlevaDatosPersonales() {
        var token = e.registrar(TEL_A, "Ana").token();

        e.api.solicitarNegocio(token, "Fonda Doña Chela", "RESTAURANTE");

        var texto = e.eventos.eventos().get(0).toString();
        assertThat(texto).doesNotContain("Ana").doesNotContain("López").doesNotContain(Escenario.correoDe(TEL_A))
                .doesNotContain("2221110001");
    }

    @Test
    @DisplayName("cada solicitud lleva su propio id de negocio y su propio hilo")
    void solicitar_dosVeces_idsDistintos() {
        var token = e.registrar(TEL_A, "Ana").token();

        var primero = e.api.solicitarNegocio(token, "Fonda Uno", "RESTAURANTE");
        var segundo = e.api.solicitarNegocio(token, "Museo Dos", "MUSEO");

        assertThat(primero.negocioId()).isNotEqualTo(segundo.negocioId());
        assertThat(e.eventos.eventos()).extracting(ev -> ev.correlacionId()).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "FARMACIA", "restaurante;drop"})
    @DisplayName("un giro desconocido falla con GIRO_INVALIDO y no publica nada")
    void solicitar_giroInvalido_falla(String giro) {
        var token = e.registrar(TEL_A, "Ana").token();

        fallaCon(() -> e.api.solicitarNegocio(token, "Fonda Doña Chela", giro), CodigoError.GIRO_INVALIDO);

        assertThat(e.eventos.eventos()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "F", "Fonda\u0000", Escenario.NOMBRE_DE_81})
    @DisplayName("un nombre de negocio invalido falla con NOMBRE_INVALIDO y no publica nada")
    void solicitar_nombreInvalido_falla(String nombre) {
        var token = e.registrar(TEL_A, "Ana").token();

        fallaCon(() -> e.api.solicitarNegocio(token, nombre, "RESTAURANTE"), CodigoError.NOMBRE_INVALIDO);

        assertThat(e.eventos.eventos()).isEmpty();
    }

    @Test
    @DisplayName("sin sesion valida no se puede pedir un negocio")
    void solicitar_sinSesion_fallaConNoAutenticado() {
        fallaCon(() -> e.api.solicitarNegocio(null, "Fonda", "RESTAURANTE"), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.solicitarNegocio("token-inventado", "Fonda", "RESTAURANTE"), CodigoError.NO_AUTENTICADO);
        assertThat(e.eventos.eventos()).isEmpty();
    }

    @Test
    @DisplayName("una cuenta no puede inundar al modulo de negocios: tope de 3 pedidos cada 15 minutos, por cuenta")
    void solicitar_tieneTopePorCuenta() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        for (var i = 0; i < 3; i++) {
            e.api.solicitarNegocio(ana.token(), "Fonda " + i, "RESTAURANTE");
        }

        fallaCon(() -> e.api.solicitarNegocio(ana.token(), "Fonda de mas", "RESTAURANTE"), CodigoError.TASA_EXCEDIDA);

        assertThat(e.eventos.eventos()).hasSize(3);
        assertThat(e.api.solicitarNegocio(beto.token(), "Tienda de Beto", "MISCELANEA").negocioId()).isNotNull();
        e.reloj.avanzar(Duration.ofMinutes(16));
        assertThat(e.api.solicitarNegocio(ana.token(), "Fonda despues", "RESTAURANTE").negocioId()).isNotNull();
    }

    // ---- la respuesta del modulo de negocios ----

    @Test
    @DisplayName("al registrarse el negocio, quien lo pidio queda como DUENO y se publica membresia_agregada")
    void registrado_creaElNegocioDeReferenciaYElDueno() {
        var sesion = e.registrar(TEL_A, "Ana");
        var cuentaId = sesion.cuenta().id();
        var negocioId = e.api.solicitarNegocio(sesion.token(), "Fonda Doña Chela", "RESTAURANTE").negocioId();
        var recibido = e.negocioRegistrado(negocioId, cuentaId, "Fonda Doña Chela", "RESTAURANTE");

        e.escucha.alRecibir(recibido);

        assertThat(e.negocios.porId(negocioId)).hasValueSatisfying(n -> {
            assertThat(n.nombre()).isEqualTo("Fonda Doña Chela");
            assertThat(n.giro()).isEqualTo(Giro.RESTAURANTE);
            assertThat(n.estado()).isEqualTo(NegocioReferencia.Estado.ACTIVO);
        });
        var membresia = e.membresias.porCuentaYNegocio(cuentaId, negocioId).orElseThrow();
        assertThat(membresia.rolCodigo()).isEqualTo("DUENO");
        assertThat(membresia.estado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(membresia.agregadaPor()).as("el dueño inicial no lo agrego nadie").isNull();
        var agregado = e.eventos.eventos().stream().filter(ev -> ev.tipo().equals("identidad.membresia_agregada")).toList();
        assertThat(agregado).singleElement().satisfies(ev -> {
            assertThat(ev.negocioId()).isEqualTo(negocioId);
            assertThat(ev.origen()).isEqualTo("identidad");
            assertThat(ev.causaId()).as("lo provoco el evento recibido").isEqualTo(recibido.id());
            assertThat(ev.correlacionId()).as("conserva el hilo de la solicitud").isEqualTo(recibido.correlacionId());
            assertThat(ev.datos()).containsEntry("cuenta_id", cuentaId.toString()).containsEntry("rol", "DUENO")
                    .containsEntry("origen", "DUENO_INICIAL");
        });
    }

    @Test
    @DisplayName("al aplicar el aviso se consume el pedido: no se puede reutilizar")
    void registrado_consumeElPedido() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE").negocioId();

        e.escucha.alRecibir(e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda", "RESTAURANTE"));

        assertThat(e.solicitudes.porNegocio(negocioId)).isEmpty();
    }

    @Test
    @DisplayName("R3: recibir dos veces el mismo evento deja una sola membresia y un solo evento")
    void registrado_entregaRepetida_esIdempotente() {
        var sesion = e.registrar(TEL_A, "Ana");
        var negocioId = e.api.solicitarNegocio(sesion.token(), "Fonda", "RESTAURANTE").negocioId();
        var recibido = e.negocioRegistrado(negocioId, sesion.cuenta().id(), "Fonda", "RESTAURANTE");

        e.escucha.alRecibir(recibido);
        e.escucha.alRecibir(recibido);

        assertThat(e.membresias.deNegocio(negocioId)).hasSize(1);
        assertThat(e.eventos.eventos().stream().filter(ev -> ev.tipo().equals("identidad.membresia_agregada"))).hasSize(1);
    }

    @Test
    @DisplayName("seguridad: un aviso de un negocio que Identidad no pidio no hace dueña a nadie")
    void registrado_sinPedidoPendiente_seIgnora() {
        var ana = e.registrar(TEL_A, "Ana");
        var inventado = UuidV7.generar(e.reloj.instant());

        e.escucha.alRecibir(e.negocioRegistrado(inventado, ana.cuenta().id(), "Negocio inventado", "RESTAURANTE"));

        assertThat(e.negocios.porId(inventado)).isEmpty();
        assertThat(e.membresias.deCuenta(ana.cuenta().id())).isEmpty();
        assertThat(e.eventos.eventos()).isEmpty();
    }

    @Test
    @DisplayName("seguridad: el aviso a nombre de otra cuenta que la que pidio el negocio se ignora, y el pedido sigue vigente")
    void registrado_aNombreDeOtraCuenta_seIgnora() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE").negocioId();

        e.escucha.alRecibir(e.negocioRegistrado(negocioId, beto.cuenta().id(), "Fonda", "RESTAURANTE"));

        assertThat(e.membresias.deNegocio(negocioId)).isEmpty();
        assertThat(e.solicitudes.porNegocio(negocioId)).isPresent();
        e.escucha.alRecibir(e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda", "RESTAURANTE"));
        assertThat(e.membresias.porCuentaYNegocio(ana.cuenta().id(), negocioId)).isPresent();
    }

    @Test
    @DisplayName("seguridad: solo vale el aviso que publica el modulo de negocios; el de otro origen se ignora")
    void registrado_deOtroOrigen_seIgnora() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE").negocioId();
        var ahora = e.reloj.instant();
        var falso = new SobreEvento(UuidV7.generar(ahora), "negocio.registrado", 1, negocioId, "clientes", ahora,
                UuidV7.generar(ahora), null, Map.of("solicitado_por", ana.cuenta().id().toString(), "nombre", "Fonda",
                        "giro", "RESTAURANTE"));

        e.escucha.alRecibir(falso);

        assertThat(e.membresias.deNegocio(negocioId)).isEmpty();
        assertThat(e.negocios.porId(negocioId)).isEmpty();
    }

    @Test
    @DisplayName("un negocio se registra una sola vez: un segundo aviso para otra cuenta no la hace dueña")
    void registrado_negocioYaExistente_noAgregaAOtraCuenta() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE").negocioId();
        e.escucha.alRecibir(e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda", "RESTAURANTE"));

        e.escucha.alRecibir(e.negocioRegistrado(negocioId, beto.cuenta().id(), "Fonda", "RESTAURANTE"));

        assertThat(e.membresias.porCuentaYNegocio(beto.cuenta().id(), negocioId)).isEmpty();
        assertThat(e.membresias.deNegocio(negocioId)).hasSize(1);
    }

    @Test
    @DisplayName("si la cuenta ya no esta activa, el aviso se ignora y deja un aviso en el registro sin datos personales")
    void registrado_cuentaSuspendida_seIgnoraYAvisa() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda Secreta", "RESTAURANTE").negocioId();
        e.cuentas.guardar(e.cuentas.porId(ana.cuenta().id()).orElseThrow().conEstado(EstadoCuenta.SUSPENDIDA));
        var recibido = e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda Secreta", "RESTAURANTE");

        e.escucha.alRecibir(recibido);

        assertThat(e.negocios.porId(negocioId)).isEmpty();
        assertThat(e.eventos.eventos().stream().filter(ev -> ev.tipo().equals("identidad.membresia_agregada"))).isEmpty();
        assertThat(avisos.list).filteredOn(ev -> ev.getLevel() == Level.WARN).singleElement().satisfies(ev -> {
            assertThat(ev.getFormattedMessage()).contains(negocioId.toString()).contains(recibido.id().toString());
            assertThat(ev.getFormattedMessage()).doesNotContain("Fonda Secreta").doesNotContain("Ana")
                    .doesNotContain(Escenario.correoDe(TEL_A));
        });
    }

    @Test
    @DisplayName("un aviso sin pedido pendiente tambien deja un aviso en el registro, para poder reconciliar")
    void registrado_sinPedido_avisaEnElRegistro() {
        var ana = e.registrar(TEL_A, "Ana");
        var inventado = UuidV7.generar(e.reloj.instant());

        e.escucha.alRecibir(e.negocioRegistrado(inventado, ana.cuenta().id(), "Negocio inventado", "RESTAURANTE"));

        assertThat(avisos.list).filteredOn(ev -> ev.getLevel() == Level.WARN).singleElement()
                .satisfies(ev -> assertThat(ev.getFormattedMessage()).contains(inventado.toString())
                        .doesNotContain("Negocio inventado"));
    }

    @Test
    @DisplayName("un giro o un nombre que Identidad no reconoce se ignora sin dejar un negocio a medias")
    void registrado_datosInvalidos_seIgnora() {
        var ana = e.registrar(TEL_A, "Ana");
        var giroRaro = e.api.solicitarNegocio(ana.token(), "Fonda", "MUSEO").negocioId();
        var sinNombre = e.api.solicitarNegocio(ana.token(), "Museo", "MUSEO").negocioId();

        e.escucha.alRecibir(e.negocioRegistrado(giroRaro, ana.cuenta().id(), "Fonda", "FARMACIA"));
        e.escucha.alRecibir(e.negocioRegistrado(sinNombre, ana.cuenta().id(), "  ", "MUSEO"));

        assertThat(e.negocios.porId(giroRaro)).isEmpty();
        assertThat(e.negocios.porId(sinNombre)).isEmpty();
        assertThat(e.membresias.deCuenta(ana.cuenta().id())).isEmpty();
    }

    @Test
    @DisplayName("una persona puede quedar como dueña de varios negocios (D-68)")
    void registrado_variosNegocios_unaCuentaDeDueña() {
        var ana = e.registrar(TEL_A, "Ana");

        var uno = e.crearNegocio(ana.token(), "Fonda Uno", "RESTAURANTE");
        e.reloj.avanzar(Duration.ofSeconds(1));
        var dos = e.crearNegocio(ana.token(), "Museo Dos", "MUSEO");

        assertThat(e.membresias.deCuenta(ana.cuenta().id())).extracting(m -> m.negocioId())
                .containsExactlyInAnyOrder(uno, dos);
    }
}
