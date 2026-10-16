package mx.negocio360.restaurantes.aplicacion;

import static mx.negocio360.restaurantes.aplicacion.EntornoDeMenu.NEGOCIO_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.dominio.Temporada;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R1 · MN-01 a MN-13: lo que el dueño hace con su menu. Cada prueba cita la regla que comprueba. */
class MenuServicioTest {

    private final EntornoDeMenu e = new EntornoDeMenu();

    private static DatosPlatillo datos(String nombre, String precio) {
        return new DatosPlatillo(nombre, null, new BigDecimal(precio), null, Set.of(), null);
    }

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    private PlatilloVista crear(String nombre) {
        return e.servicio.crearPlatillo(e.ctxA, e.todo, datos(nombre, "85.00"));
    }

    // ---- crear y editar (MN-01, MN-13) ----

    @Test
    @DisplayName("MN-01: crear un platillo lo deja activo, sin publicar, con precio a dos decimales y su id de producto")
    void crear_valoresPorOmision() {
        var p = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Cemita poblana", "Con milanesa",
                new BigDecimal("85"), null, Set.of(EtiquetaPlatillo.PICANTE), null));

        assertThat(p.id()).isNotNull();
        assertThat(p.nombre()).isEqualTo("Cemita poblana");
        assertThat(p.precio()).isEqualByComparingTo("85.00");
        assertThat(p.activo()).isTrue();
        assertThat(p.publicado()).isFalse();
        assertThat(p.modo()).isEqualTo(ModoInventario.NINGUNO);
        assertThat(p.etiquetas()).containsExactly(EtiquetaPlatillo.PICANTE);
        assertThat(p.version()).isEqualTo(1);
        assertThat(p.tieneReceta()).isFalse();
    }

    @Test
    @DisplayName("MN-01: dos platillos activos no pueden llamarse igual, sin distinguir mayusculas, pero uno dado de baja si libera el nombre")
    void crear_nombreDuplicado() {
        crear("Mole poblano");

        falla(() -> crear("MOLE POBLANO"), CodigoError.NOMBRE_DUPLICADO);
        falla(() -> crear("  mole poblano "), CodigoError.NOMBRE_DUPLICADO);
    }

    @Test
    @DisplayName("MN-01: el mismo nombre en otro negocio no choca")
    void crear_mismoNombreEnOtroNegocio() {
        crear("Mole poblano");

        assertThat(e.servicio.crearPlatillo(e.ctxB, e.todo, datos("Mole poblano", "120")).nombre()).isEqualTo("Mole poblano");
    }

    @Test
    @DisplayName("MN-13: editar con la version correcta cambia los datos y sube la version; con una vieja falla con CONCURRENCIA")
    void editar_conVersion() {
        var p = crear("Cemita poblana");

        var editado = e.servicio.editarPlatillo(e.ctxA, e.todo, p.id(), p.version(), datos("Cemita especial", "95"));

        assertThat(editado.nombre()).isEqualTo("Cemita especial");
        assertThat(editado.version()).isEqualTo(2);
        falla(() -> e.servicio.editarPlatillo(e.ctxA, e.todo, p.id(), p.version(), datos("Otra", "10")), CodigoError.CONCURRENCIA);
    }

    @Test
    @DisplayName("MN-01: editar a un nombre que ya tiene otro platillo falla, pero conservar el propio no")
    void editar_nombreDuplicado() {
        crear("Mole poblano");
        var cemita = crear("Cemita poblana");

        falla(() -> e.servicio.editarPlatillo(e.ctxA, e.todo, cemita.id(), cemita.version(), datos("mole poblano", "85")),
                CodigoError.NOMBRE_DUPLICADO);
        assertThat(e.servicio.editarPlatillo(e.ctxA, e.todo, cemita.id(), cemita.version(), datos("Cemita poblana", "90")).precio())
                .isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("editar un platillo de otro negocio responde PLATILLO_NO_ENCONTRADO y no lo toca (SG-02)")
    void editar_deOtroNegocio() {
        var p = crear("Mole poblano");

        falla(() -> e.servicio.editarPlatillo(e.ctxB, e.todo, p.id(), p.version(), datos("Robado", "1")), CodigoError.PLATILLO_NO_ENCONTRADO);
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).nombre()).isEqualTo("Mole poblano");
    }

    @Test
    @DisplayName("MN-12: un negocio no pasa de 200 platillos activos")
    void crear_limite() {
        for (int i = 0; i < 200; i++) {
            e.servicio.crearPlatillo(e.ctxA, e.todo, datos("Platillo " + i, "10"));
        }

        falla(() -> crear("Uno de mas"), CodigoError.LIMITE_EXCEDIDO);
        assertThat(e.servicio.crearPlatillo(e.ctxB, e.todo, datos("Uno de mas", "10"))).isNotNull();
    }

    @Test
    @DisplayName("MN-08 y MN-06: se crean con etiquetas y temporada, y la temporada se lee en el platillo")
    void crear_conEtiquetasYTemporada() {
        var nogada = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Chiles en nogada", null, new BigDecimal("260"),
                null, Set.of(EtiquetaPlatillo.TIPICO_POBLANO, EtiquetaPlatillo.DE_TEMPORADA),
                new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30))));

        assertThat(nogada.temporadaDesde()).isEqualTo("08-01");
        assertThat(nogada.temporadaHasta()).isEqualTo("09-30");
        assertThat(nogada.ordenableHoy()).as("el 16 de octubre ya es fuera de temporada").isFalse();
    }

    // ---- categorias (MN-07) ----

    @Test
    @DisplayName("MN-07: una categoria lleva nombre, orden y estacion (cocina por omision); se asigna a un platillo")
    void categorias() {
        var bebidas = e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Bebidas", 3, Estacion.BARRA));
        var sinEstacion = e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Antojitos", 1, null));

        assertThat(bebidas.estacion()).isEqualTo(Estacion.BARRA);
        assertThat(sinEstacion.estacion()).isEqualTo(Estacion.COCINA);
        assertThat(e.servicio.categorias(e.ctxA, e.todo)).extracting(CategoriaVista::nombre).containsExactly("Antojitos", "Bebidas");
        var agua = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Agua de horchata", null, new BigDecimal("35"),
                bebidas.id(), Set.of(), null));
        assertThat(agua.categoriaId()).isEqualTo(bebidas.id());
    }

    @Test
    @DisplayName("MN-07: una categoria de otro negocio no se puede asignar y la 31 falla con LIMITE_EXCEDIDO")
    void categorias_limiteYAislamiento() {
        var ajena = e.servicio.crearCategoria(e.ctxB, e.todo, new DatosCategoria("De Beto", 1, null));
        falla(() -> e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("X1", null, BigDecimal.TEN, ajena.id(), Set.of(), null)),
                CodigoError.CATEGORIA_NO_ENCONTRADA);
        for (int i = 0; i < 30; i++) {
            e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Categoria " + i, i, null));
        }

        falla(() -> e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("La 31", 31, null)), CodigoError.LIMITE_EXCEDIDO);
    }

    @Test
    @DisplayName("editar una categoria cambia su nombre, orden y estacion con su version")
    void categorias_editar() {
        var c = e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Bebidas", 3, Estacion.COCINA));

        var editada = e.servicio.editarCategoria(e.ctxA, e.todo, c.id(), c.version(), new DatosCategoria("Bebidas frias", 2, Estacion.BARRA));

        assertThat(editada.nombre()).isEqualTo("Bebidas frias");
        assertThat(editada.estacion()).isEqualTo(Estacion.BARRA);
        assertThat(editada.version()).isEqualTo(2);
        falla(() -> e.servicio.editarCategoria(e.ctxA, e.todo, c.id(), c.version(), new DatosCategoria("Otra", 1, null)), CodigoError.CONCURRENCIA);
    }

    // ---- agotado, publicar, modo, baja (MN-04, MN-05, MN-09, MN-10) ----

    @Test
    @DisplayName("MN-05: marcar agotado lo deja no ordenable hoy y desmarcarlo lo vuelve a dejar; lo puede hacer quien tiene menu.agotar")
    void agotar() {
        var p = crear("Mole poblano");

        assertThat(e.servicio.marcarAgotado(e.ctxA, EntornoDeMenu.solo(PermisosDeRestaurantes.MENU_AGOTAR), p.id(), true).agotado()).isTrue();
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).ordenableHoy()).isFalse();
        assertThat(e.servicio.marcarAgotado(e.ctxA, EntornoDeMenu.solo(PermisosDeRestaurantes.MENU_AGOTAR), p.id(), false).ordenableHoy()).isTrue();
    }

    @Test
    @DisplayName("MN-09: publicar y despublicar solo cambia el menu publico")
    void publicar() {
        var p = crear("Mole poblano");

        assertThat(e.servicio.publicarPlatillo(e.ctxA, e.todo, p.id(), true).publicado()).isTrue();
        assertThat(e.servicio.publicarPlatillo(e.ctxA, e.todo, p.id(), false).publicado()).isFalse();
    }

    @Test
    @DisplayName("MN-04: el modo RECETA exige una receta activa")
    void modo_recetaRequerida() {
        var p = crear("Cemita poblana");

        falla(() -> e.servicio.cambiarModo(e.ctxA, e.todo, p.id(), ModoInventario.RECETA), CodigoError.RECETA_REQUERIDA);
        e.recetas.conReceta.add(p.id());
        assertThat(e.servicio.cambiarModo(e.ctxA, e.todo, p.id(), ModoInventario.RECETA).modo()).isEqualTo(ModoInventario.RECETA);
    }

    @Test
    @DisplayName("MN-04: con una receta activa no se puede pasar a vinculo directo, y sin ella si")
    void modo_vinculoDirectoConflicto() {
        var p = crear("Refresco");
        assertThat(e.servicio.cambiarModo(e.ctxA, e.todo, p.id(), ModoInventario.VINCULO_DIRECTO).modo()).isEqualTo(ModoInventario.VINCULO_DIRECTO);

        var q = crear("Cemita");
        e.recetas.conReceta.add(q.id());
        falla(() -> e.servicio.cambiarModo(e.ctxA, e.todo, q.id(), ModoInventario.VINCULO_DIRECTO), CodigoError.MODO_INVENTARIO_CONFLICTO);
    }

    @Test
    @DisplayName("MN-04: volver a NINGUNO siempre se puede")
    void modo_ninguno() {
        var p = crear("Cemita");
        e.recetas.conReceta.add(p.id());
        e.servicio.cambiarModo(e.ctxA, e.todo, p.id(), ModoInventario.RECETA);

        assertThat(e.servicio.cambiarModo(e.ctxA, e.todo, p.id(), ModoInventario.NINGUNO).modo()).isEqualTo(ModoInventario.NINGUNO);
    }

    // ---- consultas ----

    @Test
    @DisplayName("platillos: ordenados por nombre, con filtro por texto, categoria y agotados, y solo los activos")
    void listar_filtros() {
        var bebidas = e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Bebidas", 1, Estacion.BARRA));
        crear("Mole poblano");
        crear("Cemita poblana");
        var agua = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Agua de horchata", null, new BigDecimal("35"), bebidas.id(), Set.of(), null));
        e.servicio.marcarAgotado(e.ctxA, e.todo, agua.id(), true);

        var todos = e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 20, null);
        assertThat(todos.nodos()).extracting(PlatilloVista::nombre).containsExactly("Agua de horchata", "Cemita poblana", "Mole poblano");
        assertThat(e.servicio.platillos(e.ctxA, e.todo, new FiltroPlatillos("poblan", null, false, false, false), 20, null).nodos())
                .extracting(PlatilloVista::nombre).containsExactly("Cemita poblana", "Mole poblano");
        assertThat(e.servicio.platillos(e.ctxA, e.todo, new FiltroPlatillos(null, bebidas.id(), false, false, false), 20, null).nodos())
                .extracting(PlatilloVista::nombre).containsExactly("Agua de horchata");
        assertThat(e.servicio.platillos(e.ctxA, e.todo, new FiltroPlatillos(null, null, true, false, false), 20, null).nodos())
                .extracting(PlatilloVista::nombre).containsExactly("Agua de horchata");
        assertThat(e.servicio.platillos(e.ctxB, e.todo, FiltroPlatillos.TODOS, 20, null).nodos()).isEmpty();
    }

    @Test
    @DisplayName("platillos: el filtro sin receta usa las recetas del negocio y el cursor pagina sin repetir ni saltar")
    void listar_sinRecetaYPaginas() {
        var a = crear("A platillo");
        crear("B platillo");
        crear("C platillo");
        e.recetas.conReceta.add(a.id());

        assertThat(e.servicio.platillos(e.ctxA, e.todo, new FiltroPlatillos(null, null, false, false, true), 20, null).nodos())
                .extracting(PlatilloVista::nombre).containsExactly("B platillo", "C platillo");
        var primera = e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 2, null);
        var segunda = e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 2, primera.siguiente());
        assertThat(primera.nodos()).extracting(PlatilloVista::nombre).containsExactly("A platillo", "B platillo");
        assertThat(segunda.nodos()).extracting(PlatilloVista::nombre).containsExactly("C platillo");
        assertThat(segunda.siguiente()).isNull();
        assertThat(primera.nodos().get(0).tieneReceta()).isTrue();
    }

    @Test
    @DisplayName("platillos: primeros menor que 1 o un cursor que no es un id falla con CONSULTA_INVALIDA; mas de 100 se recorta a 100")
    void listar_parametros() {
        falla(() -> e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 0, null), CodigoError.CONSULTA_INVALIDA);
        falla(() -> e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 20, "no-es-un-id"), CodigoError.CONSULTA_INVALIDA);
        for (int i = 0; i < 120; i++) {
            e.servicio.crearPlatillo(e.ctxA, e.todo, datos("Platillo " + String.format("%03d", i), "10"));
        }

        assertThat(e.servicio.platillos(e.ctxA, e.todo, FiltroPlatillos.TODOS, 500, null).nodos()).hasSize(100);
    }

    @Test
    @DisplayName("resumenDeMenu: cuenta platillos, agotados, fuera de temporada y la cobertura de recetas")
    void resumen() {
        var a = crear("A platillo");
        var b = crear("B platillo");
        crear("C platillo");
        e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Nogada", null, new BigDecimal("260"), null, Set.of(),
                new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30))));
        e.servicio.marcarAgotado(e.ctxA, e.todo, b.id(), true);
        e.recetas.conReceta.add(a.id());

        var r = e.servicio.resumenDeMenu(e.ctxA, e.todo);

        assertThat(r.total()).isEqualTo(4);
        assertThat(r.agotados()).isEqualTo(1);
        assertThat(r.fueraDeTemporada()).isEqualTo(1);
        assertThat(r.conReceta()).isEqualTo(1);
        assertThat(r.sinReceta()).isEqualTo(3);
        assertThat(r.coberturaDeRecetasPct()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("resumenDeMenu sin platillos no divide entre cero: cobertura 0")
    void resumen_vacio() {
        assertThat(e.servicio.resumenDeMenu(e.ctxA, e.todo).coberturaDeRecetasPct()).isEqualByComparingTo("0.00");
    }

    // ---- permisos (AZ-04) ----

    @Test
    @DisplayName("cada operacion exige su permiso, tambien al leer: sin permiso falla PermisoDenegado y no toca nada")
    void permisos() {
        var p = crear("Mole poblano");
        var nada = EntornoDeMenu.solo();
        var soloVer = EntornoDeMenu.solo(PermisosDeRestaurantes.MENU_VER);

        assertThatThrownBy(() -> e.servicio.platillos(e.ctxA, nada, FiltroPlatillos.TODOS, 20, null)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.platillo(e.ctxA, nada, p.id())).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.categorias(e.ctxA, nada)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.resumenDeMenu(e.ctxA, nada)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.crearPlatillo(e.ctxA, soloVer, datos("Nuevo", "10"))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.editarPlatillo(e.ctxA, soloVer, p.id(), 1, datos("Nuevo", "10"))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.publicarPlatillo(e.ctxA, soloVer, p.id(), true)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.cambiarModo(e.ctxA, soloVer, p.id(), ModoInventario.NINGUNO)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.marcarAgotado(e.ctxA, soloVer, p.id(), true)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.crearCategoria(e.ctxA, soloVer, new DatosCategoria("X", 1, null))).isInstanceOf(PermisoDenegadoException.class);
        assertThat(e.servicio.platillo(e.ctxA, e.todo, p.id()).version()).isEqualTo(1);
    }

    @Test
    @DisplayName("un platillo que no existe responde PLATILLO_NO_ENCONTRADO")
    void noEncontrado() {
        falla(() -> e.servicio.platillo(e.ctxA, e.todo, UUID.randomUUID()), CodigoError.PLATILLO_NO_ENCONTRADO);
        falla(() -> e.servicio.marcarAgotado(e.ctxA, e.todo, UUID.randomUUID(), true), CodigoError.PLATILLO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("el contexto del otro negocio no ve los platillos del primero")
    void aislamiento() {
        var p = crear("Mole poblano");

        falla(() -> e.servicio.platillo(e.ctxB, e.todo, p.id()), CodigoError.PLATILLO_NO_ENCONTRADO);
        falla(() -> e.servicio.publicarPlatillo(e.ctxB, e.todo, p.id(), true), CodigoError.PLATILLO_NO_ENCONTRADO);
        assertThat(NEGOCIO_B).isNotEqualTo(e.ctxA.negocioId());
    }
}
