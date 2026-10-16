package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.DemostracionApi;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.FiltroResenas;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.NegociosPublicosApi;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.AdministracionApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.infraestructura.EjecutorEnContexto;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * N6: negocios simulados para el stand (DM-01, DM-02, DM-05) y restablecimiento de la demo (DM-04), contra Postgres. La
 * siembra es idempotente y deja datos rotulados como simulados.
 */
@SpringBootTest(properties = "negocios.demo.activo=true")
@Import(RelojDeResenas.class)
class DemostracionTest {

    @Autowired
    private DemostracionApi demo;
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

    @BeforeEach
    void relojAlInicio() {
        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO);
    }

    private static Autorizacion todo() {
        return Autorizacion.de(Escenario.TODOS_LOS_PERMISOS.stream().collect(Collectors.toSet()));
    }

    private static ContextoNegocio contexto(UUID negocio) {
        return new ContextoNegocio(negocio, "demo-prueba");
    }

    @Test
    @DisplayName("DM-02: la siembra crea doce negocios simulados (5 restaurantes, 3 museos y 4 misceláneas), publicos y completos")
    void sembrar_creaLosDoceNegocios() {
        var resultado = demo.sembrar();

        assertThat(resultado.negocios()).hasSize(12);
        var fichas = resultado.negocios().stream().map(id -> publico.negocioPublico(id).orElseThrow()).toList();
        assertThat(fichas).allSatisfy(f -> {
            assertThat(f.simulado()).as("DM-01: " + f.nombre()).isTrue();
            assertThat(f.ubicacion().latitud()).isNotNull();
            assertThat(f.horario().semana()).isNotEmpty();
            assertThat(f.subcategorias()).isNotEmpty();
            assertThat(f.descripcion()).isNotBlank();
            assertThat(f.calificacion().total()).isGreaterThanOrEqualTo(2);
            assertThat(f.calificacion().promedio()).isBetween(1.0, 5.0);
        });
        assertThat(fichas.stream().collect(Collectors.groupingBy(f -> f.giro(), Collectors.counting())))
                .containsEntry("RESTAURANTE", 5L).containsEntry("MUSEO", 3L).containsEntry("MISCELANEA", 4L);
        assertThat(fichas.stream().map(f -> f.ubicacion().municipio()).distinct()).contains("Puebla", "San Pedro Cholula");
        assertThat(fichas.stream().map(f -> f.nombre()).distinct()).hasSize(12);
        assertThat(fichas.stream().map(f -> f.ubicacion().colonia()).distinct())
                .contains("Centro", "Analco", "Barrio del Artista");
    }

    @Test
    @DisplayName("DM-02: los simulados nacen por el camino del alta: cada uno tiene su alta REGISTRADA")
    void sembrar_pasaPorElAlta() {
        var resultado = demo.sembrar();

        assertThat(panel.miNegocio(contexto(resultado.negocios().get(0)), todo()).id())
                .isEqualTo(resultado.negocios().get(0));
        var solicitante = ejecutor.enNegocio(resultado.negocios().get(0), () -> jdbc.sql(
                "SELECT solicitado_por FROM negocios.negocio WHERE id = :id").param("id", resultado.negocios().get(0))
                .query(UUID.class).single());
        assertThat(altas.misAltas(solicitante)).extracting("negocioId").containsAll(resultado.negocios());
        assertThat(altas.misAltas(solicitante)).allSatisfy(a -> assertThat(a.estado().name()).isEqualTo("REGISTRADO"));
    }

    @Test
    @DisplayName("DM-05: las reseñas sembradas llevan origen DEMOSTRACION y salen rotuladas; algunas ya tienen respuesta")
    void sembrar_rotulaLasResenas() {
        var negocio = demo.sembrar().negocios().get(0);

        var panelResenas = panel.resenas(contexto(negocio), todo(), FiltroResenas.TODAS, 20, null);
        var publicas = resenas.resenasDeNegocio(negocio, 20, null);

        assertThat(panelResenas.resenas()).isNotEmpty().allMatch(r -> r.demostracion());
        assertThat(publicas.resenas()).isNotEmpty().allMatch(r -> r.demostracion());
        assertThat(publicas.resenas()).extracting(r -> r.comentario()).allSatisfy(c -> assertThat(c).isNotBlank());
        var todas = demo.sembrar().negocios().stream()
                .flatMap(id -> resenas.resenasDeNegocio(id, 20, null).resenas().stream()).toList();
        assertThat(todas).anyMatch(r -> r.respuesta() != null).anyMatch(r -> r.respuesta() == null);
    }

    @Test
    @DisplayName("la siembra es idempotente: la segunda vez no crea nada y no duplica reseñas")
    void sembrar_esIdempotente() {
        var primera = demo.sembrar();
        var antes = primera.negocios().stream().mapToInt(id -> publico.negocioPublico(id).orElseThrow().calificacion()
                .total()).sum();

        var segunda = demo.sembrar();

        assertThat(segunda.negocios()).containsExactlyInAnyOrderElementsOf(primera.negocios());
        assertThat(segunda.negociosCreados()).isZero();
        assertThat(segunda.resenasSembradas()).isZero();
        var despues = segunda.negocios().stream().mapToInt(id -> publico.negocioPublico(id).orElseThrow()
                .calificacion().total()).sum();
        assertThat(despues).isEqualTo(antes);
    }

    @Test
    @DisplayName("DM-04: restablecer borra las reseñas y la disponibilidad del stand y deja la siembra como estaba")
    void restablecer_dejaLaSiembraComoEstaba() {
        var negocio = demo.sembrar().negocios().get(0);
        var antes = resenas.resenasDeNegocio(negocio, 20, null).resenas();
        var resumenAntes = publico.negocioPublico(negocio).orElseThrow().calificacion();
        // Lo que pasa en el stand: un visitante reseña, el dueño responde a una sembrada, la plataforma oculta otra y se
        // declara disponibilidad.
        var visitante = UUID.randomUUID().toString();
        var preparacion = resenas.prepararResena(visitante, negocio, 1, "Comentario del stand");
        resenas.confirmarResena(visitante, preparacion.token());
        var sembradaSinRespuesta = panel.resenas(contexto(negocio), todo(), FiltroResenas.SIN_RESPUESTA, 20, null)
                .resenas().stream().filter(r -> !r.comentario().equals("Comentario del stand")).findFirst().orElseThrow();
        panel.responderResena(contexto(negocio), todo(), sembradaSinRespuesta.id(), "Respuesta del stand");
        admin.ocultarResena(negocio, antes.get(0).id(), "PRUEBA", "operador");
        panel.definirDisponibilidad(contexto(negocio), todo(), "LLENA", "Del stand");

        var seis = demo.sembrar().negocios();
        for (var i = 1; i <= 4; i++) {
            resenas.prepararResena(visitante, seis.get(i), 5, null);
        }
        var topeAlcanzado = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(visitante,
                seis.get(5), 5, null));
        assertThat(topeAlcanzado).isInstanceOf(mx.negocio360.negocios.dominio.NegociosException.class);

        var resultado = demo.restablecer();

        assertThat(resultado.negocios()).isGreaterThanOrEqualTo(12);
        var despues = resenas.resenasDeNegocio(negocio, 20, null).resenas();
        assertThat(despues).extracting(r -> r.comentario()).containsExactlyInAnyOrderElementsOf(
                antes.stream().map(r -> r.comentario()).toList());
        assertThat(despues).extracting(r -> r.respuesta()).containsExactlyInAnyOrderElementsOf(
                antes.stream().map(r -> r.respuesta()).toList());
        assertThat(publico.negocioPublico(negocio).orElseThrow().calificacion()).isEqualTo(resumenAntes);
        assertThat(panel.miNegocio(contexto(negocio), todo()).disponibilidad()).isNull();
        // El limite por hora tambien se reinicia: el mismo dispositivo, que habia llegado a su tope, vuelve a poder.
        assertThat(resenas.prepararResena(visitante, seis.get(5), 5, null).token()).isNotBlank();
    }

    @Test
    @DisplayName("DM-04: restablecer no toca a un negocio que no es simulado")
    void restablecer_noTocaNegociosReales() {
        demo.sembrar();
        var real = Escenario.nuevo(altas, "Fonda Real Zr" + UUID.randomUUID().toString().substring(0, 8), "RESTAURANTE");
        var sucursal = panel.sucursales(real.contexto, real.dueno).get(0);
        panel.actualizarSucursal(real.contexto, real.dueno, sucursal.id(), new ActualizarSucursal(0, null, null, null,
                null, null, null, null, null, new BigDecimal("19.04"), new BigDecimal("-98.19"), null));
        panel.definirHorario(real.contexto, real.dueno, sucursal.id(), 0,
                List.of(new DiaHorarioComando(4, List.of(new TramoComando("08:00", "17:00")))));
        panel.publicarNegocio(real.contexto, real.dueno, true);
        var visitante = UUID.randomUUID().toString();
        resenas.confirmarResena(visitante, resenas.prepararResena(visitante, real.negocioId, 5, "Real").token());
        panel.definirDisponibilidad(real.contexto, real.dueno, "LIBRE", null);

        demo.restablecer();

        assertThat(resenas.resenasDeNegocio(real.negocioId, 20, null).resenas()).hasSize(1);
        assertThat(panel.miNegocio(real.contexto, real.dueno).disponibilidad()).isNotNull();
        assertThat(publico.negocioPublico(real.negocioId).orElseThrow().simulado()).isFalse();
    }

    @Test
    @DisplayName("la reseña sembrada no depende del reloj: restablecer pasada una hora deja lo mismo")
    void restablecer_pasadoElTiempo() {
        var negocio = demo.sembrar().negocios().get(1);
        var antes = resenas.resenasDeNegocio(negocio, 20, null).resenas().size();
        RelojDeResenas.RELOJ.fijar(RelojDeResenas.INICIO.plus(Duration.ofHours(2)));

        demo.restablecer();

        assertThat(resenas.resenasDeNegocio(negocio, 20, null).resenas()).hasSize(antes);
    }
}
