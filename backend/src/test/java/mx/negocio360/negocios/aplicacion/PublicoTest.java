package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.negocios.ActualizarNegocio;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.CercaDe;
import mx.negocio360.negocios.DefinirAtributos;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.FiltroPublico;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.NegociosPublicosApi;
import mx.negocio360.negocios.OrdenPublico;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.infraestructura.EjecutorEnContexto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * N3: la superficie publica (PU-01 a PU-06, SN-14, DI-03, DI-04) contra Postgres, con un reloj que se mueve. Las
 * busquedas filtran por un texto unico de cada prueba, porque la base la comparten todas las pruebas.
 */
@SpringBootTest
@Import(PublicoTest.RelojConfig.class)
class PublicoTest {

    /** Jueves 15 de octubre de 2026, 10:30 en la Ciudad de Mexico (UTC-6). */
    private static final Instant JUEVES_1030 = Instant.parse("2026-10-15T16:30:00Z");
    private static final RelojDePrueba RELOJ = new RelojDePrueba(JUEVES_1030);

    @TestConfiguration
    static class RelojConfig {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return RELOJ;
        }
    }

    @Autowired
    private NegociosApi panel;
    @Autowired
    private NegociosPublicosApi publico;
    @Autowired
    private AltasApi altas;
    @Autowired
    private EjecutorEnContexto ejecutor;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private TransactionTemplate transaccion;

    @BeforeEach
    void relojAlJueves() {
        RELOJ.fijar(JUEVES_1030);
    }

    private static String marca() {
        return "Zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** Da de alta un negocio, le pone ubicacion y horario (jueves 08:00-17:00) y lo publica. */
    private Escenario publicado(String nombre, String giro, double latitud, double longitud) {
        var e = Escenario.nuevo(altas, nombre, giro);
        var sucursal = panel.sucursales(e.contexto, e.dueno).get(0);
        panel.actualizarSucursal(e.contexto, e.dueno, sucursal.id(), new ActualizarSucursal(0, null, "5 de Mayo", "312",
                "Centro", "Puebla", "Puebla", "72000", null, BigDecimal.valueOf(latitud), BigDecimal.valueOf(longitud),
                null));
        panel.definirHorario(e.contexto, e.dueno, sucursal.id(), 0,
                List.of(new DiaHorarioComando(4, List.of(new TramoComando("08:00", "17:00")))));
        panel.publicarNegocio(e.contexto, e.dueno, true);
        return e;
    }

    /** La version actual del negocio: publicar la sube, y cada edicion debe llevar la que vio. */
    private long v(Escenario e) {
        return panel.miNegocio(e.contexto, e.dueno).version();
    }

    private static FiltroPublico texto(String texto) {
        return new FiltroPublico(texto, null, null, null, null, null, null, null);
    }

    // ---- ficha publica ----

    @Test
    @DisplayName("PU-01 y PU-03: la ficha publica trae lo permitido; el contacto, solo si el dueno lo marco (PU-04)")
    void fichaPublica_loPermitido() {
        var marca = marca();
        var e = publicado("Fonda " + marca, "RESTAURANTE", 19.0433, -98.1982);
        panel.actualizarNegocio(e.contexto, e.dueno, new ActualizarNegocio(v(e), null, "Comida de la casa", "2225550101",
                "chela@fonda.example", false));
        panel.definirSubcategorias(e.contexto, e.dueno, v(e), List.of("GASTRONOMIA_TIPICA", "ARTESANIAS"));
        panel.definirAtributos(e.contexto, e.dueno, new DefinirAtributos(v(e), List.of("VEGETARIANA"),
                List.of("ACCESO_SIN_ESCALONES"), 90, 180));

        var privado = publico.negocioPublico(e.negocioId).orElseThrow();
        panel.actualizarNegocio(e.contexto, e.dueno, new ActualizarNegocio(v(e), null, null, null, null, true));
        var conContacto = publico.negocioPublico(e.negocioId).orElseThrow();

        assertThat(privado.nombre()).isEqualTo("Fonda " + marca);
        assertThat(privado.giro()).isEqualTo("RESTAURANTE");
        assertThat(privado.giroNombre()).isEqualTo("Restaurante");
        assertThat(privado.descripcion()).isEqualTo("Comida de la casa");
        assertThat(privado.telefono()).isNull();
        assertThat(privado.correo()).isNull();
        assertThat(conContacto.telefono()).isEqualTo("+522225550101");
        assertThat(conContacto.correo()).isEqualTo("chela@fonda.example");
        assertThat(privado.ubicacion().calle()).isEqualTo("5 de Mayo");
        assertThat(privado.ubicacion().latitud()).isEqualByComparingTo("19.0433");
        assertThat(privado.subcategorias()).extracting("codigo").containsExactly("GASTRONOMIA_TIPICA", "ARTESANIAS");
        assertThat(privado.subcategorias().get(1).capacidad()).isTrue();
        assertThat(privado.atributos().dieta()).extracting("codigo").containsExactly("VEGETARIANA");
        assertThat(privado.atributos().duracionVisitaMin()).isEqualTo(90);
        assertThat(privado.calificacion().total()).isZero();
        assertThat(privado.calificacion().promedio()).isNull();
        assertThat(privado.simulado()).isFalse();
        assertThat(privado.distanciaKm()).isNull();
        assertThat(privado.toString()).doesNotContain(e.cuentaId.toString());
    }

    @Test
    @DisplayName("PU-06: abierto ahora se calcula en el servidor con la zona del negocio")
    void abiertoAhora_loCalculaElServidor() {
        var e = publicado("Fonda " + marca(), "RESTAURANTE", 19.0433, -98.1982);

        var abierto = publico.negocioPublico(e.negocioId).orElseThrow();
        RELOJ.fijar(Instant.parse("2026-10-15T14:30:00Z")); // 08:30 en Mexico
        var tempranoAbierto = publico.negocioPublico(e.negocioId).orElseThrow();
        RELOJ.fijar(Instant.parse("2026-10-15T13:30:00Z")); // 07:30 en Mexico
        var tempranoCerrado = publico.negocioPublico(e.negocioId).orElseThrow();
        RELOJ.fijar(Instant.parse("2026-10-16T16:30:00Z")); // viernes
        var viernes = publico.negocioPublico(e.negocioId).orElseThrow();

        assertThat(abierto.horario().abiertoAhora()).isTrue();
        assertThat(abierto.horario().zonaHoraria()).isEqualTo("America/Mexico_City");
        assertThat(tempranoAbierto.horario().abiertoAhora()).isTrue();
        assertThat(tempranoCerrado.horario().abiertoAhora()).isFalse();
        assertThat(viernes.horario().abiertoAhora()).isFalse();
        assertThat(abierto.horario().semana()).extracting("dia").containsExactly(4);
    }

    @Test
    @DisplayName("PU-01 y PU-02: un negocio sin publicar, pausado, suspendido o cerrado responde igual que uno inexistente")
    void noPublico_respondeComoInexistente() {
        var sinPublicar = Escenario.nuevo(altas, "Fonda " + marca(), "RESTAURANTE");
        var e = publicado("Fonda " + marca(), "RESTAURANTE", 19.04, -98.19);
        assertThat(publico.negocioPublico(e.negocioId)).isPresent();

        ejecutor.enNegocio(e.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'SUSPENDIDO', "
                + "suspendido_por = 'DUENO' WHERE id = :id").param("id", e.negocioId).update());
        var suspendido = publico.negocioPublico(e.negocioId);
        ejecutor.enNegocio(e.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'ACTIVO', "
                + "suspendido_por = NULL WHERE id = :id").param("id", e.negocioId).update());
        var reanudado = publico.negocioPublico(e.negocioId);
        ejecutor.enNegocio(e.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'CERRADO', "
                + "cerrado_en = now() WHERE id = :id").param("id", e.negocioId).update());
        var cerrado = publico.negocioPublico(e.negocioId);

        assertThat(publico.negocioPublico(sinPublicar.negocioId)).isEmpty();
        assertThat(publico.negocioPublico(UUID.randomUUID())).isEmpty();
        assertThat(suspendido).as("pausado o suspendido").isEmpty();
        assertThat(reanudado).as("PU-02: al reanudar vuelve solo, sin tocar su bandera").isPresent();
        assertThat(cerrado).isEmpty();
    }

    @Test
    @DisplayName("DI-03 y DI-04: la disponibilidad vigente sale; vencida, o con el negocio cerrado ahora, sale como sin dato")
    void disponibilidad_vigenciaYApertura() {
        var e = publicado("Fonda " + marca(), "RESTAURANTE", 19.04, -98.19);
        panel.definirDisponibilidad(e.contexto, e.dueno, "MODERADA", "Mesas en la terraza");

        var vigente = publico.negocioPublico(e.negocioId).orElseThrow();
        RELOJ.fijar(JUEVES_1030.plus(Duration.ofHours(3)).plusSeconds(1));
        var vencida = publico.negocioPublico(e.negocioId).orElseThrow();
        RELOJ.fijar(JUEVES_1030.plus(Duration.ofMinutes(5)));
        var antesDeVencer = publico.negocioPublico(e.negocioId).orElseThrow();
        RELOJ.fijar(Instant.parse("2026-10-15T13:30:00Z")); // 07:30: cerrado ahora
        var cerradoAhora = publico.negocioPublico(e.negocioId).orElseThrow();

        assertThat(vigente.disponibilidad()).isNotNull();
        assertThat(vigente.disponibilidad().nivel()).isEqualTo("MODERADA");
        assertThat(vigente.disponibilidad().nota()).isEqualTo("Mesas en la terraza");
        assertThat(antesDeVencer.disponibilidad()).isNotNull();
        assertThat(vencida.disponibilidad()).as("vencida: sin dato, nunca LIBRE").isNull();
        assertThat(cerradoAhora.disponibilidad()).isNull();
    }

    // ---- busqueda ----

    @Test
    @DisplayName("la busqueda por texto no distingue mayusculas ni acentos")
    void busqueda_porTexto() {
        var marca = marca();
        var e = publicado("Fonda Doña " + marca, "RESTAURANTE", 19.04, -98.19);

        var porMarca = publico.negociosPublicos(texto(marca.toLowerCase()), 20, null);
        var sinAcento = publico.negociosPublicos(texto("dona " + marca.toUpperCase()), 20, null);
        var ninguno = publico.negociosPublicos(texto(marca + "xyz"), 20, null);

        assertThat(porMarca.negocios()).extracting("id").containsExactly(e.negocioId);
        assertThat(sinAcento.negocios()).extracting("id").containsExactly(e.negocioId);
        assertThat(ninguno.negocios()).isEmpty();
        assertThat(ninguno.hayMas()).isFalse();
    }

    @Test
    @DisplayName("la busqueda filtra por giro, por subcategorias (todas), por dieta y por accesibilidad")
    void busqueda_porCategoriasYAtributos() {
        var marca = marca();
        var vegano = publicado("Verde " + marca, "RESTAURANTE", 19.04, -98.19);
        var comun = publicado("Comun " + marca, "RESTAURANTE", 19.04, -98.19);
        var tienda = publicado("Tienda " + marca, "MISCELANEA", 19.04, -98.19);
        panel.definirSubcategorias(vegano.contexto, vegano.dueno, v(vegano), List.of("MIXTO", "VERANO"));
        panel.definirSubcategorias(comun.contexto, comun.dueno, v(comun), List.of("MIXTO"));
        panel.definirAtributos(vegano.contexto, vegano.dueno, new DefinirAtributos(v(vegano), List.of("VEGANA"),
                List.of("BANO_ACCESIBLE"), null, null));

        var restaurantes = publico.negociosPublicos(new FiltroPublico(marca, "RESTAURANTE", null, null, null, null,
                null, null), 20, null);
        var mixtoYVerano = publico.negociosPublicos(new FiltroPublico(marca, null, List.of("MIXTO", "VERANO"), null,
                null, null, null, null), 20, null);
        var mixto = publico.negociosPublicos(new FiltroPublico(marca, null, List.of("MIXTO"), null, null, null, null,
                null), 20, null);
        var dieta = publico.negociosPublicos(new FiltroPublico(marca, null, null, List.of("VEGANA"), null, null, null,
                null), 20, null);
        var acceso = publico.negociosPublicos(new FiltroPublico(marca, null, null, null, List.of("BANO_ACCESIBLE"),
                null, null, null), 20, null);

        assertThat(restaurantes.negocios()).extracting("id").containsExactlyInAnyOrder(vegano.negocioId, comun.negocioId);
        assertThat(mixtoYVerano.negocios()).extracting("id").containsExactly(vegano.negocioId);
        assertThat(mixto.negocios()).extracting("id").containsExactlyInAnyOrder(vegano.negocioId, comun.negocioId);
        assertThat(dieta.negocios()).as("AT-06: no declarar no es cumplir").extracting("id")
                .containsExactly(vegano.negocioId);
        assertThat(acceso.negocios()).extracting("id").containsExactly(vegano.negocioId);
        assertThat(tienda.negocioId).isNotNull();
    }

    @Test
    @DisplayName("PU-06: el filtro abiertoAhora deja solo los que abren a esa hora")
    void busqueda_abiertoAhora() {
        var marca = marca();
        var e = publicado("Fonda " + marca, "RESTAURANTE", 19.04, -98.19);
        var filtro = new FiltroPublico(marca, null, null, null, null, true, null, null);

        var abierto = publico.negociosPublicos(filtro, 20, null);
        RELOJ.fijar(Instant.parse("2026-10-15T13:30:00Z")); // 07:30
        var cerrado = publico.negociosPublicos(filtro, 20, null);

        assertThat(abierto.negocios()).extracting("id").containsExactly(e.negocioId);
        assertThat(cerrado.negocios()).isEmpty();
    }

    @Test
    @DisplayName("la busqueda por cercania da la distancia, ordena por ella y respeta el radio")
    void busqueda_porCercania() {
        var marca = marca();
        // Zocalo de Puebla y dos puntos a ~1.1 km y ~11 km al norte.
        var cerca = publicado("Cerca " + marca, "RESTAURANTE", 19.0450, -98.2000);
        var medio = publicado("Medio " + marca, "RESTAURANTE", 19.0550, -98.2000);
        var lejos = publicado("Lejos " + marca, "RESTAURANTE", 19.1450, -98.2000);

        var cinco = publico.negociosPublicos(new FiltroPublico(marca, null, null, null, null, null,
                new CercaDe(19.0440, -98.2000, 5), OrdenPublico.DISTANCIA), 20, null);
        var veinte = publico.negociosPublicos(new FiltroPublico(marca, null, null, null, null, null,
                new CercaDe(19.0440, -98.2000, 20), OrdenPublico.DISTANCIA), 20, null);

        assertThat(cinco.negocios()).extracting("id").containsExactly(cerca.negocioId, medio.negocioId);
        assertThat(cinco.negocios().get(0).distanciaKm()).isBetween(0.1, 0.2);
        assertThat(veinte.negocios()).extracting("id").containsExactly(cerca.negocioId, medio.negocioId,
                lejos.negocioId);
        assertThat(veinte.negocios().get(2).distanciaKm()).isBetween(10.0, 12.0);
    }

    @Test
    @DisplayName("PU-05: 20 por pagina como maximo, con cursor; el texto y el radio tienen tope")
    void busqueda_topes_yPaginacion() {
        var marca = marca();
        for (var i = 0; i < 23; i++) {
            publicado("Local %02d %s".formatted(i, marca), "RESTAURANTE", 19.04, -98.19);
        }

        var primera = publico.negociosPublicos(texto(marca), 500, null);
        var segunda = publico.negociosPublicos(texto(marca), 500, primera.cursorFinal());

        assertThat(primera.negocios()).hasSize(20);
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.negocios()).hasSize(3);
        assertThat(segunda.hayMas()).isFalse();
        assertThat(segunda.negocios()).extracting("id").doesNotContainAnyElementsOf(
                primera.negocios().stream().map(n -> n.id()).toList());
        var radio = org.assertj.core.api.Assertions.catchThrowable(() -> publico.negociosPublicos(
                new FiltroPublico(null, null, null, null, null, null, new CercaDe(19.04, -98.19, 100), null), 20, null));
        var largo = org.assertj.core.api.Assertions.catchThrowable(() -> publico.negociosPublicos(
                texto("x".repeat(81)), 20, null));
        var cursorRoto = org.assertj.core.api.Assertions.catchThrowable(() -> publico.negociosPublicos(texto(marca),
                20, "no-es-un-cursor"));
        assertThat(((NegociosException) radio).codigo()).isEqualTo(CodigoError.CONSULTA_INVALIDA);
        assertThat(((NegociosException) largo).codigo()).isEqualTo(CodigoError.CONSULTA_INVALIDA);
        assertThat(((NegociosException) cursorRoto).codigo()).isEqualTo(CodigoError.CONSULTA_INVALIDA);
    }

    @Test
    @DisplayName("un primeros de cero o negativo se trata como uno solo por pagina, nunca como todo")
    void busqueda_primerosNoPositivo() {
        var marca = marca();
        publicado("A " + marca, "RESTAURANTE", 19.04, -98.19);
        publicado("B " + marca, "RESTAURANTE", 19.04, -98.19);

        assertThat(publico.negociosPublicos(texto(marca), 0, null).negocios()).hasSize(1);
        assertThat(publico.negociosPublicos(texto(marca), -5, null).negocios()).hasSize(1);
    }

    // ---- SN-14, J2 ----

    @Test
    @DisplayName("SN-14: con el rol publico se lee la vista pero ninguna tabla base")
    void rolPublico_soloLaVista() {
        var e = publicado("Fonda " + marca(), "RESTAURANTE", 19.04, -98.19);

        var enLaVista = transaccion.execute(estado -> {
            jdbc.sql("SET LOCAL ROLE rol_negocios_publico").update();
            return jdbc.sql("SELECT count(*) FROM negocios.v_negocio_publico WHERE id = :id").param("id", e.negocioId)
                    .query(Integer.class).single();
        });

        assertThat(enLaVista).isEqualTo(1);
        for (var tabla : List.of("negocio", "sucursal", "alta_negocio", "historial_negocio", "disponibilidad")) {
            assertThatThrownBy(() -> transaccion.execute(estado -> {
                jdbc.sql("SET LOCAL ROLE rol_negocios_publico").update();
                return jdbc.sql("SELECT count(*) FROM negocios." + tabla).query(Integer.class).single();
            })).as(tabla).rootCause().hasMessageContaining("permission denied");
        }
    }

    @Test
    @DisplayName("la vista publica no tiene columnas privadas: ni dueno, ni cuenta, ni version, ni estado")
    void vista_sinColumnasPrivadas() {
        var columnas = jdbc.sql("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'negocios' AND table_name = 'v_negocio_publico'").query(String.class).list();

        assertThat(columnas).isNotEmpty().doesNotContain("solicitado_por", "version", "estado", "suspendido_por",
                "publicado", "telefono_contacto", "correo_contacto", "contacto_publico", "creado_en", "logo_archivo_id");
    }

    @Test
    @DisplayName("J2: vistaDeNegocio da nombre, giro, estado, zona y coordenadas a otros modulos; sin negocio, vacio")
    void vistaDeNegocio_paraOtrosModulos() {
        var e = publicado("Fonda " + marca(), "RESTAURANTE", 19.0433, -98.1982);

        var vista = publico.vistaDeNegocio(e.negocioId).orElseThrow();

        assertThat(vista.negocioId()).isEqualTo(e.negocioId);
        assertThat(vista.giro()).isEqualTo("RESTAURANTE");
        assertThat(vista.estado()).isEqualTo("ACTIVO");
        assertThat(vista.zonaHoraria()).isEqualTo("America/Mexico_City");
        assertThat(vista.latitud()).isEqualByComparingTo("19.0433");
        assertThat(publico.vistaDeNegocio(UUID.randomUUID())).isEmpty();
    }
}
