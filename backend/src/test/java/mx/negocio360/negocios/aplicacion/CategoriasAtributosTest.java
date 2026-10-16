package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.DefinirAtributos;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.infraestructura.EjecutorEnContexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** N3: subcategorias (CA), atributos (AT), disponibilidad (DI) y publicacion (PU-01, PU-02) contra Postgres. */
@SpringBootTest
@RecordApplicationEvents
class CategoriasAtributosTest {

    @Autowired
    private NegociosApi negocios;
    @Autowired
    private AltasApi altas;
    @Autowired
    private EjecutorEnContexto ejecutor;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private ApplicationEvents eventos;

    private static CodigoError codigo(Throwable error) {
        assertThat(error).isInstanceOf(NegociosException.class);
        return ((NegociosException) error).codigo();
    }

    private List<SobreEvento> publicados(String tipo, UUID negocio) {
        return eventos.stream(SobreEvento.class).filter(e -> e.tipo().equals(tipo) && e.negocioId().equals(negocio))
                .toList();
    }

    // ---- subcategorias ----

    @Test
    @DisplayName("CA-01 y CA-03: se eligen subcategorias y las de otro giro salen como capacidad")
    void definirSubcategorias_conCapacidad() {
        var e = Escenario.nuevo(altas);

        var vista = negocios.definirSubcategorias(e.contexto, e.dueno, 0,
                List.of("GASTRONOMIA_TIPICA", "COMIDA_CORRIDA", "ARTESANIAS"));

        assertThat(vista.subcategorias()).extracting("codigo").containsExactly("GASTRONOMIA_TIPICA", "COMIDA_CORRIDA",
                "ARTESANIAS");
        assertThat(vista.subcategorias()).filteredOn(s -> s.codigo().equals("ARTESANIAS")).singleElement()
                .satisfies(s -> {
                    assertThat(s.capacidad()).isTrue();
                    assertThat(s.giro()).isEqualTo("MISCELANEA");
                    assertThat(s.nombre()).isEqualTo("Artesanías");
                });
        assertThat(vista.subcategorias()).filteredOn(s -> !s.codigo().equals("ARTESANIAS"))
                .allMatch(s -> !s.capacidad());
        assertThat(vista.giro()).as("CA-04: una subcategoria no cambia el giro").isEqualTo("RESTAURANTE");
        assertThat(vista.version()).isEqualTo(1);
        var sobre = publicados("negocio.actualizado", e.negocioId).get(0);
        assertThat((List<Object>) sobre.datos().get("campos")).containsExactly("subcategorias");
        assertThat((List<Object>) sobre.datos().get("subcategorias")).containsExactlyInAnyOrder("GASTRONOMIA_TIPICA",
                "COMIDA_CORRIDA", "ARTESANIAS");
    }

    @Test
    @DisplayName("CA-06: guardar reemplaza el conjunto completo; un conjunto vacio quita todas")
    void definirSubcategorias_reemplaza() {
        var e = Escenario.nuevo(altas);
        negocios.definirSubcategorias(e.contexto, e.dueno, 0, List.of("SIN_GLUTEN", "VERANO"));

        var reemplazo = negocios.definirSubcategorias(e.contexto, e.dueno, 1, List.of("MIXTO"));
        var vacio = negocios.definirSubcategorias(e.contexto, e.dueno, 2, List.of());

        assertThat(reemplazo.subcategorias()).extracting("codigo").containsExactly("MIXTO");
        assertThat(vacio.subcategorias()).isEmpty();
    }

    @Test
    @DisplayName("CA-01 y CA-02: mas de ocho, repetidas o inexistentes fallan y no cambian nada")
    void definirSubcategorias_invalidas() {
        var e = Escenario.nuevo(altas);
        negocios.definirSubcategorias(e.contexto, e.dueno, 0, List.of("SIN_GLUTEN"));

        var nueve = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirSubcategorias(e.contexto,
                e.dueno, 1, List.of("SIN_GLUTEN", "VERANO", "MIXTO", "EXTRANJERA", "COMIDA_CORRIDA",
                        "GASTRONOMIA_TIPICA", "HISTORIA", "CULTURA", "ARTE")));
        var inexistente = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirSubcategorias(
                e.contexto, e.dueno, 1, List.of("VERANO", "INEXISTENTE")));
        var repetida = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirSubcategorias(e.contexto,
                e.dueno, 1, List.of("VERANO", "VERANO")));

        assertThat(codigo(nueve)).isEqualTo(CodigoError.SUBCATEGORIAS_EXCEDIDAS);
        assertThat(codigo(inexistente)).isEqualTo(CodigoError.SUBCATEGORIA_INVALIDA);
        assertThat(codigo(repetida)).isEqualTo(CodigoError.SUBCATEGORIA_INVALIDA);
        var vista = negocios.miNegocio(e.contexto, e.dueno);
        assertThat(vista.subcategorias()).extracting("codigo").containsExactly("SIN_GLUTEN");
        assertThat(vista.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("NG-10 y SG-04: version vieja da CONCURRENCIA; sin negocios.perfil.editar, PERMISO_DENEGADO")
    void definirSubcategorias_versionYPermiso() {
        var e = Escenario.nuevo(altas);
        negocios.definirSubcategorias(e.contexto, e.dueno, 0, List.of("VERANO"));

        var vieja = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirSubcategorias(e.contexto,
                e.dueno, 0, List.of("MIXTO")));

        assertThat(codigo(vieja)).isEqualTo(CodigoError.CONCURRENCIA);
        assertThatThrownBy(() -> negocios.definirSubcategorias(e.contexto, e.nadie, 1, List.of("MIXTO")))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- atributos ----

    @Test
    @DisplayName("AT-01 a AT-05: se declaran dieta, accesibilidad, duracion y costo; no cuentan para la completitud")
    void definirAtributos_guardaYPublica() {
        var e = Escenario.nuevo(altas);

        var vista = negocios.definirAtributos(e.contexto, e.dueno, new DefinirAtributos(0,
                List.of("VEGETARIANA", "SIN_GLUTEN"), List.of("ACCESO_SIN_ESCALONES"), 90, 180));

        assertThat(vista.atributos().dieta()).extracting("codigo").containsExactly("VEGETARIANA", "SIN_GLUTEN");
        assertThat(vista.atributos().dieta().get(0).nombre()).isEqualTo("Vegetariana");
        assertThat(vista.atributos().accesibilidad()).extracting("codigo").containsExactly("ACCESO_SIN_ESCALONES");
        assertThat(vista.atributos().duracionVisitaMin()).isEqualTo(90);
        assertThat(vista.atributos().costoMedioMxn()).isEqualTo(180);
        assertThat(vista.completitud().porcentaje()).as("AT-01").isEqualTo(40);
        var datos = publicados("negocio.actualizado", e.negocioId).get(0).datos();
        assertThat((List<Object>) datos.get("campos")).contains("atributos");
        assertThat((List<Object>) datos.get("dieta")).containsExactly("VEGETARIANA", "SIN_GLUTEN");
        assertThat((List<Object>) datos.get("accesibilidad")).containsExactly("ACCESO_SIN_ESCALONES");
        assertThat(datos).containsEntry("duracion_visita_min", 90).containsEntry("costo_medio_mxn", 180);
    }

    @Test
    @DisplayName("AT-01: todo es opcional; las listas vacias y los numeros nulos borran lo declarado")
    void definirAtributos_opcional_yBorrado() {
        var e = Escenario.nuevo(altas);
        negocios.definirAtributos(e.contexto, e.dueno, new DefinirAtributos(0, List.of("VEGANA"), List.of(), 60, 100));

        var vista = negocios.definirAtributos(e.contexto, e.dueno, new DefinirAtributos(1, List.of(), List.of(), null,
                null));

        assertThat(vista.atributos().dieta()).isEmpty();
        assertThat(vista.atributos().accesibilidad()).isEmpty();
        assertThat(vista.atributos().duracionVisitaMin()).isNull();
        assertThat(vista.atributos().costoMedioMxn()).isNull();
        var datos = publicados("negocio.actualizado", e.negocioId).get(1).datos();
        assertThat(datos).doesNotContainKeys("duracion_visita_min", "costo_medio_mxn");
    }

    @Test
    @DisplayName("AT-02 y AT-03: codigos del catalogo, hasta diez por tipo, duracion de 5 a 480 y costo de 0 a 100 000")
    void definirAtributos_invalidos() {
        var e = Escenario.nuevo(altas);
        var inexistente = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto,
                e.dueno, new DefinirAtributos(0, List.of("PALEO"), List.of(), null, null)));
        var tipoCambiado = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto,
                e.dueno, new DefinirAtributos(0, List.of(), List.of("VEGANA"), null, null)));
        var repetido = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto,
                e.dueno, new DefinirAtributos(0, List.of("VEGANA", "VEGANA"), List.of(), null, null)));
        var once = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto, e.dueno,
                new DefinirAtributos(0, java.util.Collections.nCopies(11, "VEGANA"), List.of(), null, null)));
        var poco = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto, e.dueno,
                new DefinirAtributos(0, List.of(), List.of(), 4, null)));
        var mucho = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto,
                e.dueno, new DefinirAtributos(0, List.of(), List.of(), 481, null)));
        var negativo = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto,
                e.dueno, new DefinirAtributos(0, List.of(), List.of(), null, -1)));
        var caro = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirAtributos(e.contexto, e.dueno,
                new DefinirAtributos(0, List.of(), List.of(), null, 100_001)));

        assertThat(codigo(inexistente)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(codigo(tipoCambiado)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(codigo(repetido)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(codigo(once)).isEqualTo(CodigoError.ATRIBUTOS_EXCEDIDOS);
        assertThat(codigo(poco)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(codigo(mucho)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(codigo(negativo)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(codigo(caro)).isEqualTo(CodigoError.ATRIBUTO_INVALIDO);
        assertThat(negocios.miNegocio(e.contexto, e.dueno).version()).isZero();
    }

    @Test
    @DisplayName("SG-04: declarar atributos pide negocios.perfil.editar; los limites de AT-03 son inclusivos")
    void definirAtributos_permisoYLimites() {
        var e = Escenario.nuevo(altas);

        assertThatThrownBy(() -> negocios.definirAtributos(e.contexto, e.con("negocios.perfil.ver"),
                new DefinirAtributos(0, List.of(), List.of(), null, null))).isInstanceOf(PermisoDenegadoException.class);
        var bordes = negocios.definirAtributos(e.contexto, e.dueno, new DefinirAtributos(0, List.of(), List.of(), 5, 0));
        var altos = negocios.definirAtributos(e.contexto, e.dueno, new DefinirAtributos(1, List.of(), List.of(), 480,
                100_000));

        assertThat(bordes.atributos().costoMedioMxn()).isZero();
        assertThat(altos.atributos().duracionVisitaMin()).isEqualTo(480);
    }

    // ---- disponibilidad ----

    @Test
    @DisplayName("DI-01 y DI-05: se declara el nivel, vence en 3 horas y publica solo cuando cambia")
    void definirDisponibilidad_publicaSoloSiCambia() {
        var e = Escenario.nuevo(altas);
        var antes = Instant.now();

        var primera = negocios.definirDisponibilidad(e.contexto, e.dueno, "LLENA", "Solo barra");
        var repetida = negocios.definirDisponibilidad(e.contexto, e.dueno, "LLENA", null);
        var cambio = negocios.definirDisponibilidad(e.contexto, e.dueno, "LIBRE", null);

        assertThat(primera.nivel()).isEqualTo("LLENA");
        assertThat(primera.nota()).isEqualTo("Solo barra");
        assertThat(primera.venceEn()).isBetween(antes.plus(3, ChronoUnit.HOURS).minusSeconds(5),
                Instant.now().plus(3, ChronoUnit.HOURS).plusSeconds(5));
        assertThat(repetida.venceEn()).isAfterOrEqualTo(primera.venceEn());
        assertThat(cambio.nivel()).isEqualTo("LIBRE");
        var avisos = publicados("negocio.disponibilidad_actualizada", e.negocioId);
        assertThat(avisos).hasSize(2);
        assertThat(avisos.get(0).datos()).containsEntry("nivel_nuevo", "LLENA").containsEntry("fuente", "MANUAL")
                .doesNotContainKey("nivel_anterior");
        assertThat(avisos.get(1).datos()).containsEntry("nivel_anterior", "LLENA").containsEntry("nivel_nuevo", "LIBRE");
        assertThat(negocios.miNegocio(e.contexto, e.dueno).disponibilidad().nivel()).isEqualTo("LIBRE");
    }

    @Test
    @DisplayName("DI-01: un nivel que no existe o una nota de mas de 80 caracteres fallan")
    void definirDisponibilidad_invalida() {
        var e = Escenario.nuevo(altas);

        var nivel = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirDisponibilidad(e.contexto,
                e.dueno, "PLENO", null));
        var nota = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirDisponibilidad(e.contexto,
                e.dueno, "LIBRE", "x".repeat(81)));

        assertThat(codigo(nivel)).isEqualTo(CodigoError.DISPONIBILIDAD_INVALIDA);
        assertThat(codigo(nota)).isEqualTo(CodigoError.DISPONIBILIDAD_INVALIDA);
        assertThat(negocios.miNegocio(e.contexto, e.dueno).disponibilidad()).isNull();
    }

    @Test
    @DisplayName("DI-02: declarar la disponibilidad pide negocios.disponibilidad.editar")
    void definirDisponibilidad_permiso() {
        var e = Escenario.nuevo(altas);

        assertThatThrownBy(() -> negocios.definirDisponibilidad(e.contexto, e.con("negocios.perfil.editar"), "LIBRE",
                null)).isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- publicar ----

    @Test
    @DisplayName("PU-01: publicar un negocio recien dado de alta falla y dice que falta: ubicacion y horario")
    void publicar_incompleto_dicePorQue() {
        var e = Escenario.nuevo(altas);

        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.publicarNegocio(e.contexto, e.dueno,
                true));

        assertThat(codigo(error)).isEqualTo(CodigoError.PUBLICACION_INCOMPLETA);
        assertThat(((NegociosException) error).faltantes()).containsExactly("UBICACION", "HORARIO");
        assertThat(negocios.miNegocio(e.contexto, e.dueno).publicado()).isFalse();
    }

    @Test
    @DisplayName("PU-01: con ubicacion falta solo el horario; completo, publica, y despublicar es siempre posible")
    void publicar_completo_yDespublicar() {
        var e = Escenario.nuevo(altas);
        var sucursal = negocios.sucursales(e.contexto, e.dueno).get(0);
        negocios.actualizarSucursal(e.contexto, e.dueno, sucursal.id(), new ActualizarSucursal(0, null, null, null,
                null, null, null, null, null, new BigDecimal("19.0433"), new BigDecimal("-98.1982"), null));
        var soloHorario = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.publicarNegocio(e.contexto,
                e.dueno, true));
        negocios.definirHorario(e.contexto, e.dueno, sucursal.id(), 0,
                List.of(new DiaHorarioComando(1, List.of(new TramoComando("08:00", "17:00")))));

        var publicado = negocios.publicarNegocio(e.contexto, e.dueno, true);
        var oculto = negocios.publicarNegocio(e.contexto, e.dueno, false);

        assertThat(((NegociosException) soloHorario).faltantes()).containsExactly("HORARIO");
        assertThat(publicado.publicado()).isTrue();
        assertThat(oculto.publicado()).isFalse();
        var cambios = publicados("negocio.actualizado", e.negocioId);
        assertThat(cambios).hasSize(2);
        assertThat(cambios.get(0).datos()).containsEntry("publicado", true);
        assertThat((List<Object>) cambios.get(0).datos().get("campos")).containsExactly("publicado");
        assertThat(cambios.get(1).datos()).containsEntry("publicado", false);
    }

    @Test
    @DisplayName("SG-04: publicar pide negocios.negocio.publicar; NG-14: uno suspendido no se publica")
    void publicar_permisoYSuspendido() {
        var e = Escenario.nuevo(altas);
        var suspendido = Escenario.nuevo(altas);
        ejecutor.enNegocio(suspendido.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'SUSPENDIDO', "
                + "suspendido_por = 'PLATAFORMA' WHERE id = :id").param("id", suspendido.negocioId).update());

        assertThatThrownBy(() -> negocios.publicarNegocio(e.contexto, e.con("negocios.perfil.editar"), true))
                .isInstanceOf(PermisoDenegadoException.class);
        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.publicarNegocio(suspendido.contexto,
                suspendido.dueno, true));
        assertThat(codigo(error)).isEqualTo(CodigoError.NEGOCIO_SUSPENDIDO);
    }
}
