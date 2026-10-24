package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.stream.IntStream;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.EstadoDeMesa;
import mx.negocio360.restaurantes.dominio.ModoServicio;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R4 · MS-01 a MS-09 y QR-01, QR-02: el salon, el servicio, la disponibilidad y el QR de cada mesa. Cada prueba cita su regla. */
class SalonServicioTest {

    private final EntornoDeSalon e = new EntornoDeSalon();

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    // ---- MS-01: limites y datos de zonas y mesas ----

    @Test
    @DisplayName("MS-01: una zona guarda su nombre y su orden y nace activa")
    void zona_nace() {
        var z = e.servicio.crearZona(e.ctxA, e.todo, "Terraza", 2);

        assertThat(z.id()).isNotNull();
        assertThat(z.nombre()).isEqualTo("Terraza");
        assertThat(z.orden()).isEqualTo(2);
        assertThat(z.activa()).isTrue();
    }

    @Test
    @DisplayName("MS-01: la zona numero 11 falla con LIMITE_EXCEDIDO")
    void zona_limite() {
        IntStream.rangeClosed(1, 10).forEach(i -> e.zona("Zona " + i));

        falla(() -> e.zona("Zona 11"), CodigoError.LIMITE_EXCEDIDO);
    }

    @Test
    @DisplayName("MS-01: el nombre de la zona debe tener entre 1 y 40 caracteres")
    void zona_nombreInvalido() {
        falla(() -> e.zona("  "), CodigoError.NOMBRE_INVALIDO);
        falla(() -> e.zona("x".repeat(41)), CodigoError.NOMBRE_INVALIDO);
    }

    @Test
    @DisplayName("MS-01: la mesa 61 falla con LIMITE_EXCEDIDO")
    void mesa_limite() {
        var zona = e.zona("Salon");
        IntStream.rangeClosed(1, 60).forEach(i -> e.mesa(zona, "M" + i, 4));

        falla(() -> e.mesa(zona, "M61", 4), CodigoError.LIMITE_EXCEDIDO);
    }

    @Test
    @DisplayName("MS-01: la capacidad va de 1 a 20")
    void mesa_capacidad() {
        var zona = e.zona("Salon");

        falla(() -> e.mesa(zona, "M1", 0), CodigoError.CAPACIDAD_INVALIDA);
        falla(() -> e.mesa(zona, "M1", 21), CodigoError.CAPACIDAD_INVALIDA);
        assertThat(e.servicio.crearMesa(e.ctxA, e.todo, zona, "M1", 20).capacidad()).isEqualTo(20);
    }

    @Test
    @DisplayName("MS-01: la etiqueta de la mesa es unica en el negocio, sin distinguir mayusculas ni espacios")
    void mesa_etiquetaUnica() {
        var zona = e.zona("Salon");
        e.mesa(zona, "Mesa 1", 4);

        falla(() -> e.mesa(zona, "  MESA 1 ", 4), CodigoError.MESA_DUPLICADA);
    }

    @Test
    @DisplayName("MS-01: la misma etiqueta en otro negocio no choca")
    void mesa_mismaEtiquetaOtroNegocio() {
        var zonaA = e.zona("Salon");
        e.mesa(zonaA, "Mesa 1", 4);
        var zonaB = e.servicio.crearZona(e.ctxB, e.todo, "Salon", 0).id();

        assertThat(e.servicio.crearMesa(e.ctxB, e.todo, zonaB, "Mesa 1", 4).etiqueta()).isEqualTo("Mesa 1");
    }

    @Test
    @DisplayName("MS-01: la zona de una mesa debe existir en el negocio; la de otro negocio no cuenta")
    void mesa_zonaDeOtroNegocio() {
        var zonaB = e.servicio.crearZona(e.ctxB, e.todo, "Salon", 0).id();

        falla(() -> e.mesa(zonaB, "Mesa 1", 4), CodigoError.ZONA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("MS-01: crear zonas y mesas exige restaurantes.salon.configurar")
    void configurar_exigePermiso() {
        assertThatThrownBy(() -> e.servicio.crearZona(e.ctxA, e.operador, "Salon", 0))
                .isInstanceOf(PermisoDenegadoException.class);
        var zona = e.zona("Salon");
        assertThatThrownBy(() -> e.servicio.crearMesa(e.ctxA, e.operador, zona, "M1", 4))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("MS-01: editar la mesa con la version vista; con otra, CONCURRENCIA")
    void mesa_editar() {
        var zona = e.zona("Salon");
        var mesa = e.servicio.crearMesa(e.ctxA, e.todo, zona, "M1", 4);

        var editada = e.servicio.editarMesa(e.ctxA, e.todo, mesa.id(), mesa.version(), zona, "Mesa uno", 6);

        assertThat(editada.etiqueta()).isEqualTo("Mesa uno");
        assertThat(editada.capacidad()).isEqualTo(6);
        assertThat(editada.version()).isEqualTo(mesa.version() + 1);
        falla(() -> e.servicio.editarMesa(e.ctxA, e.todo, mesa.id(), mesa.version(), zona, "Otra", 2),
                CodigoError.CONCURRENCIA);
    }

    // ---- MS-02: estado derivado ----

    @Test
    @DisplayName("MS-02: la mesa sin cuenta esta LIBRE, con cuenta no pagada OCUPADA y con marca manual BLOQUEADA")
    void estadoDerivado() {
        var zona = e.zona("Salon");
        var libre = e.mesa(zona, "M1", 4);
        var ocupada = e.mesa(zona, "M2", 4);
        var bloqueada = e.mesa(zona, "M3", 4);
        e.ocupacion.ocupar(ocupada);
        e.servicio.bloquearMesa(e.ctxA, e.todo, bloqueada, true);

        var salon = e.servicio.salon(e.ctxA, e.mesero);

        assertThat(salon.mesas()).extracting(MesaVista::id, MesaVista::estado).containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(libre, EstadoDeMesa.LIBRE),
                org.assertj.core.groups.Tuple.tuple(ocupada, EstadoDeMesa.OCUPADA),
                org.assertj.core.groups.Tuple.tuple(bloqueada, EstadoDeMesa.BLOQUEADA));
    }

    @Test
    @DisplayName("MS-02: pagar la cuenta cambia el estado sin otra escritura")
    void estadoSeDeriva() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        e.ocupacion.ocupar(mesa);
        assertThat(e.servicio.salon(e.ctxA, e.mesero).mesas().get(0).estado()).isEqualTo(EstadoDeMesa.OCUPADA);

        e.ocupacion.liberar(mesa);

        assertThat(e.servicio.salon(e.ctxA, e.mesero).mesas().get(0).estado()).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    @DisplayName("MS-02: el salon muestra la cuenta resumida de la mesa ocupada")
    void salon_conCuenta() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        e.ocupacion.ocupar(mesa);

        var vista = e.servicio.salon(e.ctxA, e.mesero).mesas().get(0);

        assertThat(vista.cuenta()).isNotNull();
        assertThat(vista.cuenta().estado()).isEqualTo("ABIERTA");
    }

    @Test
    @DisplayName("MS-02: ver el salon exige restaurantes.salon.ver")
    void salon_exigePermiso() {
        assertThatThrownBy(() -> e.servicio.salon(e.ctxA, EntornoDeSalon.solo(PermisosDeRestaurantes.MENU_VER)))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("MS-02: un negocio no ve las mesas de otro")
    void salon_aislado() {
        var zona = e.zona("Salon");
        e.mesa(zona, "M1", 4);

        assertThat(e.servicio.salon(e.ctxB, e.todo).mesas()).isEmpty();
    }

    // ---- MS-04: disponibilidad ----

    @Test
    @DisplayName("MS-04: libres, total y abierto; bloquear una mesa libre baja libres y total en 1")
    void disponibilidad() {
        var zona = e.zona("Salon");
        var m1 = e.mesa(zona, "M1", 4);
        e.mesa(zona, "M2", 4);
        e.mesa(zona, "M3", 4);
        e.servicio.abrirServicio(e.ctxA, e.todo);
        e.ocupacion.ocupar(m1);

        var antes = e.servicio.salon(e.ctxA, e.mesero).disponibilidad();
        e.servicio.bloquearMesa(e.ctxA, e.todo, e.servicio.salon(e.ctxA, e.mesero).mesas().stream()
                .filter(m -> m.etiqueta().equals("M3")).findFirst().orElseThrow().id(), true);
        var despues = e.servicio.salon(e.ctxA, e.mesero).disponibilidad();

        assertThat(antes).isEqualTo(new DisponibilidadVista(2, 3, true));
        assertThat(despues).isEqualTo(new DisponibilidadVista(1, 2, true));
    }

    // ---- MS-05: servicio ----

    @Test
    @DisplayName("MS-05: el servicio nace cerrado; abrirlo y cerrarlo son manuales")
    void servicio_abrirCerrar() {
        assertThat(e.servicio.salon(e.ctxA, e.mesero).servicioAbierto()).isFalse();

        e.servicio.abrirServicio(e.ctxA, e.operador);
        assertThat(e.servicio.salon(e.ctxA, e.mesero).servicioAbierto()).isTrue();

        e.servicio.cerrarServicio(e.ctxA, e.operador, null);
        assertThat(e.servicio.salon(e.ctxA, e.mesero).servicioAbierto()).isFalse();
    }

    @Test
    @DisplayName("MS-05: abrir y cerrar el servicio exigen restaurantes.servicio.operar")
    void servicio_exigePermiso() {
        assertThatThrownBy(() -> e.servicio.abrirServicio(e.ctxA, e.mesero)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.cerrarServicio(e.ctxA, e.mesero, null))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("MS-05: cerrar con cuentas abiertas exige permiso de gerente y motivo")
    void servicio_cerrarConCuentas() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        e.servicio.abrirServicio(e.ctxA, e.todo);
        e.ocupacion.ocupar(mesa);

        assertThatThrownBy(() -> e.servicio.cerrarServicio(e.ctxA, e.operador, "Se fue la luz"))
                .isInstanceOf(PermisoDenegadoException.class);
        falla(() -> e.servicio.cerrarServicio(e.ctxA, e.todo, "  "), CodigoError.MOTIVO_REQUERIDO);
        falla(() -> e.servicio.cerrarServicio(e.ctxA, e.todo, null), CodigoError.MOTIVO_REQUERIDO);

        e.servicio.cerrarServicio(e.ctxA, e.todo, "Se fue la luz");
        assertThat(e.servicio.salon(e.ctxA, e.mesero).servicioAbierto()).isFalse();
    }

    // ---- MS-06: evento de disponibilidad solo cuando cambia ----

    @Test
    @DisplayName("MS-06: cada cambio de (libres, total, abierto) publica el estado completo; repetir lo mismo no publica nada")
    void evento_soloSiCambia() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        var tras = e.eventosDe("negocio.disponibilidad_cambiada").size();
        assertThat(tras).isEqualTo(1);

        e.servicio.bloquearMesa(e.ctxA, e.todo, mesa, true);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(2);

        e.servicio.bloquearMesa(e.ctxA, e.todo, mesa, true);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(2);

        e.servicio.abrirServicio(e.ctxA, e.todo);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(3);
    }

    @Test
    @DisplayName("MS-06: el evento trae tipo MESAS, libres, total, abierto y la hora, sin datos personales")
    void evento_contenido() {
        var zona = e.zona("Salon");
        e.mesa(zona, "M1", 4);
        e.mesa(zona, "M2", 4);
        e.servicio.abrirServicio(e.ctxA, e.todo);

        var ultimo = e.eventosDe("negocio.disponibilidad_cambiada").get(2);

        assertThat(ultimo.version()).isEqualTo(1);
        assertThat(ultimo.origen()).isEqualTo("restaurantes");
        assertThat(ultimo.negocioId()).isEqualTo(EntornoDeSalon.NEGOCIO_A);
        assertThat(ultimo.datos()).containsEntry("tipo", "MESAS").containsEntry("libres", 2).containsEntry("total", 2)
                .containsEntry("abierto", true).containsKey("actualizado_en");
    }

    @Test
    @DisplayName("MS-06: una cuenta que se abre y se paga (cambio en las cuentas) publica al recalcular, no antes")
    void evento_alRecalcular() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        e.servicio.abrirServicio(e.ctxA, e.todo);
        var base = e.eventosDe("negocio.disponibilidad_cambiada").size();

        e.ocupacion.ocupar(mesa);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(base);
        e.servicio.recalcularDisponibilidad(EntornoDeSalon.NEGOCIO_A);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(base + 1);

        e.ocupacion.liberar(mesa);
        e.servicio.recalcularDisponibilidad(EntornoDeSalon.NEGOCIO_A);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(base + 2);

        e.servicio.recalcularDisponibilidad(EntornoDeSalon.NEGOCIO_A);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(base + 2);
    }

    // ---- MS-07: baja logica ----

    @Test
    @DisplayName("MS-07: dar de baja una mesa libre la quita del salon y libera su etiqueta")
    void baja_mesa() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);

        e.servicio.darDeBajaMesa(e.ctxA, e.todo, mesa);

        assertThat(e.servicio.salon(e.ctxA, e.mesero).mesas()).isEmpty();
        assertThat(e.mesa(zona, "M1", 4)).isNotEqualTo(mesa);
    }

    @Test
    @DisplayName("MS-07: no se da de baja una mesa con cuenta no pagada")
    void baja_mesaConCuenta() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        e.ocupacion.ocupar(mesa);

        falla(() -> e.servicio.darDeBajaMesa(e.ctxA, e.todo, mesa), CodigoError.MESA_OCUPADA);
    }

    @Test
    @DisplayName("MS-07: no se da de baja una zona con mesas activas; sin mesas, si")
    void baja_zona() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);

        falla(() -> e.servicio.darDeBajaZona(e.ctxA, e.todo, zona), CodigoError.ZONA_CON_MESAS);
        e.servicio.darDeBajaMesa(e.ctxA, e.todo, mesa);
        e.servicio.darDeBajaZona(e.ctxA, e.todo, zona);

        assertThat(e.servicio.salon(e.ctxA, e.mesero).zonas()).isEmpty();
    }

    // ---- MS-08: modo de servicio ----

    @Test
    @DisplayName("MS-08: el modo de servicio nace AMBOS y se puede cambiar con restaurantes.salon.configurar")
    void modo() {
        assertThat(e.servicio.salon(e.ctxA, e.mesero).modo()).isEqualTo(ModoServicio.AMBOS);

        e.servicio.configurarModo(e.ctxA, e.todo, ModoServicio.MOSTRADOR);

        assertThat(e.servicio.salon(e.ctxA, e.mesero).modo()).isEqualTo(ModoServicio.MOSTRADOR);
        assertThatThrownBy(() -> e.servicio.configurarModo(e.ctxA, e.operador, ModoServicio.MESAS))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- MS-09 y QR-01, QR-02: el QR de cada mesa ----

    @Test
    @DisplayName("QR-01: cada mesa activa tiene un token distinto que no contiene el id de la mesa ni el del negocio")
    void qr_tokens() {
        var zona = e.zona("Salon");
        var m1 = e.mesa(zona, "M1", 4);
        var m2 = e.mesa(zona, "M2", 4);

        var u1 = e.servicio.qrDeMesa(e.ctxA, e.todo, m1);
        var u2 = e.servicio.qrDeMesa(e.ctxA, e.todo, m2);

        assertThat(u1).startsWith(EntornoDeSalon.BASE_QR).isNotEqualTo(u2);
        assertThat(u1).doesNotContain(m1.toString()).doesNotContain(EntornoDeSalon.NEGOCIO_A.toString());
    }

    @Test
    @DisplayName("QR-01: el token real es aleatorio de 128 bits en base64url, sin relleno")
    void qr_tokenAleatorio() {
        var token = GeneradorDeTokens.ALEATORIO.nuevo();

        assertThat(token).hasSize(22).matches("[A-Za-z0-9_-]{22}");
        assertThat(GeneradorDeTokens.ALEATORIO.nuevo()).isNotEqualTo(token);
    }

    @Test
    @DisplayName("QR-02: regenerar el QR invalida el token anterior de inmediato y exige restaurantes.salon.configurar")
    void qr_regenerar() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        var viejo = e.servicio.qrDeMesa(e.ctxA, e.todo, mesa).substring(EntornoDeSalon.BASE_QR.length());
        assertThat(e.almacen.mesaPorToken(viejo)).isPresent();

        var nuevo = e.servicio.regenerarQrDeMesa(e.ctxA, e.todo, mesa);

        assertThat(nuevo).isNotEqualTo(EntornoDeSalon.BASE_QR + viejo);
        assertThat(e.almacen.mesaPorToken(viejo)).isEmpty();
        assertThat(e.almacen.mesaPorToken(nuevo.substring(EntornoDeSalon.BASE_QR.length()))).isPresent();
        assertThatThrownBy(() -> e.servicio.regenerarQrDeMesa(e.ctxA, e.operador, mesa))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("MS-09: dar de baja la mesa invalida su QR")
    void qr_bajaInvalida() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);
        var token = e.servicio.qrDeMesa(e.ctxA, e.todo, mesa).substring(EntornoDeSalon.BASE_QR.length());

        e.servicio.darDeBajaMesa(e.ctxA, e.todo, mesa);

        assertThat(e.almacen.mesaPorToken(token)).isEmpty();
    }

    @Test
    @DisplayName("QR-17: pausar el QR de una mesa la deja pausada una hora; reanudarlo la libera; exige servicio.operar")
    void qr_pausar() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);

        var pausada = e.servicio.pausarQrDeMesa(e.ctxA, e.operador, mesa, true);
        assertThat(pausada.qrPausado()).isTrue();
        assertThat(e.almacen.mesa(EntornoDeSalon.NEGOCIO_A, mesa).orElseThrow().qrPausadoHasta())
                .isEqualTo(EntornoDeSalon.AHORA.plus(Duration.ofHours(1)));

        var reanudada = e.servicio.pausarQrDeMesa(e.ctxA, e.operador, mesa, false);
        assertThat(reanudada.qrPausado()).isFalse();
        assertThatThrownBy(() -> e.servicio.pausarQrDeMesa(e.ctxA, e.mesero, mesa, true))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("QR-17: el restaurante puede apagar y volver a encender los pedidos por QR de todo el local")
    void qr_habilitar() {
        assertThat(e.servicio.salon(e.ctxA, e.mesero).qrHabilitado()).isTrue();

        e.servicio.habilitarQr(e.ctxA, e.operador, false);
        assertThat(e.servicio.salon(e.ctxA, e.mesero).qrHabilitado()).isFalse();

        e.servicio.habilitarQr(e.ctxA, e.operador, true);
        assertThat(e.servicio.salon(e.ctxA, e.mesero).qrHabilitado()).isTrue();
        assertThatThrownBy(() -> e.servicio.habilitarQr(e.ctxA, e.mesero, false))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("QR-19: un negocio no puede ver ni regenerar el QR de una mesa de otro")
    void qr_aislado() {
        var zona = e.zona("Salon");
        var mesa = e.mesa(zona, "M1", 4);

        falla(() -> e.servicio.qrDeMesa(e.ctxB, e.todo, mesa), CodigoError.MESA_NO_ENCONTRADA);
        falla(() -> e.servicio.regenerarQrDeMesa(e.ctxB, e.todo, mesa), CodigoError.MESA_NO_ENCONTRADA);
        falla(() -> e.servicio.bloquearMesa(e.ctxB, e.todo, mesa, true), CodigoError.MESA_NO_ENCONTRADA);
    }
}
