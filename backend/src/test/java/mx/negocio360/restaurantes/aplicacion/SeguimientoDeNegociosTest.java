package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.infraestructura.RestaurantesEnMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DM-01 y PB-01: Restaurantes crea su extension al escuchar {@code negocio.registrado} de un negocio de giro RESTAURANTE y
 * sigue su publicacion y su estado con {@code negocio.actualizado} y {@code negocio.estado_cambiado}. Lector tolerante (R5):
 * ignora lo que no usa, no lanza con datos malos y un evento viejo no pisa a uno nuevo.
 */
class SeguimientoDeNegociosTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-16T11:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-16T12:00:00Z");

    private final RestaurantesEnMemoria restaurantes = new RestaurantesEnMemoria();
    private final SeguimientoDeNegocios seguimiento = new SeguimientoDeNegocios(restaurantes);

    private static SobreEvento evento(String tipo, Instant cuando, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(cuando), tipo, 1, NEGOCIO, "negocios", cuando, NEGOCIO, null, datos);
    }

    private static SobreEvento registrado(String giro, Instant cuando) {
        return evento("negocio.registrado", cuando, Map.of("solicitado_por", UUID.randomUUID().toString(), "nombre", "Fonda Dona Chela", "giro", giro));
    }

    private static SobreEvento actualizado(String giro, boolean publicado, Instant cuando) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("campos", java.util.List.of("publicado"));
        datos.put("nombre", "Fonda Dona Chela");
        datos.put("giro", giro);
        datos.put("publicado", publicado);
        return evento("negocio.actualizado", cuando, datos);
    }

    private static SobreEvento estado(String nuevo, Instant cuando) {
        return evento("negocio.estado_cambiado", cuando, Map.of("estado_anterior", "ACTIVO", "estado_nuevo", nuevo, "origen", "DUENO"));
    }

    @Test
    @DisplayName("DM-01: negocio.registrado con giro RESTAURANTE crea la extension, activa y sin publicar")
    void registrado_creaLaExtension() {
        assertThat(seguimiento.procesar(registrado("RESTAURANTE", T0))).isEqualTo(ResultadoSeguimiento.APLICADO);

        var r = restaurantes.porNegocio(NEGOCIO).orElseThrow();
        assertThat(r.publicado()).isFalse();
        assertThat(r.estado()).isEqualTo("ACTIVO");
        assertThat(r.visible()).isFalse();
    }

    @Test
    @DisplayName("un negocio de otro giro no crea nada")
    void registrado_otroGiro() {
        assertThat(seguimiento.procesar(registrado("MUSEO", T0))).isEqualTo(ResultadoSeguimiento.IGNORADO);
        assertThat(seguimiento.procesar(registrado("MISCELANEA", T0))).isEqualTo(ResultadoSeguimiento.IGNORADO);

        assertThat(restaurantes.porNegocio(NEGOCIO)).isEmpty();
    }

    @Test
    @DisplayName("R3: el mismo evento dos veces deja una sola extension, sin pisar lo que ya cambio")
    void registrado_idempotente() {
        var alta = registrado("RESTAURANTE", T0);
        seguimiento.procesar(alta);
        seguimiento.procesar(actualizado("RESTAURANTE", true, T1));

        assertThat(seguimiento.procesar(alta)).isEqualTo(ResultadoSeguimiento.YA_PROCESADO);
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().publicado()).isTrue();
    }

    @Test
    @DisplayName("PB-01: publicar vuelve visible al restaurante y despublicar lo oculta")
    void actualizado_publicacion() {
        seguimiento.procesar(registrado("RESTAURANTE", T0));

        seguimiento.procesar(actualizado("RESTAURANTE", true, T1));
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().visible()).isTrue();

        seguimiento.procesar(actualizado("RESTAURANTE", false, T2));
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().visible()).isFalse();
    }

    @Test
    @DisplayName("un evento viejo que llega tarde no pisa el estado nuevo (sin orden global)")
    void actualizado_viejoNoPisa() {
        seguimiento.procesar(registrado("RESTAURANTE", T0));
        seguimiento.procesar(actualizado("RESTAURANTE", true, T2));

        assertThat(seguimiento.procesar(actualizado("RESTAURANTE", false, T1))).isEqualTo(ResultadoSeguimiento.OBSOLETO);
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().publicado()).isTrue();
    }

    @Test
    @DisplayName("si negocio.actualizado llega antes que negocio.registrado, crea la extension si es de un restaurante")
    void actualizado_antesDelAlta() {
        assertThat(seguimiento.procesar(actualizado("RESTAURANTE", true, T1))).isEqualTo(ResultadoSeguimiento.APLICADO);
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().visible()).isTrue();
        assertThat(seguimiento.procesar(registrado("RESTAURANTE", T0))).as("el alta vieja ya no pisa nada").isEqualTo(ResultadoSeguimiento.OBSOLETO);
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().visible()).isTrue();
    }

    @Test
    @DisplayName("negocio.actualizado de otro giro se ignora")
    void actualizado_otroGiro() {
        assertThat(seguimiento.procesar(actualizado("MUSEO", true, T1))).isEqualTo(ResultadoSeguimiento.IGNORADO);
        assertThat(restaurantes.porNegocio(NEGOCIO)).isEmpty();
    }

    @Test
    @DisplayName("PB-02 y PB-01: pausar o cerrar el negocio lo oculta sin tocar su publicacion; reanudar lo devuelve")
    void estado() {
        seguimiento.procesar(registrado("RESTAURANTE", T0));
        seguimiento.procesar(actualizado("RESTAURANTE", true, T1));

        seguimiento.procesar(estado("SUSPENDIDO", T2));
        var suspendido = restaurantes.porNegocio(NEGOCIO).orElseThrow();
        assertThat(suspendido.visible()).isFalse();
        assertThat(suspendido.publicado()).as("la bandera del dueño no se toca").isTrue();

        seguimiento.procesar(estado("ACTIVO", Instant.parse("2026-10-16T13:00:00Z")));
        assertThat(restaurantes.porNegocio(NEGOCIO).orElseThrow().visible()).isTrue();
    }

    @Test
    @DisplayName("un estado de un negocio que no es restaurante se ignora")
    void estado_sinExtension() {
        assertThat(seguimiento.procesar(estado("CERRADO", T1))).isEqualTo(ResultadoSeguimiento.IGNORADO);
    }

    @Test
    @DisplayName("lector tolerante: datos mal formados y eventos que no son de Negocios no lanzan y no cambian nada")
    void tolerancia() {
        assertThat(seguimiento.procesar(evento("negocio.registrado", T0, Map.of("nombre", "Sin giro")))).isEqualTo(ResultadoSeguimiento.DATOS_INVALIDOS);
        assertThat(seguimiento.procesar(evento("negocio.estado_cambiado", T0, Map.of("nada", "x")))).isEqualTo(ResultadoSeguimiento.DATOS_INVALIDOS);
        assertThat(seguimiento.procesar(evento("venta.registrada", T0, Map.of("venta_id", UUID.randomUUID().toString())))).isEqualTo(ResultadoSeguimiento.IGNORADO);
        var ajeno = new SobreEvento(UuidV7.generar(T0), "negocio.registrado", 1, NEGOCIO, "otro-modulo", T0, NEGOCIO, null,
                Map.of("giro", "RESTAURANTE"));
        assertThat(seguimiento.procesar(ajeno)).as("solo Negocios puede dar de alta un restaurante").isEqualTo(ResultadoSeguimiento.DATOS_INVALIDOS);
        assertThat(restaurantes.porNegocio(NEGOCIO)).isEmpty();
    }

    @Test
    @DisplayName("lee lo que usa y tolera campos nuevos que Negocios agregue")
    void camposNuevos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("giro", "RESTAURANTE");
        datos.put("campo_futuro", "algo");
        datos.put("solicitado_por", UUID.randomUUID().toString());

        assertThat(seguimiento.procesar(evento("negocio.registrado", T0, datos))).isEqualTo(ResultadoSeguimiento.APLICADO);
    }
}
