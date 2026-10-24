package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.pruebas.Permisos;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.infraestructura.InventarioNoDisponible;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RC-01 a RC-08 y RC-10: guardar una receta es preparar -&gt; confirmar. Preparar valida contra Inventario, calcula el costo
 * teorico y el margen y los muestra; confirmar revalida y crea la version siguiente. Las cifras del ejemplo (cemita poblana a
 * 85.00, costo 43.25) son las de la seccion 4.2 de docs/diseno/restaurantes.md y son simuladas.
 */
class ManejadorGuardarRecetaTest {

    private final EntornoDeRecetas e = new EntornoDeRecetas();
    private final UUID pan = e.inventario.articulo("Pan de cemita", "pieza", "6.00");
    private final UUID milanesa = e.inventario.articulo("Milanesa", "g", "0.18");
    private final UUID quesillo = e.inventario.articulo("Quesillo", "g", "0.16");
    private final UUID aguacate = e.inventario.articulo("Aguacate", "g", "0.07");
    private final UUID chipotle = e.inventario.articulo("Chipotle", "g", "0.12");
    private final UUID papalo = e.inventario.articulo("Papalo", "g", "0.05");
    private final UUID cebolla = e.inventario.articulo("Cebolla", "g", "0.03");
    private final PlatilloVista cemita = e.menu.crearPlatillo(e.ctxA, e.todo,
            new DatosPlatillo("Cemita poblana", null, new BigDecimal("85"), null, Set.of(), null));

    private static InsumoComando insumo(UUID articulo, String cantidad, String unidad, String merma) {
        return new InsumoComando(articulo, new BigDecimal(cantidad), unidad, new BigDecimal(merma));
    }

    private List<InsumoComando> ingredientesDeLaCemita() {
        return List.of(insumo(pan, "1", "pieza", "0"), insumo(milanesa, "120", "g", "5"), insumo(quesillo, "50", "g", "0"),
                insumo(aguacate, "60", "g", "10"), insumo(chipotle, "10", "g", "0"), insumo(papalo, "3", "g", "0"),
                insumo(cebolla, "20", "g", "0"));
    }

    private GuardarRecetaComando comando(List<InsumoComando> insumos) {
        return new GuardarRecetaComando(cemita.id(), 1, insumos);
    }

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    private String guardar(GuardarRecetaComando c) {
        var preparada = e.preparacion.preparar(e.ctxA, Permisos.TODOS, c);
        e.preparacion.confirmar(e.ctxA, Permisos.TODOS, preparada.token());
        return preparada.resumen();
    }

    @Test
    @DisplayName("RC-05, RC-06 y RC-08: preparar muestra los insumos, el costo teorico 43.25 y el margen 41.75 (49.12 %) sin escribir nada")
    void preparar_muestraCostoYMargen() {
        var preparada = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita()));

        assertThat(preparada.resumen()).contains("Cemita poblana").contains("versión 1").contains("Milanesa: 120 g (merma 5 %)")
                .contains("$43.25").contains("$85.00").contains("$41.75").contains("49.12 %");
        assertThat(e.recetasAlmacen.porPlatillo(EntornoDeRecetas.NEGOCIO_A, cemita.id())).as("preparar no escribe").isEmpty();
    }

    @Test
    @DisplayName("RC-01: confirmar guarda la version 1, activa la receta y el platillo ya puede pasar a modo RECETA")
    void confirmar_creaLaVersion1() {
        var token = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).token();

        var resultado = e.preparacion.confirmar(e.ctxA, Permisos.TODOS, token);

        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(resultado.recursoId()).isEqualTo(cemita.id());
        assertThat(resultado.datos()).containsEntry("version", "1").containsEntry("platillo_id", cemita.id().toString());
        var receta = e.recetasAlmacen.porPlatillo(EntornoDeRecetas.NEGOCIO_A, cemita.id()).orElseThrow();
        assertThat(receta.versionVigente().orElseThrow().insumos()).hasSize(7);
        assertThat(receta.versionVigente().orElseThrow().creadaPor()).isEqualTo("ana");
        assertThat(e.menu.cambiarModo(e.ctxA, e.todo, cemita.id(), ModoInventario.RECETA).tieneReceta()).isTrue();
    }

    @Test
    @DisplayName("RC-01: guardar otra vez crea la version 2 y cierra la 1 en ese instante; la 1 sigue siendo la de antes")
    void confirmar_segundaVersion() {
        guardar(comando(ingredientesDeLaCemita()));
        e.reloj.avanzar(Duration.ofHours(1));
        var antes = e.reloj.instant().minusSeconds(1);

        var resumen = guardar(comando(List.of(insumo(pan, "1", "pieza", "0"), insumo(milanesa, "150", "g", "5"))));

        assertThat(resumen).contains("versión 2");
        var receta = e.recetasAlmacen.porPlatillo(EntornoDeRecetas.NEGOCIO_A, cemita.id()).orElseThrow();
        assertThat(receta.versiones()).hasSize(2);
        assertThat(receta.versionEn(antes).orElseThrow().numero()).isEqualTo(1);
        assertThat(receta.versionEn(e.reloj.instant()).orElseThrow().numero()).isEqualTo(2);
    }

    @Test
    @DisplayName("RC-02: las unidades distintas a la base se aceptan si se pueden convertir (kg a g) y el costo usa la cantidad en base")
    void conversion() {
        var resumen = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(milanesa, "0.12", "kg", "0")))).resumen();

        assertThat(resumen).contains("Milanesa: 0.12 kg").contains("$21.60");
    }

    @Test
    @DisplayName("RC-05: si a un insumo le falta el costo, el costo teorico queda incompleto, se nombra el insumo y no hay margen")
    void costoIncompleto() {
        var sinCosto = e.inventario.articulo("Chile en polvo", "g", null);

        var resumen = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(pan, "1", "pieza", "0"), insumo(sinCosto, "5", "g", "0")))).resumen();

        assertThat(resumen).contains("Costo teórico incompleto").contains("Chile en polvo").doesNotContain("Margen");
    }

    @Test
    @DisplayName("RC-10: sin el permiso de ver costos, el resumen no trae costo ni margen")
    void sinPermisoDeCostos() {
        e.permisos.concedidos = Set.of(PermisosDeRestaurantes.MENU_EDITAR, PermisosDeRestaurantes.RECETA_EDITAR);

        var resumen = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).resumen();

        assertThat(resumen).contains("Milanesa: 120 g").doesNotContain("Costo").doesNotContain("Margen").doesNotContain("43.25");
    }

    @Test
    @DisplayName("RC-03: un articulo con numeros de serie no puede ser insumo")
    void serie() {
        var serie = e.inventario.articulo("Licuadora", "pieza", "900");
        e.inventario.reemplazar(new InventarioDePrueba.Articulo(serie, "Licuadora", "pieza", new BigDecimal("900"), true, Rastreo.SERIE));

        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(serie, "1", "pieza", "0")))), CodigoError.SERIE_EN_RECETA);
    }

    @Test
    @DisplayName("RC-03: un articulo dado de baja en Inventario no puede ser insumo")
    void articuloInactivo() {
        var viejo = e.inventario.articulo("Viejo", "g", "1");
        e.inventario.reemplazar(new InventarioDePrueba.Articulo(viejo, "Viejo", "g", BigDecimal.ONE, false, Rastreo.NINGUNO));

        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(viejo, "1", "g", "0")))), CodigoError.ARTICULO_INACTIVO);
    }

    @Test
    @DisplayName("un articulo que no existe en el negocio y una unidad que no se convierte tienen su propio codigo")
    void articuloOUnidad() {
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(UUID.randomUUID(), "1", "g", "0")))), CodigoError.ARTICULO_NO_ENCONTRADO);
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(milanesa, "1", "ml", "0")))), CodigoError.UNIDAD_NO_CONVERTIBLE);
    }

    @Test
    @DisplayName("si Inventario no responde se avisa con INVENTARIO_NO_DISPONIBLE y no se guarda nada")
    void inventarioNoResponde() {
        var roto = new EntornoDeRecetas();
        var rota = roto.inventario;
        rota.articulos.clear();
        var platillo = roto.menu.crearPlatillo(roto.ctxA, roto.todo, new DatosPlatillo("Taco", null, BigDecimal.TEN, null, Set.of(), null));
        var manejador = new ManejadorGuardarReceta(roto.menuAlmacen, roto.recetasAlmacen, new InventarioNoDisponible(), roto.permisos, roto.reloj);

        assertThatThrownBy(() -> manejador.preparar(roto.ctxA, new GuardarRecetaComando(platillo.id(), 1,
                List.of(insumo(UUID.randomUUID(), "1", "g", "0")))))
                .isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(CodigoError.INVENTARIO_NO_DISPONIBLE);
    }

    @Test
    @DisplayName("RC-02 y RC-04: sin insumos, con un insumo repetido o con un rendimiento fuera de rango la receta no es valida")
    void validaciones() {
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of())), CodigoError.RECETA_INVALIDA);
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(pan, "1", "pieza", "0"), insumo(pan, "2", "pieza", "0")))),
                CodigoError.INSUMO_REPETIDO);
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, new GuardarRecetaComando(cemita.id(), 0, ingredientesDeLaCemita())), CodigoError.RECETA_INVALIDA);
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, new GuardarRecetaComando(cemita.id(), 501, ingredientesDeLaCemita())), CodigoError.RECETA_INVALIDA);
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(List.of(insumo(pan, "0", "pieza", "0")))), CodigoError.RECETA_INVALIDA);
        var masDeTreinta = new ArrayList<InsumoComando>();
        for (int i = 0; i < 31; i++) {
            masDeTreinta.add(insumo(e.inventario.articulo("Insumo " + i, "g", "1"), "1", "g", "0"));
        }
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(masDeTreinta)), CodigoError.RECETA_INVALIDA);
        assertThatThrownBy(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, new GuardarRecetaComando(cemita.id(), 1, null)))
                .isInstanceOf(RestaurantesException.class);
    }

    @Test
    @DisplayName("el platillo debe existir en el negocio y estar activo; el de otro negocio no se puede ni nombrar")
    void platillo() {
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, new GuardarRecetaComando(UUID.randomUUID(), 1, ingredientesDeLaCemita())), CodigoError.PLATILLO_NO_ENCONTRADO);
        falla(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, new GuardarRecetaComando(null, 1, ingredientesDeLaCemita())), CodigoError.PLATILLO_NO_ENCONTRADO);
        falla(() -> e.preparacion.preparar(e.ctxB, Permisos.TODOS, comando(ingredientesDeLaCemita())), CodigoError.PLATILLO_NO_ENCONTRADO);
        var token = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).token();
        var baja = e.preparacion.preparar(e.ctxA, Permisos.TODOS, new DarDeBajaPlatilloComando(cemita.id())).token();
        e.preparacion.confirmar(e.ctxA, Permisos.TODOS, baja);

        falla(() -> e.preparacion.confirmar(e.ctxA, Permisos.TODOS, token), CodigoError.PLATILLO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("RC-08: el permiso de editar recetas se exige al preparar y otra vez al confirmar")
    void permisoEnLosDosPasos() {
        e.permisos.concedidos = Set.of();
        assertThatThrownBy(() -> e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita()))).isInstanceOf(PermisoDenegadoException.class);

        e.permisos.concedidos = Set.of(PermisosDeRestaurantes.RECETA_EDITAR);
        var token = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).token();
        e.permisos.concedidos = Set.of();

        assertThatThrownBy(() -> e.preparacion.confirmar(e.ctxA, Permisos.TODOS, token)).isInstanceOf(PermisoDenegadoException.class);
        assertThat(e.recetasAlmacen.porPlatillo(EntornoDeRecetas.NEGOCIO_A, cemita.id())).isEmpty();
    }

    @Test
    @DisplayName("RC-08: si el precio o un costo cambia entre los dos pasos, confirmar falla con COMANDO_CAMBIO y no guarda")
    void cambioEntreLosPasos() {
        var token = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).token();
        e.inventario.reemplazar(new InventarioDePrueba.Articulo(milanesa, "Milanesa", "g", new BigDecimal("0.20"), true, Rastreo.NINGUNO));

        falla(() -> e.preparacion.confirmar(e.ctxA, Permisos.TODOS, token), CodigoError.COMANDO_CAMBIO);
        assertThat(e.recetasAlmacen.porPlatillo(EntornoDeRecetas.NEGOCIO_A, cemita.id())).isEmpty();
        var otro = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).token();
        e.menu.editarPlatillo(e.ctxA, e.todo, cemita.id(), cemita.version(), new DatosPlatillo("Cemita poblana", null, new BigDecimal("95"), null, Set.of(), null));

        falla(() -> e.preparacion.confirmar(e.ctxA, Permisos.TODOS, otro), CodigoError.COMANDO_CAMBIO);
    }

    @Test
    @DisplayName("el token de una receta es de un solo uso")
    void tokenDeUnSoloUso() {
        var token = e.preparacion.preparar(e.ctxA, Permisos.TODOS, comando(ingredientesDeLaCemita())).token();
        e.preparacion.confirmar(e.ctxA, Permisos.TODOS, token);

        falla(() -> e.preparacion.confirmar(e.ctxA, Permisos.TODOS, token), CodigoError.TOKEN_USADO);
        assertThat(e.recetasAlmacen.porPlatillo(EntornoDeRecetas.NEGOCIO_A, cemita.id()).orElseThrow().versiones()).hasSize(1);
    }
}
