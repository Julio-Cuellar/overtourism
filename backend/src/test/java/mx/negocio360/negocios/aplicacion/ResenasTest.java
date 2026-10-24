package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.AdministracionApi;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.FiltroResenas;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.NegociosPublicosApi;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.infraestructura.EjecutorEnContexto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * N6: reseñas en modo demostracion (RS-01 a RS-11) contra Postgres: preparar y confirmar con el id de dispositivo, el
 * resumen, la respuesta del negocio, la moderacion de la plataforma y la vista publica sin autor.
 */
@SpringBootTest(properties = "negocios.demo.activo=true")
@RecordApplicationEvents
@Import(RelojDeResenas.class)
class ResenasTest {

    @Autowired
    private NegociosApi panel;
    @Autowired
    private NegociosPublicosApi publico;
    @Autowired
    private ResenasPublicasApi resenas;
    @Autowired
    private AdministracionApi admin;
    @Autowired
    private AltasApi altas;
    @Autowired
    private EjecutorEnContexto ejecutor;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private ApplicationEvents eventos;

    @BeforeEach
    void relojAlInicio() {
        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO);
    }

    private static String dispositivo() {
        return UUID.randomUUID().toString();
    }

    private static CodigoError codigo(Throwable error) {
        assertThat(error).isInstanceOf(NegociosException.class);
        return ((NegociosException) error).codigo();
    }

    /** Un negocio publico: con ubicacion, horario y publicado. */
    private Escenario publicado() {
        var e = Escenario.nuevo(altas, "Fonda Zr" + UUID.randomUUID().toString().substring(0, 8), "RESTAURANTE");
        var sucursal = panel.sucursales(e.contexto, e.dueno).get(0);
        panel.actualizarSucursal(e.contexto, e.dueno, sucursal.id(), new ActualizarSucursal(0, null, null, null, null,
                null, null, null, null, new BigDecimal("19.04"), new BigDecimal("-98.19"), null));
        panel.definirHorario(e.contexto, e.dueno, sucursal.id(), 0,
                List.of(new DiaHorarioComando(4, List.of(new TramoComando("08:00", "17:00")))));
        panel.publicarNegocio(e.contexto, e.dueno, true);
        return e;
    }

    private UUID reseñar(Escenario e, String dispositivo, int calificacion, String comentario) {
        var preparacion = resenas.prepararResena(dispositivo, e.negocioId, calificacion, comentario);
        return resenas.confirmarResena(dispositivo, preparacion.token()).resenaId();
    }

    private List<SobreEvento> publicados(String tipo, UUID negocio) {
        return eventos.stream(SobreEvento.class).filter(x -> x.tipo().equals(tipo) && x.negocioId().equals(negocio))
                .toList();
    }

    // ---- publicar ----

    @Test
    @DisplayName("RS-09 y RS-11: preparar y confirmar con el id de dispositivo publica la reseña, sin cuenta ni sello")
    void publicar_porPrepararYConfirmar() {
        var e = publicado();
        var dispositivo = dispositivo();

        var preparacion = resenas.prepararResena(dispositivo, e.negocioId, 5, "El mole estaba buenisimo");
        var resultado = resenas.confirmarResena(dispositivo, preparacion.token());

        assertThat(preparacion.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(preparacion.resumen()).contains("5").doesNotContain("buenisimo");
        assertThat(preparacion.expiraEn()).isEqualTo(RelojDeResenas.INICIO.plus(Duration.ofMinutes(5)));
        assertThat(resultado.resenaId()).isNotNull();
        var pagina = panel.resenas(e.contexto, e.dueno, FiltroResenas.TODAS, 20, null);
        assertThat(pagina.resenas()).singleElement().satisfies(r -> {
            assertThat(r.id()).isEqualTo(resultado.resenaId());
            assertThat(r.calificacion()).isEqualTo(5);
            assertThat(r.comentario()).isEqualTo("El mole estaba buenisimo");
            assertThat(r.estado()).isEqualTo("PUBLICADA");
            assertThat(r.demostracion()).isTrue();
            assertThat(r.respuesta()).isNull();
        });
    }

    @Test
    @DisplayName("RS-06: el resumen se actualiza en la misma operacion: 5 y 3 dan total 2 y promedio 4,0, y sale en la ficha publica")
    void resumen_seActualiza() {
        var e = publicado();

        reseñar(e, dispositivo(), 5, "Excelente");
        reseñar(e, dispositivo(), 3, null);

        var resumen = panel.resenas(e.contexto, e.dueno, FiltroResenas.TODAS, 20, null).resumen();
        assertThat(resumen.total()).isEqualTo(2);
        assertThat(resumen.promedio()).isEqualTo(4.0);
        assertThat(resumen.distribucion()).containsExactly(0, 0, 1, 0, 1);
        var ficha = publico.negocioPublico(e.negocioId).orElseThrow();
        assertThat(ficha.calificacion().total()).isEqualTo(2);
        assertThat(ficha.calificacion().promedio()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("B13: publicar emite negocio.resena_publicada con el dispositivo como autor y sin el comentario")
    void publicar_emiteElEvento() {
        var e = publicado();
        var dispositivo = dispositivo();

        var id = reseñar(e, dispositivo, 4, "Llamenme al 2225550101");

        var sobre = publicados("negocio.resena_publicada", e.negocioId).get(0);
        assertThat(sobre.origen()).isEqualTo("negocios");
        assertThat(sobre.datos()).containsEntry("resena_id", id.toString()).containsEntry("visitante_id", dispositivo)
                .containsEntry("accion", "PUBLICADA").containsEntry("calificacion", 4)
                .containsEntry("tiene_comentario", true);
        assertThat(sobre.datos().toString()).doesNotContain("2225550101");
    }

    @Test
    @DisplayName("SN-09: tras confirmar, la preparacion ya no conserva el comentario")
    void confirmar_vaciaElComentario() {
        var e = publicado();
        var dispositivo = dispositivo();
        var preparacion = resenas.prepararResena(dispositivo, e.negocioId, 5, "Texto de un tercero");
        resenas.confirmarResena(dispositivo, preparacion.token());

        var conservados = jdbc.sql("SELECT count(*) FROM negocios.preparacion_resena WHERE visitante_id = :d "
                + "AND comentario IS NOT NULL").param("d", UUID.fromString(dispositivo)).query(Integer.class).single();

        assertThat(conservados).isZero();
    }

    // ---- tokens ----

    @Test
    @DisplayName("D-23: un token se usa una vez; otro, vencido o ajeno, no sirve")
    void token_reglas() {
        var e = publicado();
        var dispositivo = dispositivo();
        var preparacion = resenas.prepararResena(dispositivo, e.negocioId, 5, null);
        var otro = resenas.prepararResena(dispositivo(), e.negocioId, 5, null);

        var ajeno = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(dispositivo(),
                preparacion.token()));
        var inventado = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(dispositivo,
                "no-existe"));
        resenas.confirmarResena(dispositivo, preparacion.token());
        var repetido = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(dispositivo,
                preparacion.token()));
        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plus(Duration.ofMinutes(5)).plusSeconds(1));
        var vencido = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(
                otro.token().isEmpty() ? dispositivo : dispositivo(), otro.token()));

        assertThat(codigo(ajeno)).isEqualTo(CodigoError.TOKEN_INVALIDO);
        assertThat(codigo(inventado)).isEqualTo(CodigoError.TOKEN_INVALIDO);
        assertThat(codigo(repetido)).isEqualTo(CodigoError.TOKEN_USADO);
        assertThat(codigo(vencido)).as("otro dispositivo con un token vencido: no revela que existe")
                .isEqualTo(CodigoError.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("el token vence a los 5 minutos")
    void token_vence() {
        var e = publicado();
        var dispositivo = dispositivo();
        var preparacion = resenas.prepararResena(dispositivo, e.negocioId, 5, null);

        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plus(Duration.ofMinutes(5)).plusSeconds(1));
        var error = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(dispositivo,
                preparacion.token()));

        assertThat(codigo(error)).isEqualTo(CodigoError.TOKEN_EXPIRADO);
        assertThat(panel.resenas(e.contexto, e.dueno, FiltroResenas.TODAS, 20, null).resenas()).isEmpty();
    }

    // ---- reglas de la reseña ----

    @Test
    @DisplayName("RS-02: una sola reseña por dispositivo y negocio; en otro negocio si puede")
    void unaPorDispositivoYNegocio() {
        var uno = publicado();
        var dos = publicado();
        var dispositivo = dispositivo();
        reseñar(uno, dispositivo, 5, null);

        var repetida = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(dispositivo,
                uno.negocioId, 1, null));
        var enOtro = reseñar(dos, dispositivo, 4, null);

        assertThat(codigo(repetida)).isEqualTo(CodigoError.RESENA_YA_EXISTE);
        assertThat(enOtro).isNotNull();
    }

    @Test
    @DisplayName("RS-02: dos confirmaciones en carrera del mismo dispositivo dejan una sola reseña")
    void confirmacionDoble_dejaUna() {
        var e = publicado();
        var dispositivo = dispositivo();
        var a = resenas.prepararResena(dispositivo, e.negocioId, 5, null);
        var b = resenas.prepararResena(dispositivo, e.negocioId, 1, null);

        resenas.confirmarResena(dispositivo, a.token());
        var segunda = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(dispositivo,
                b.token()));

        assertThat(codigo(segunda)).isEqualTo(CodigoError.RESENA_YA_EXISTE);
        assertThat(panel.resenas(e.contexto, e.dueno, FiltroResenas.TODAS, 20, null).resenas()).hasSize(1);
    }

    @Test
    @DisplayName("RS-03: calificacion fuera de 1 a 5 y comentario invalido fallan")
    void calificacionYComentario() {
        var e = publicado();

        var cero = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(dispositivo(),
                e.negocioId, 0, null));
        var seis = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(dispositivo(),
                e.negocioId, 6, null));
        var largo = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(dispositivo(),
                e.negocioId, 5, "x".repeat(1001)));

        assertThat(codigo(cero)).isEqualTo(CodigoError.CALIFICACION_INVALIDA);
        assertThat(codigo(seis)).isEqualTo(CodigoError.CALIFICACION_INVALIDA);
        assertThat(codigo(largo)).isEqualTo(CodigoError.COMENTARIO_INVALIDO);
    }

    @Test
    @DisplayName("RS-10: un negocio sin publicar, suspendido o cerrado, o que no existe, no admite reseñas y no dice por que")
    void negocioQueNoEsPublico() {
        var sinPublicar = Escenario.nuevo(altas);
        var suspendido = publicado();
        var cerrado = publicado();
        ejecutor.enNegocio(suspendido.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'SUSPENDIDO', "
                + "suspendido_por = 'DUENO' WHERE id = :id").param("id", suspendido.negocioId).update());
        ejecutor.enNegocio(cerrado.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'CERRADO', "
                + "cerrado_en = now() WHERE id = :id").param("id", cerrado.negocioId).update());

        for (var id : List.of(sinPublicar.negocioId, suspendido.negocioId, cerrado.negocioId, UUID.randomUUID())) {
            var error = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(dispositivo(), id,
                    5, null));
            assertThat(codigo(error)).isEqualTo(CodigoError.NEGOCIO_NO_DISPONIBLE);
        }
    }

    // ---- RS-11 y DM-03 ----

    @Test
    @DisplayName("RS-11: un id de dispositivo que no es UUID se rechaza, al preparar y al confirmar")
    void dispositivoInvalido() {
        var e = publicado();

        var preparar = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena("no-es-uuid",
                e.negocioId, 5, null));
        var confirmar = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.confirmarResena(null, "x"));

        assertThat(codigo(preparar)).isEqualTo(CodigoError.DISPOSITIVO_INVALIDO);
        assertThat(codigo(confirmar)).isEqualTo(CodigoError.DISPOSITIVO_INVALIDO);
    }

    @Test
    @DisplayName("RS-11: un dispositivo puede preparar 5 reseñas por hora; la sexta se corta y pasada la hora vuelve a poder")
    void limitePorHora() {
        var uno = publicado();
        var otros = List.of(publicado(), publicado(), publicado(), publicado(), publicado());
        var dispositivo = dispositivo();
        resenas.prepararResena(dispositivo, uno.negocioId, 5, null);
        for (var i = 0; i < 4; i++) {
            resenas.prepararResena(dispositivo, otros.get(i).negocioId, 5, null);
        }

        var sexta = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(dispositivo,
                otros.get(4).negocioId, 5, null));
        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plus(Duration.ofMinutes(61)));
        var despues = resenas.prepararResena(dispositivo, otros.get(4).negocioId, 5, null);

        assertThat(codigo(sexta)).isEqualTo(CodigoError.DEMASIADAS_SOLICITUDES);
        assertThat(despues.token()).isNotBlank();
        // Otro dispositivo no se ve afectado.
        assertThat(resenas.prepararResena(dispositivo(), uno.negocioId, 5, null).token()).isNotBlank();
    }

    // ---- lectura publica (T3) ----

    @Test
    @DisplayName("T3 y RS-08: las reseñas publicas salen sin autor, con la respuesta y la marca de demostracion, de la mas nueva")
    void resenasPublicas_sinAutor() {
        var e = publicado();
        var primera = reseñar(e, dispositivo(), 5, "Primera");
        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plusSeconds(60));
        var segunda = reseñar(e, dispositivo(), 2, "Segunda");
        panel.responderResena(e.contexto, e.dueno, primera, "Gracias por venir");

        var pagina = resenas.resenasDeNegocio(e.negocioId, 20, null);

        assertThat(pagina.resenas()).extracting("id").containsExactly(segunda, primera);
        assertThat(pagina.resenas().get(1).respuesta()).isEqualTo("Gracias por venir");
        assertThat(pagina.resenas().get(0).demostracion()).isTrue();
        assertThat(pagina.resenas().toString()).doesNotContain("visitante");
        assertThat(pagina.hayMas()).isFalse();
    }

    @Test
    @DisplayName("PU-05: las reseñas publicas van de 20 en 20 con cursor; un negocio que no es publico responde vacio")
    void resenasPublicas_paginaYTopes() {
        var e = publicado();
        for (var i = 0; i < 23; i++) {
            RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plusSeconds(i));
            reseñar(e, dispositivo(), 1 + i % 5, null);
        }

        var primera = resenas.resenasDeNegocio(e.negocioId, 500, null);
        var segunda = resenas.resenasDeNegocio(e.negocioId, 500, primera.cursorFinal());
        var inexistente = resenas.resenasDeNegocio(UUID.randomUUID(), 20, null);
        var sinPublicar = resenas.resenasDeNegocio(Escenario.nuevo(altas).negocioId, 20, null);

        assertThat(primera.resenas()).hasSize(20);
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.resenas()).hasSize(3);
        assertThat(inexistente.resenas()).isEmpty();
        assertThat(sinPublicar.resenas()).isEmpty();
    }

    @Test
    @DisplayName("SN-14 y RS-08: la vista publica de reseñas no tiene al autor y el rol publico no lee la tabla")
    void vistaPublica_sinAutor() {
        var e = publicado();
        reseñar(e, dispositivo(), 5, "Bien");

        var columnas = jdbc.sql("SELECT column_name FROM information_schema.columns WHERE table_schema = 'negocios' "
                + "AND table_name = 'v_resena_publica'").query(String.class).list();

        assertThat(columnas).isNotEmpty().doesNotContain("visitante_id", "respondida_por", "estado", "origen", "version");
        assertThatThrownBy(() -> transaccion.execute(estado -> {
            jdbc.sql("SET LOCAL ROLE rol_negocios_publico").update();
            return jdbc.sql("SELECT count(*) FROM negocios.resena").query(Integer.class).single();
        })).rootCause().hasMessageContaining("permission denied");
    }

    // ---- panel ----

    @Test
    @DisplayName("RS-04: el negocio responde una vez, sin editar; publica negocio.resena_respondida")
    void responder_unaVez() {
        var e = publicado();
        var dispositivo = dispositivo();
        var id = reseñar(e, dispositivo, 3, "Tardaron");

        var respondida = panel.responderResena(e.contexto, e.dueno, id, "  Gracias, lo mejoramos  ");
        var segunda = org.assertj.core.api.Assertions.catchThrowable(() -> panel.responderResena(e.contexto, e.dueno,
                id, "Otra"));

        assertThat(respondida.respuesta()).isEqualTo("Gracias, lo mejoramos");
        assertThat(respondida.respondidaEn()).isNotNull();
        assertThat(codigo(segunda)).isEqualTo(CodigoError.RESENA_YA_RESPONDIDA);
        var sobre = publicados("negocio.resena_respondida", e.negocioId).get(0);
        assertThat(sobre.datos()).containsEntry("resena_id", id.toString()).containsEntry("visitante_id", dispositivo);
        assertThat(sobre.datos().toString()).doesNotContain("mejoramos");
        assertThat(publicados("negocio.resena_respondida", e.negocioId)).hasSize(1);
    }

    @Test
    @DisplayName("RS-04: respuesta vacia o de mas de 500 caracteres, o de una reseña ajena, falla")
    void responder_invalidas() {
        var a = publicado();
        var b = publicado();
        var deA = reseñar(a, dispositivo(), 5, null);

        var vacia = org.assertj.core.api.Assertions.catchThrowable(() -> panel.responderResena(a.contexto, a.dueno,
                deA, " "));
        var larga = org.assertj.core.api.Assertions.catchThrowable(() -> panel.responderResena(a.contexto, a.dueno,
                deA, "x".repeat(501)));
        var ajena = org.assertj.core.api.Assertions.catchThrowable(() -> panel.responderResena(b.contexto, b.dueno,
                deA, "Hola"));
        var inexistente = org.assertj.core.api.Assertions.catchThrowable(() -> panel.responderResena(a.contexto,
                a.dueno, UUID.randomUUID(), "Hola"));

        assertThat(codigo(vacia)).isEqualTo(CodigoError.RESPUESTA_INVALIDA);
        assertThat(codigo(larga)).isEqualTo(CodigoError.RESPUESTA_INVALIDA);
        assertThat(codigo(ajena)).isEqualTo(CodigoError.RESENA_NO_ENCONTRADA);
        assertThat(codigo(inexistente)).isEqualTo(CodigoError.RESENA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("SG-04: ver las reseñas pide negocios.resena.ver y responderlas, negocios.resena.responder")
    void permisos() {
        var e = publicado();
        var id = reseñar(e, dispositivo(), 5, null);

        assertThatThrownBy(() -> panel.resenas(e.contexto, e.nadie, FiltroResenas.TODAS, 20, null))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> panel.responderResena(e.contexto, e.con("negocios.resena.ver"), id, "Hola"))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThat(panel.resenas(e.contexto, e.con("negocios.resena.ver"), FiltroResenas.TODAS, 20, null).resenas())
                .hasSize(1);
    }

    @Test
    @DisplayName("el panel filtra las que no tienen respuesta y pagina con cursor")
    void panel_filtroYPagina() {
        var e = publicado();
        var ids = new java.util.ArrayList<UUID>();
        for (var i = 0; i < 5; i++) {
            RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plusSeconds(i));
            ids.add(reseñar(e, dispositivo(), 5, null));
        }
        panel.responderResena(e.contexto, e.dueno, ids.get(0), "Gracias");

        var sinRespuesta = panel.resenas(e.contexto, e.dueno, FiltroResenas.SIN_RESPUESTA, 20, null);
        var primera = panel.resenas(e.contexto, e.dueno, FiltroResenas.TODAS, 2, null);
        var segunda = panel.resenas(e.contexto, e.dueno, FiltroResenas.TODAS, 2, primera.cursorFinal());

        assertThat(sinRespuesta.resenas()).extracting("id").doesNotContain(ids.get(0)).hasSize(4);
        assertThat(primera.resenas()).hasSize(2);
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.resenas()).hasSize(2);
        assertThat(segunda.resenas()).extracting("id").doesNotContainAnyElementsOf(
                primera.resenas().stream().map(r -> r.id()).toList());
    }

    // ---- moderacion (R6) ----

    @Test
    @DisplayName("RS-05: la plataforma oculta una reseña: baja el resumen, sale de lo publico y el negocio ya no la responde; restituirla la regresa")
    void ocultarYRestituir() {
        var e = publicado();
        var primera = reseñar(e, dispositivo(), 5, "Bien");
        reseñar(e, dispositivo(), 1, "Mal");

        var oculta = admin.ocultarResena(e.negocioId, primera, "CONTENIDO_INDEBIDO", "operador-prueba");

        assertThat(oculta.estado()).isEqualTo("OCULTA");
        assertThat(publico.negocioPublico(e.negocioId).orElseThrow().calificacion().total()).isEqualTo(1);
        assertThat(resenas.resenasDeNegocio(e.negocioId, 20, null).resenas()).extracting("id").doesNotContain(primera);
        var error = org.assertj.core.api.Assertions.catchThrowable(() -> panel.responderResena(e.contexto, e.dueno,
                primera, "Hola"));
        assertThat(codigo(error)).isEqualTo(CodigoError.RESENA_NO_ENCONTRADA);
        var sobre = publicados("negocio.resena_publicada", e.negocioId).stream()
                .filter(s -> "OCULTADA".equals(s.datos().get("accion"))).findFirst().orElseThrow();
        assertThat(sobre.datos()).containsEntry("resena_id", primera.toString());

        var restituida = admin.restituirResena(e.negocioId, primera, "operador-prueba");

        assertThat(restituida.estado()).isEqualTo("PUBLICADA");
        assertThat(publico.negocioPublico(e.negocioId).orElseThrow().calificacion().total()).isEqualTo(2);
        var historial = ejecutor.enNegocio(e.negocioId, () -> jdbc.sql("SELECT accion || '|' || actor FROM "
                + "negocios.historial_negocio WHERE negocio_id = :id AND accion LIKE '%RESENA' ORDER BY ocurrido_en")
                .param("id", e.negocioId).query(String.class).list());
        // El reloj de la prueba esta fijo: las dos filas tienen el mismo ocurrido_en y su orden en la consulta no esta definido.
        assertThat(historial).containsExactlyInAnyOrder("OCULTAR_RESENA|operador-prueba", "RESTITUIR_RESENA|operador-prueba");
    }

    @Test
    @DisplayName("una reseña de otro negocio o inexistente no se puede ocultar")
    void ocultar_ajena() {
        var a = publicado();
        var b = publicado();
        var deA = reseñar(a, dispositivo(), 5, null);

        var ajena = org.assertj.core.api.Assertions.catchThrowable(() -> admin.ocultarResena(b.negocioId, deA, "X",
                "operador"));

        assertThat(codigo(ajena)).isEqualTo(CodigoError.RESENA_NO_ENCONTRADA);
        assertThat(resenas.resenasDeNegocio(a.negocioId, 20, null).resenas()).hasSize(1);
    }
}
