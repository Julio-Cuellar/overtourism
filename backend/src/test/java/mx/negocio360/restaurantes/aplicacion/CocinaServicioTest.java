package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R5 · CD-01 a CD-09: el tablero de comandas de cocina y barra. Cada prueba cita su regla. */
class CocinaServicioTest {

    private final EntornoDeCuentas e = new EntornoDeCuentas().armado();

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    /** Una cuenta con una cemita (cocina) y una horchata (barra), ya enviada: dos comandas NUEVAS. */
    private java.util.List<ComandaVista> enviar(java.util.UUID mesa) {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, mesa, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, "sin cebolla", true);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false);
        return e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
    }

    private ComandaVista de(java.util.List<ComandaVista> comandas, Estacion estacion) {
        return comandas.stream().filter(c -> c.estacion() == estacion).findFirst().orElseThrow();
    }

    // ---- CD-01: estados ----

    @Test
    @DisplayName("CD-01: NUEVA, EN_PREPARACION, LISTA y ENTREGADA, de uno en uno; saltarse uno falla con TRANSICION_INVALIDA")
    void estados() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);
        assertThat(cocina.estado()).isEqualTo(EstadoDeComanda.NUEVA);

        var enPreparacion = e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        var lista = e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        var entregada = e.cocina.avanzarComanda(e.ctxA, e.mesero, cocina.id());

        assertThat(enPreparacion.estado()).isEqualTo(EstadoDeComanda.EN_PREPARACION);
        assertThat(lista.estado()).isEqualTo(EstadoDeComanda.LISTA);
        assertThat(entregada.estado()).isEqualTo(EstadoDeComanda.ENTREGADA);
        falla(() -> e.cocina.avanzarComanda(e.ctxA, e.dueno, cocina.id()), CodigoError.TRANSICION_INVALIDA);
    }

    @Test
    @DisplayName("CD-01: solo se permite devolver LISTA a EN_PREPARACION")
    void devolver() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);
        falla(() -> e.cocina.devolverComanda(e.ctxA, e.cocinero, cocina.id()), CodigoError.TRANSICION_INVALIDA);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        falla(() -> e.cocina.devolverComanda(e.ctxA, e.cocinero, cocina.id()), CodigoError.TRANSICION_INVALIDA);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        var devuelta = e.cocina.devolverComanda(e.ctxA, e.cocinero, cocina.id());

        assertThat(devuelta.estado()).isEqualTo(EstadoDeComanda.EN_PREPARACION);
        assertThat(devuelta.listaEn()).isNull();
    }

    @Test
    @DisplayName("CD-01: cada cambio publica restaurantes.comanda_actualizada con el estado nuevo")
    void publica() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);
        e.eventos.clear();

        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        var avisos = e.eventosDe("restaurantes.comanda_actualizada");
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).datos()).containsEntry("estado", "EN_PREPARACION").containsEntry("estacion", "COCINA")
                .containsEntry("comanda_id", cocina.id().toString()).containsEntry("envio_id", cocina.envioId().toString());
    }

    // ---- CD-02: permisos ----

    @Test
    @DisplayName("CD-02: el mesero no puede marcar LISTA; el cocinero no puede marcar ENTREGADA")
    void permisosPorPaso() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);

        assertThatThrownBy(() -> e.cocina.avanzarComanda(e.ctxA, e.mesero, cocina.id()))
                .isInstanceOf(PermisoDenegadoException.class);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        assertThatThrownBy(() -> e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id()))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThat(e.cocina.avanzarComanda(e.ctxA, e.mesero, cocina.id()).estado()).isEqualTo(EstadoDeComanda.ENTREGADA);
    }

    @Test
    @DisplayName("CD-02: ver el tablero exige restaurantes.comanda.ver")
    void verExigePermiso() {
        assertThatThrownBy(() -> e.cocina.comandas(e.ctxA, EntornoDeSalon.solo("restaurantes.menu.ver"), null, null, 20))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- CD-03: estaciones con estado propio ----

    @Test
    @DisplayName("CD-03: una cemita lista en cocina no cambia el estado de la comanda de bebidas")
    void estacionesIndependientes() {
        var comandas = enviar(e.mesa1);
        var cocina = de(comandas, Estacion.COCINA);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        var tablero = e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50);

        assertThat(de(tablero, Estacion.COCINA).estado()).isEqualTo(EstadoDeComanda.LISTA);
        assertThat(de(tablero, Estacion.BARRA).estado()).isEqualTo(EstadoDeComanda.NUEVA);
    }

    @Test
    @DisplayName("CD-03: el tablero de una estacion muestra sus comandas no entregadas, de la mas antigua a la mas nueva")
    void tablero() {
        var primera = enviar(e.mesa1);
        e.reloj.avanzar(Duration.ofMinutes(3));
        var segunda = enviar(e.mesa2);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, de(primera, Estacion.BARRA).id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, de(primera, Estacion.BARRA).id());
        e.cocina.avanzarComanda(e.ctxA, e.mesero, de(primera, Estacion.BARRA).id());

        var barra = e.cocina.comandas(e.ctxA, e.cocinero, Estacion.BARRA, null, 50);
        var cocina = e.cocina.comandas(e.ctxA, e.cocinero, Estacion.COCINA, null, 50);

        assertThat(barra).extracting(ComandaVista::id).containsExactly(de(segunda, Estacion.BARRA).id());
        assertThat(cocina).extracting(ComandaVista::id).containsExactly(de(primera, Estacion.COCINA).id(),
                de(segunda, Estacion.COCINA).id());
    }

    @Test
    @DisplayName("CD-03: se puede filtrar por estados, incluida ENTREGADA")
    void filtroPorEstados() {
        var comandas = enviar(e.mesa1);
        var barra = de(comandas, Estacion.BARRA);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, barra.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, barra.id());
        e.cocina.avanzarComanda(e.ctxA, e.mesero, barra.id());

        var listas = e.cocina.comandas(e.ctxA, e.cocinero, null, Set.of(EstadoDeComanda.ENTREGADA), 50);

        assertThat(listas).extracting(ComandaVista::id).containsExactly(barra.id());
    }

    @Test
    @DisplayName("CD-03: primeros menor que 1 es invalido y mas de 100 se recorta")
    void paginacion() {
        falla(() -> e.cocina.comandas(e.ctxA, e.cocinero, null, null, 0), CodigoError.CONSULTA_INVALIDA);
        assertThat(e.cocina.comandas(e.ctxA, e.cocinero, null, null, 5000)).isEmpty();
    }

    // ---- CD-04: tiempos ----

    @Test
    @DisplayName("CD-04: el tiempo de preparacion es LISTA menos enviada, calculado por el codigo")
    void tiempoDePreparacion() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.reloj.avanzar(Duration.ofMinutes(9));

        var lista = e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        assertThat(lista.segundosDePreparacion()).isEqualTo(540L);
        assertThat(lista.retrasada()).isFalse();
    }

    @Test
    @DisplayName("CD-04: se resalta como retrasada una comanda que lleva mas de 15 minutos sin estar lista")
    void retrasada() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);
        assertThat(e.cocina.comandas(e.ctxA, e.cocinero, Estacion.COCINA, null, 10).get(0).retrasada()).isFalse();

        e.reloj.avanzar(Duration.ofMinutes(16));

        var tablero = e.cocina.comandas(e.ctxA, e.cocinero, Estacion.COCINA, null, 10);
        assertThat(tablero.get(0).id()).isEqualTo(cocina.id());
        assertThat(tablero.get(0).retrasada()).isTrue();
    }

    // ---- CD-05, CD-06, CD-08: lineas en el tablero ----

    @Test
    @DisplayName("CD-06: las lineas con marca de alergia llegan marcadas, con su nota y sin detalle medico")
    void alergia() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);

        var linea = cocina.lineas().get(0);

        assertThat(linea.platillo()).isEqualTo("Cemita poblana");
        assertThat(linea.cantidad()).isEqualTo(2);
        assertThat(linea.marcaAlergia()).isTrue();
        assertThat(linea.nota()).isEqualTo("sin cebolla");
    }

    @Test
    @DisplayName("CD-08: las comandas de un mismo envio comparten envio_id y llevan la mesa y el numero de cuenta")
    void envioCompartido() {
        var comandas = enviar(e.mesa1);

        assertThat(comandas).extracting(ComandaVista::envioId).containsOnly(comandas.get(0).envioId());
        assertThat(comandas).extracting(ComandaVista::mesa).containsOnly("Mesa 1");
        assertThat(comandas).extracting(ComandaVista::numeroDeCuenta).containsOnly(1);
    }

    @Test
    @DisplayName("CD-05: una linea anulada se ve tachada en el tablero y la comanda sigue su camino")
    void tachada() {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 3, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var segunda = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(1).id();

        e.servicio.anularLinea(e.ctxA, e.mesero, segunda, mx.negocio360.restaurantes.dominio.MotivoDeAnulacion.ERROR_DEL_PEDIDO,
                null);

        var comanda = e.cocina.comandas(e.ctxA, e.cocinero, Estacion.COCINA, null, 10).get(0);
        assertThat(comanda.lineas()).extracting(LineaDeComandaVista::anulada).containsExactly(false, true);
        assertThat(comanda.estado()).isEqualTo(EstadoDeComanda.NUEVA);
    }

    // ---- CD-07 y CD-09 ----

    @Test
    @DisplayName("CD-07: un negocio no ve ni mueve las comandas de otro")
    void aislamiento() {
        var cocina = de(enviar(e.mesa1), Estacion.COCINA);

        assertThat(e.cocina.comandas(e.ctxB, e.dueno, null, null, 50)).isEmpty();
        falla(() -> e.cocina.avanzarComanda(e.ctxB, e.dueno, cocina.id()), CodigoError.COMANDA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("CD-09: quien trabaja en barra usa el rol cocinero y ve las dos estaciones; el filtro es de pantalla")
    void cocineroVeLasDos() {
        enviar(e.mesa1);

        var todas = e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50);

        assertThat(todas).extracting(ComandaVista::estacion).containsExactlyInAnyOrder(Estacion.COCINA, Estacion.BARRA);
    }
}
