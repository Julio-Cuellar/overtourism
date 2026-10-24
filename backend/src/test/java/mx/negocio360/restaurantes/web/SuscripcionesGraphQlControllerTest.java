package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.aplicacion.AvisoDeComanda;
import mx.negocio360.restaurantes.aplicacion.AvisoDelSalon;
import mx.negocio360.restaurantes.aplicacion.EventosDeRestaurantes;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.LineaDeCuenta;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;
import mx.negocio360.restaurantes.infraestructura.AvisosEnVivo;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 8.4 y SG-16: las suscripciones {@code comandasDeCocina} y {@code salonActualizado}. Solo para quien tiene sesion, negocio
 * activo y el permiso de ver; cada quien recibe unicamente lo de su negocio, y la pausa del QR solo llega a quien puede
 * operar el servicio (QR-17). Los avisos salen de los eventos reales de Restaurantes.
 */
class SuscripcionesGraphQlControllerTest {

    private static final UUID NEGOCIO_A = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final UUID NEGOCIO_B = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");
    private static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    private final AvisosEnVivo avisos = new AvisosEnVivo();
    private final SuscripcionesGraphQlController controlador = new SuscripcionesGraphQlController(avisos);

    private static EntradaGraphQl entrada(UUID negocio, String... permisos) {
        return EntradaGraphQl.de(UUID.randomUUID(), new ContextoNegocio(negocio, "ana"), Autorizacion.de(Set.of(permisos)),
                null);
    }

    private static Cuenta cuenta(UUID negocio) {
        return Cuenta.nueva(UuidV7.generar(AHORA), negocio, 7, UuidV7.generar(AHORA), OrigenDeCuenta.QR, null, null, AHORA);
    }

    private static SobreEvento comandaEnviada(UUID negocio, Estacion estacion) {
        var cuenta = cuenta(negocio);
        var linea = LineaDeCuenta.pendiente(UuidV7.generar(AHORA), UuidV7.generar(AHORA), "Cemita poblana",
                new BigDecimal("85.00"), 2, "sin cebolla", true, estacion, AHORA);
        var comanda = Comanda.nueva(UuidV7.generar(AHORA), negocio, UuidV7.generar(AHORA), cuenta.id(), estacion,
                List.of(linea.id()), "ana", AHORA);
        return EventosDeRestaurantes.comandaEnviada(comanda, cuenta, "Mesa 5", List.of(linea), AHORA);
    }

    private static SobreEvento comandaActualizada(UUID negocio, Estacion estacion) {
        var comanda = Comanda.nueva(UuidV7.generar(AHORA), negocio, UuidV7.generar(AHORA), UuidV7.generar(AHORA), estacion,
                List.of(UuidV7.generar(AHORA)), "ana", AHORA).a(EstadoDeComanda.EN_PREPARACION, "luis", AHORA);
        return EventosDeRestaurantes.comandaActualizada(comanda, List.of(), AHORA);
    }

    private static SobreEvento pedidoQr(UUID negocio) {
        return EventosDeRestaurantes.pedidoQrRecibido(cuenta(negocio), "Mesa 5", 3, true, AHORA);
    }

    private static SobreEvento cuentaPedida(UUID negocio) {
        return EventosDeRestaurantes.cuentaSolicitada(cuenta(negocio), "Mesa 5", "QR", AHORA);
    }

    private static SobreEvento qrPausado(UUID negocio) {
        return EventosDeRestaurantes.qrPausado(negocio, "Mesa 5", AHORA.plusSeconds(3600), AHORA);
    }

    @Test
    @DisplayName("sin sesion no hay suscripcion: NO_AUTENTICADO, para las dos")
    void sinSesion() {
        assertThatThrownBy(() -> controlador.comandasDeCocina(null, EntradaGraphQl.anonima(null)))
                .isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> controlador.salonActualizado(EntradaGraphQl.anonima(null)))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    @DisplayName("SG-04: sin restaurantes.comanda.ver no se abre el tablero, y sin restaurantes.salon.ver no se abre el salon")
    void sinPermiso() {
        assertThatThrownBy(() -> controlador.comandasDeCocina(null, entrada(NEGOCIO_A, PermisosDeRestaurantes.SALON_VER)))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> controlador.salonActualizado(entrada(NEGOCIO_A, PermisosDeRestaurantes.COMANDA_VER)))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("SG-16: la cocina recibe las comandas de su negocio y nunca las de otro")
    void cocina_aislaPorNegocio() {
        var recibidas = new CopyOnWriteArrayList<AvisoDeComanda>();
        controlador.comandasDeCocina(null, entrada(NEGOCIO_A, PermisosDeRestaurantes.COMANDA_VER))
                .subscribe(recibidas::add);

        avisos.alRecibir(comandaEnviada(NEGOCIO_B, Estacion.COCINA));
        avisos.alRecibir(comandaEnviada(NEGOCIO_A, Estacion.COCINA));
        avisos.alRecibir(comandaActualizada(NEGOCIO_B, Estacion.COCINA));
        avisos.alRecibir(comandaActualizada(NEGOCIO_A, Estacion.BARRA));

        assertThat(recibidas).extracting(AvisoDeComanda::tipo).containsExactly(AvisoDeComanda.Tipo.ENVIADA,
                AvisoDeComanda.Tipo.ACTUALIZADA);
        assertThat(recibidas.get(0).mesa()).isEqualTo("Mesa 5");
        assertThat(recibidas.get(0).lineas()).extracting(AvisoDeComanda.Linea::platillo).containsExactly("Cemita poblana");
        assertThat(recibidas.get(1).estado()).isEqualTo(EstadoDeComanda.EN_PREPARACION);
    }

    @Test
    @DisplayName("CD-09: con estacion solo llegan las comandas de esa estacion; sin ella, las dos")
    void cocina_filtraPorEstacion() {
        var barra = new CopyOnWriteArrayList<AvisoDeComanda>();
        var todas = new CopyOnWriteArrayList<AvisoDeComanda>();
        var permiso = entrada(NEGOCIO_A, PermisosDeRestaurantes.COMANDA_VER);
        controlador.comandasDeCocina(Estacion.BARRA, permiso).subscribe(barra::add);
        controlador.comandasDeCocina(null, permiso).subscribe(todas::add);

        avisos.alRecibir(comandaEnviada(NEGOCIO_A, Estacion.COCINA));
        avisos.alRecibir(comandaEnviada(NEGOCIO_A, Estacion.BARRA));

        assertThat(barra).extracting(AvisoDeComanda::estacion).containsExactly(Estacion.BARRA);
        assertThat(todas).extracting(AvisoDeComanda::estacion).containsExactly(Estacion.COCINA, Estacion.BARRA);
    }

    @Test
    @DisplayName("QR-07, QR-13: el salon recibe el pedido por QR, la cuenta pedida y la disponibilidad de su negocio solamente")
    void salon_aislaPorNegocio() {
        var recibidos = new CopyOnWriteArrayList<AvisoDelSalon>();
        controlador.salonActualizado(entrada(NEGOCIO_A, PermisosDeRestaurantes.SALON_VER)).subscribe(recibidos::add);

        avisos.alRecibir(pedidoQr(NEGOCIO_B));
        avisos.alRecibir(pedidoQr(NEGOCIO_A));
        avisos.alRecibir(cuentaPedida(NEGOCIO_B));
        avisos.alRecibir(cuentaPedida(NEGOCIO_A));
        avisos.alRecibir(comandaEnviada(NEGOCIO_A, Estacion.COCINA));

        assertThat(recibidos).extracting(AvisoDelSalon::tipo).containsExactly(AvisoDelSalon.Tipo.PEDIDO_QR,
                AvisoDelSalon.Tipo.CUENTA_SOLICITADA);
        assertThat(recibidos.get(0).lineas()).isEqualTo(3);
        assertThat(recibidos.get(1).solicitadaPor()).isEqualTo("QR");
    }

    @Test
    @DisplayName("QR-17: la pausa del QR solo llega a quien puede operar el servicio, no al mesero que solo ve el salon")
    void salon_pausaSoloParaQuienOpera() {
        var mesero = new CopyOnWriteArrayList<AvisoDelSalon>();
        var gerente = new CopyOnWriteArrayList<AvisoDelSalon>();
        controlador.salonActualizado(entrada(NEGOCIO_A, PermisosDeRestaurantes.SALON_VER)).subscribe(mesero::add);
        controlador.salonActualizado(entrada(NEGOCIO_A, PermisosDeRestaurantes.SALON_VER, PermisosDeRestaurantes.SERVICIO_OPERAR))
                .subscribe(gerente::add);

        avisos.alRecibir(qrPausado(NEGOCIO_A));
        avisos.alRecibir(qrPausado(NEGOCIO_B));

        assertThat(mesero).isEmpty();
        assertThat(gerente).extracting(AvisoDelSalon::tipo).containsExactly(AvisoDelSalon.Tipo.QR_PAUSADO);
        assertThat(gerente.get(0).hasta()).isEqualTo(AHORA.plusSeconds(3600));
    }

    @Test
    @DisplayName("el puente ignora lo que no es de Restaurantes y no falla sin suscriptores ni con un suscriptor que lanza")
    void puente_esRobusto() {
        var ajeno = new SobreEvento(UuidV7.generar(AHORA), "ventas.venta_registrada", 1, NEGOCIO_A, "ventas", AHORA,
                UuidV7.generar(AHORA), null, java.util.Map.of("venta_id", UuidV7.generar(AHORA).toString()));
        avisos.alRecibir(pedidoQr(NEGOCIO_A));
        var sanos = new CopyOnWriteArrayList<AvisoDelSalon>();
        controlador.salonActualizado(entrada(NEGOCIO_A, PermisosDeRestaurantes.SALON_VER)).subscribe(a -> {
            throw new IllegalStateException("socket roto");
        }, e -> { });
        controlador.salonActualizado(entrada(NEGOCIO_A, PermisosDeRestaurantes.SALON_VER)).subscribe(sanos::add);

        avisos.alRecibir(ajeno);
        avisos.alRecibir(pedidoQr(NEGOCIO_A));

        assertThat(sanos).hasSize(1);
    }
}
