package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;
import mx.negocio360.restaurantes.dominio.EstadoDeMesa;
import mx.negocio360.restaurantes.dominio.ModoServicio;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R5 · CT-01 a CT-07, CT-11, CT-12, CT-15 y QR-08, QR-10: la cuenta abierta del restaurante. Cada prueba cita su regla. */
class CuentasServicioTest {

    private final EntornoDeCuentas e = new EntornoDeCuentas().armado();

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    private CuentaVista abrir() {
        return e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
    }

    private CuentaVista conCemitaYHorchata() {
        var cuenta = abrir();
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, null, false);
        return e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false);
    }

    // ---- CT-01: abrir cuenta ----

    @Test
    @DisplayName("CT-01: abrir cuenta deja una cuenta ABIERTA del personal, con numero consecutivo y la mesa ocupada")
    void abrir_nace() {
        var primera = abrir();
        var segunda = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa2, 4);

        assertThat(primera.estado()).isEqualTo(EstadoDeCuenta.ABIERTA);
        assertThat(primera.origen()).isEqualTo(OrigenDeCuenta.PERSONAL);
        assertThat(primera.numero()).isEqualTo(1);
        assertThat(segunda.numero()).isEqualTo(2);
        assertThat(primera.comensales()).isEqualTo(2);
        assertThat(primera.mesa()).isEqualTo("Mesa 1");
        assertThat(e.salon.salon(e.ctxA, e.mesero).mesas()).filteredOn(m -> m.id().equals(e.mesa1))
                .extracting(MesaVista::estado).containsExactly(EstadoDeMesa.OCUPADA);
    }

    @Test
    @DisplayName("CT-01: sin servicio abierto no se abren cuentas")
    void abrir_servicioCerrado() {
        e.salon.cerrarServicio(e.ctxA, e.dueno, null);

        falla(() -> abrir(), CodigoError.SERVICIO_CERRADO);
    }

    @Test
    @DisplayName("CT-01: los comensales van de 1 a 50")
    void abrir_comensales() {
        falla(() -> e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 0), CodigoError.COMENSALES_INVALIDOS);
        falla(() -> e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 51), CodigoError.COMENSALES_INVALIDOS);
        assertThat(e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 6).comensales()).isEqualTo(6);
    }

    @Test
    @DisplayName("MS-03: una mesa tiene a lo mas una cuenta sin pagar")
    void abrir_mesaOcupada() {
        abrir();

        falla(() -> abrir(), CodigoError.MESA_OCUPADA);
    }

    @Test
    @DisplayName("MS-03: una mesa bloqueada no recibe cuenta")
    void abrir_mesaBloqueada() {
        e.salon.bloquearMesa(e.ctxA, e.dueno, e.mesa1, true);

        falla(() -> abrir(), CodigoError.MESA_BLOQUEADA);
    }

    @Test
    @DisplayName("MS-08: en modo MOSTRADOR la cuenta no lleva mesa; en MESAS la mesa es obligatoria")
    void abrir_modos() {
        e.salon.configurarModo(e.ctxA, e.dueno, ModoServicio.MOSTRADOR);
        assertThat(e.servicio.abrirCuenta(e.ctxA, e.mesero, null, 1).mesaId()).isNull();
        falla(() -> e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 1), CodigoError.MESA_NO_PERMITIDA);

        e.salon.configurarModo(e.ctxA, e.dueno, ModoServicio.MESAS);
        falla(() -> e.servicio.abrirCuenta(e.ctxA, e.mesero, null, 1), CodigoError.MESA_REQUERIDA);
    }

    @Test
    @DisplayName("CT-01: la mesa de otro negocio no existe para este")
    void abrir_mesaDeOtroNegocio() {
        e.salon.abrirServicio(e.ctxB, e.dueno);

        falla(() -> e.servicio.abrirCuenta(e.ctxB, e.dueno, e.mesa1, 2), CodigoError.MESA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("CT-01: abrir una cuenta exige restaurantes.cuenta.abrir")
    void abrir_exigePermiso() {
        assertThatThrownBy(() -> e.servicio.abrirCuenta(e.ctxA, e.cocinero, e.mesa1, 2))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("MS-06: abrir y cerrar una cuenta cambia las mesas libres y publica la disponibilidad solo al cambiar")
    void abrir_publicaDisponibilidad() {
        var cuenta = abrir();
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(1);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada").get(0).datos()).containsEntry("libres", 1)
                .containsEntry("total", 2);

        e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), MotivoDeAnulacion.SIN_COMENSALES, null);

        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(2);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada").get(1).datos()).containsEntry("libres", 2);
    }

    // ---- CT-02: agregar lineas ----

    @Test
    @DisplayName("CT-02: una linea nace PENDIENTE con el precio del platillo y la estacion de su categoria")
    void linea_nace() {
        var cuenta = conCemitaYHorchata();

        assertThat(cuenta.lineas()).extracting(LineaVista::nombre, LineaVista::cantidad, LineaVista::estado,
                LineaVista::estacion).containsExactly(
                org.assertj.core.groups.Tuple.tuple("Cemita poblana", 2, EstadoDeLinea.PENDIENTE, Estacion.COCINA),
                org.assertj.core.groups.Tuple.tuple("Agua de horchata", 1, EstadoDeLinea.PENDIENTE, Estacion.BARRA));
        assertThat(cuenta.total()).isEqualByComparingTo("205.00");
    }

    @Test
    @DisplayName("CT-02: la cantidad es un entero de 1 a 99 y la nota de hasta 80 caracteres")
    void linea_limites() {
        var cuenta = abrir();

        falla(() -> e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 0, null, false),
                CodigoError.CANTIDAD_INVALIDA);
        falla(() -> e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 100, null, false),
                CodigoError.CANTIDAD_INVALIDA);
        falla(() -> e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, "x".repeat(81), false),
                CodigoError.NOTA_INVALIDA);
        var conNota = e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, "sin cebolla", true);
        assertThat(conNota.lineas().get(0).nota()).isEqualTo("sin cebolla");
        assertThat(conNota.lineas().get(0).marcaAlergia()).isTrue();
    }

    @Test
    @DisplayName("CT-02: un platillo agotado, inactivo o de otro negocio no se agrega")
    void linea_noOrdenable() {
        var cuenta = abrir();
        e.menuServicio.marcarAgotado(e.ctxA, e.dueno, e.cemita, true);

        falla(() -> e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false),
                CodigoError.PLATILLO_AGOTADO);
        falla(() -> e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), UUID.randomUUID(), 1, null, false),
                CodigoError.PLATILLO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("CT-02: agregar a una cuenta por cobrar falla con CUENTA_NO_ABIERTA")
    void linea_cuentaPorCobrar() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());

        falla(() -> e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false),
                CodigoError.CUENTA_NO_ABIERTA);
    }

    @Test
    @DisplayName("CT-03: una linea pendiente se quita libremente; una enviada ya no")
    void linea_quitarPendiente() {
        var cuenta = conCemitaYHorchata();
        var linea = cuenta.lineas().get(0).id();

        var sin = e.servicio.quitarLineaPendiente(e.ctxA, e.mesero, cuenta.id(), linea);
        assertThat(sin.lineas()).hasSize(1);

        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var enviada = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0).id();
        falla(() -> e.servicio.quitarLineaPendiente(e.ctxA, e.mesero, cuenta.id(), enviada), CodigoError.TRANSICION_INVALIDA);
    }

    // ---- CT-04: enviar a cocina ----

    @Test
    @DisplayName("CT-04: un envio con una cemita y una bebida crea dos comandas, una por estacion, con el mismo envio")
    void enviar_unaPorEstacion() {
        var cuenta = conCemitaYHorchata();

        var comandas = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());

        assertThat(comandas).extracting(ComandaVista::estacion).containsExactlyInAnyOrder(Estacion.COCINA, Estacion.BARRA);
        assertThat(comandas).extracting(ComandaVista::estado).containsOnly(EstadoDeComanda.NUEVA);
        assertThat(comandas.get(0).envioId()).isEqualTo(comandas.get(1).envioId());
        assertThat(e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas()).extracting(LineaVista::estado)
                .containsOnly(EstadoDeLinea.ENVIADA);
    }

    @Test
    @DisplayName("CT-04: cada comanda publica restaurantes.comanda_enviada con sus lineas, la mesa y el numero de cuenta")
    void enviar_publica() {
        var cuenta = conCemitaYHorchata();

        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());

        var enviados = e.eventosDe("restaurantes.comanda_enviada");
        assertThat(enviados).hasSize(2);
        var cocina = enviados.stream().filter(ev -> "COCINA".equals(ev.datos().get("estacion"))).findFirst().orElseThrow();
        assertThat(cocina.origen()).isEqualTo("restaurantes");
        assertThat(cocina.datos()).containsEntry("mesa", "Mesa 1").containsEntry("numero", 1)
                .containsEntry("cuenta_id", cuenta.id().toString()).containsKeys("comanda_id", "envio_id", "lineas");
        @SuppressWarnings("unchecked")
        var lineas = (List<Map<String, Object>>) cocina.datos().get("lineas");
        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0)).containsEntry("platillo", "Cemita poblana").containsEntry("cantidad", 2)
                .containsEntry("marca_alergia", false);
    }

    @Test
    @DisplayName("CT-04: enviar sin lineas pendientes falla con SIN_LINEAS_PENDIENTES")
    void enviar_sinPendientes() {
        var cuenta = abrir();

        falla(() -> e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id()), CodigoError.SIN_LINEAS_PENDIENTES);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        falla(() -> e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id()), CodigoError.SIN_LINEAS_PENDIENTES);
    }

    @Test
    @DisplayName("CT-04: un segundo envio crea comandas nuevas con otro envio y no toca las anteriores")
    void enviar_segundoEnvio() {
        var cuenta = abrir();
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false);
        var primero = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false);

        var segundo = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());

        assertThat(segundo.get(0).envioId()).isNotEqualTo(primero.get(0).envioId());
        assertThat(e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50)).hasSize(2);
    }

    // ---- CT-05, CT-06: pedir la cuenta ----

    @Test
    @DisplayName("CT-05: pedir la cuenta la pasa a POR_COBRAR, guarda el total con los precios vigentes y avisa")
    void pedir_cuenta() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.menuServicio.editarPlatillo(e.ctxA, e.dueno, e.cemita, 1,
                new DatosPlatillo("Cemita poblana", null, new BigDecimal("90.00"), null, Set.of(), null));

        var pedida = e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());

        assertThat(pedida.estado()).isEqualTo(EstadoDeCuenta.POR_COBRAR);
        assertThat(pedida.total()).isEqualByComparingTo("215.00");
        var avisos = e.eventosDe("restaurantes.cuenta_solicitada");
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).datos()).containsEntry("mesa", "Mesa 1").containsEntry("solicitada_por", "ana")
                .containsEntry("cuenta_id", cuenta.id().toString());
    }

    @Test
    @DisplayName("CT-05: pide al menos una linea no anulada y ninguna pendiente")
    void pedir_requisitos() {
        var vacia = abrir();
        falla(() -> e.servicio.pedirCuenta(e.ctxA, e.mesero, vacia.id()), CodigoError.CUENTA_VACIA);

        e.servicio.agregarLinea(e.ctxA, e.mesero, vacia.id(), e.cemita, 1, null, false);
        falla(() -> e.servicio.pedirCuenta(e.ctxA, e.mesero, vacia.id()), CodigoError.LINEAS_SIN_ENVIAR);
    }

    @Test
    @DisplayName("CT-06: reabrir una cuenta por cobrar la regresa a ABIERTA; solo si estaba por cobrar")
    void reabrir() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        falla(() -> e.servicio.reabrirCuenta(e.ctxA, e.mesero, cuenta.id()), CodigoError.CUENTA_NO_SOLICITADA);
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());

        var reabierta = e.servicio.reabrirCuenta(e.ctxA, e.mesero, cuenta.id());

        assertThat(reabierta.estado()).isEqualTo(EstadoDeCuenta.ABIERTA);
        assertThat(e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false).lineas()).hasSize(3);
    }

    // ---- CT-07: cuenta para cobro ----

    @Test
    @DisplayName("CT-07: la cuenta para cobro agrega las lineas por platillo, da el total y la referencia opaca")
    void cobro() {
        var cuenta = abrir();
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, null, false);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, "sin cebolla", false);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());

        var cobro = e.servicio.cuentaParaCobro(e.ctxA, e.mesero, cuenta.id());

        assertThat(cobro.referencia()).isEqualTo("restaurantes:cuenta:" + cuenta.id());
        assertThat(cobro.total()).isEqualByComparingTo("290.00");
        assertThat(cobro.lineas()).extracting(CuentaParaCobro.LineaDeCobro::productoId,
                CuentaParaCobro.LineaDeCobro::cantidad).containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(e.cemita, 3), org.assertj.core.groups.Tuple.tuple(e.horchata, 1));
    }

    @Test
    @DisplayName("CT-07: solo una cuenta por cobrar se puede cobrar, y las lineas anuladas no cuentan")
    void cobro_requisitos() {
        var cuenta = conCemitaYHorchata();
        falla(() -> e.servicio.cuentaParaCobro(e.ctxA, e.mesero, cuenta.id()), CodigoError.CUENTA_NO_SOLICITADA);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var horchata = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(1).id();
        e.servicio.anularLinea(e.ctxA, e.mesero, horchata, MotivoDeAnulacion.ERROR_DEL_PEDIDO, null);
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());

        var cobro = e.servicio.cuentaParaCobro(e.ctxA, e.mesero, cuenta.id());

        assertThat(cobro.total()).isEqualByComparingTo("170.00");
        assertThat(cobro.lineas()).hasSize(1);
    }

    // ---- CT-03 y QR-10: anular una linea ----

    @Test
    @DisplayName("QR-10: anular una linea exige un motivo del catalogo; OTRO pide texto de hasta 80 caracteres")
    void anular_motivo() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var linea = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0).id();

        falla(() -> e.servicio.anularLinea(e.ctxA, e.mesero, linea, null, null), CodigoError.MOTIVO_REQUERIDO);
        falla(() -> e.servicio.anularLinea(e.ctxA, e.mesero, linea, MotivoDeAnulacion.OTRO, "  "),
                CodigoError.MOTIVO_REQUERIDO);
        falla(() -> e.servicio.anularLinea(e.ctxA, e.mesero, linea, MotivoDeAnulacion.OTRO, "x".repeat(81)),
                CodigoError.MOTIVO_INVALIDO);
        var anulada = e.servicio.anularLinea(e.ctxA, e.mesero, linea, MotivoDeAnulacion.AGOTADO, null);

        var vista = anulada.lineas().get(0);
        assertThat(vista.estado()).isEqualTo(EstadoDeLinea.ANULADA);
        assertThat(vista.mensajeDelMotivo()).isEqualTo("Cancelado: agotado");
        assertThat(anulada.total()).isEqualByComparingTo("35.00");
    }

    @Test
    @DisplayName("CT-03: el mesero anula una linea mientras su comanda esta NUEVA o EN_PREPARACION y no una LISTA")
    void anular_segunLaComanda() {
        var cuenta = conCemitaYHorchata();
        var comandas = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var cocina = comandas.stream().filter(c -> c.estacion() == Estacion.COCINA).findFirst().orElseThrow();
        var lineaCocina = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0).id();
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        falla(() -> e.servicio.anularLinea(e.ctxA, e.mesero, lineaCocina, MotivoDeAnulacion.AGOTADO, null),
                CodigoError.LINEA_PREPARADA);
        var horchata = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(1).id();
        assertThat(e.servicio.anularLinea(e.ctxA, e.mesero, horchata, MotivoDeAnulacion.AGOTADO, null).lineas().get(1)
                .estado()).isEqualTo(EstadoDeLinea.ANULADA);
    }

    @Test
    @DisplayName("CT-03: una linea ya lista la anula quien tiene linea.anular_preparada, y queda registrada")
    void anular_preparada() {
        var cuenta = conCemitaYHorchata();
        var cocina = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id()).stream()
                .filter(c -> c.estacion() == Estacion.COCINA).findFirst().orElseThrow();
        var linea = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0).id();
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        assertThatThrownBy(() -> e.servicio.anularLineaPreparada(e.ctxA, e.mesero, linea, MotivoDeAnulacion.ERROR_DEL_PEDIDO,
                null)).isInstanceOf(PermisoDenegadoException.class);
        var anulada = e.servicio.anularLineaPreparada(e.ctxA, e.gerente, linea, MotivoDeAnulacion.ERROR_DEL_PEDIDO, null);

        assertThat(anulada.lineas().get(0).estado()).isEqualTo(EstadoDeLinea.ANULADA);
        assertThat(anulada.lineas().get(0).anuladaEn()).isNotNull();
    }

    @Test
    @DisplayName("CD-05, CD-08: anular una linea actualiza solo la comanda de su estacion y publica comanda_actualizada")
    void anular_actualizaLaComanda() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var horchata = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(1).id();
        e.eventos.clear();

        e.servicio.anularLinea(e.ctxA, e.mesero, horchata, MotivoDeAnulacion.AGOTADO, null);

        var avisos = e.eventosDe("restaurantes.comanda_actualizada");
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).datos()).containsEntry("estacion", "BARRA").containsKeys("comanda_id", "envio_id", "estado",
                "lineas_anuladas");
        var tablero = e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50);
        var cocinaComanda = tablero.stream().filter(c -> c.estacion() == Estacion.COCINA).findFirst().orElseThrow();
        assertThat(cocinaComanda.estado()).isEqualTo(EstadoDeComanda.NUEVA);
        assertThat(cocinaComanda.lineas()).noneMatch(LineaDeComandaVista::anulada);
        assertThat(tablero).noneMatch(c -> c.estacion() == Estacion.BARRA);
    }

    @Test
    @DisplayName("CT-03: una cuenta sin lineas a pedir despues de anular todo no se puede pedir")
    void anular_todo() {
        var cuenta = abrir();
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        var linea = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0).id();
        e.servicio.anularLinea(e.ctxA, e.mesero, linea, MotivoDeAnulacion.AGOTADO, null);

        falla(() -> e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id()), CodigoError.CUENTA_VACIA);
    }

    // ---- CT-11: cancelar la cuenta ----

    @Test
    @DisplayName("CT-11: cancelar una cuenta anula sus lineas, la deja CANCELADA y libera la mesa")
    void cancelar() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());

        var cancelada = e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), MotivoDeAnulacion.SIN_COMENSALES, null);

        assertThat(cancelada.estado()).isEqualTo(EstadoDeCuenta.CANCELADA);
        assertThat(cancelada.lineas()).extracting(LineaVista::estado).containsOnly(EstadoDeLinea.ANULADA);
        assertThat(e.salon.salon(e.ctxA, e.mesero).mesas()).filteredOn(m -> m.id().equals(e.mesa1))
                .extracting(MesaVista::estado).containsExactly(EstadoDeMesa.LIBRE);
        assertThat(e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50)).isEmpty();
        assertThat(e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2).numero()).isEqualTo(2);
    }

    @Test
    @DisplayName("CT-11: cancelar exige motivo; con lineas LISTA o ENTREGADA exige el permiso de gerente")
    void cancelar_requisitos() {
        var cuenta = conCemitaYHorchata();
        var cocina = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id()).stream()
                .filter(c -> c.estacion() == Estacion.COCINA).findFirst().orElseThrow();
        falla(() -> e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), null, null), CodigoError.MOTIVO_REQUERIDO);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());

        falla(() -> e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), MotivoDeAnulacion.OTRO, "se fueron"),
                CodigoError.CUENTA_CON_LINEAS_PREPARADAS);
        assertThatThrownBy(() -> e.servicio.cancelarCuentaPreparada(e.ctxA, e.mesero, cuenta.id(),
                MotivoDeAnulacion.OTRO, "se fueron")).isInstanceOf(PermisoDenegadoException.class);
        var cancelada = e.servicio.cancelarCuentaPreparada(e.ctxA, e.gerente, cuenta.id(), MotivoDeAnulacion.OTRO, "se fueron");
        assertThat(cancelada.estado()).isEqualTo(EstadoDeCuenta.CANCELADA);
    }

    @Test
    @DisplayName("CT-11: cancelar exige restaurantes.cuenta.cancelar")
    void cancelar_exigePermiso() {
        var cuenta = abrir();

        assertThatThrownBy(() -> e.servicio.cancelarCuenta(e.ctxA, e.cocinero, cuenta.id(), MotivoDeAnulacion.AGOTADO, null))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- CT-15: liberar mesa ----

    @Test
    @DisplayName("CT-15: liberarMesa cierra una cuenta cobrada fuera del flujo sin venta, con motivo y permiso de gerente")
    void liberar() {
        var cuenta = conCemitaYHorchata();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());

        assertThatThrownBy(() -> e.servicio.liberarMesa(e.ctxA, e.mesero, cuenta.id(), "Cobrada a mano"))
                .isInstanceOf(PermisoDenegadoException.class);
        falla(() -> e.servicio.liberarMesa(e.ctxA, e.gerente, cuenta.id(), " "), CodigoError.MOTIVO_REQUERIDO);
        var liberada = e.servicio.liberarMesa(e.ctxA, e.gerente, cuenta.id(), "Cobrada a mano");

        assertThat(liberada.estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(liberada.ventaId()).isNull();
        assertThat(e.salon.salon(e.ctxA, e.mesero).mesas()).filteredOn(m -> m.id().equals(e.mesa1))
                .extracting(MesaVista::estado).containsExactly(EstadoDeMesa.LIBRE);
    }

    @Test
    @DisplayName("CT-15: solo una cuenta por cobrar se libera a mano")
    void liberar_requisitos() {
        var cuenta = abrir();

        falla(() -> e.servicio.liberarMesa(e.ctxA, e.gerente, cuenta.id(), "Cobrada a mano"),
                CodigoError.CUENTA_NO_SOLICITADA);
    }

    // ---- QR-08 y consultas ----

    @Test
    @DisplayName("QR-08: confirmar presencia es informativo y no cambia la cuenta ni la cocina")
    void presencia() {
        var cuenta = abrir();
        assertThat(cuenta.presenciaConfirmada()).isFalse();

        var confirmada = e.servicio.confirmarPresencia(e.ctxA, e.mesero, cuenta.id());

        assertThat(confirmada.presenciaConfirmada()).isTrue();
        assertThat(confirmada.estado()).isEqualTo(EstadoDeCuenta.ABIERTA);
    }

    @Test
    @DisplayName("las cuentas abiertas de una mesa y de todo el negocio, solo las que no estan pagadas ni canceladas")
    void cuentasAbiertas() {
        var uno = abrir();
        var dos = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa2, 2);
        e.servicio.cancelarCuenta(e.ctxA, e.mesero, dos.id(), MotivoDeAnulacion.SIN_COMENSALES, null);

        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, null)).extracting(CuentaVista::id).containsExactly(uno.id());
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1)).hasSize(1);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa2)).isEmpty();
    }

    @Test
    @DisplayName("SR-01: un negocio no ve ni toca las cuentas de otro")
    void aislamiento() {
        var cuenta = conCemitaYHorchata();

        falla(() -> e.servicio.cuenta(e.ctxB, e.dueno, cuenta.id()), CodigoError.CUENTA_NO_ENCONTRADA);
        falla(() -> e.servicio.pedirCuenta(e.ctxB, e.dueno, cuenta.id()), CodigoError.CUENTA_NO_ENCONTRADA);
        falla(() -> e.servicio.anularLinea(e.ctxB, e.dueno, cuenta.lineas().get(0).id(), MotivoDeAnulacion.AGOTADO, null),
                CodigoError.LINEA_NO_ENCONTRADA);
        assertThat(e.servicio.cuentasAbiertas(e.ctxB, e.dueno, null)).isEmpty();
    }

    @Test
    @DisplayName("AZ-04: ver y operar cuentas exige sus permisos")
    void permisos() {
        var cuenta = abrir();

        assertThatThrownBy(() -> e.servicio.cuenta(e.ctxA, e.cocinero, cuenta.id()))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.servicio.agregarLinea(e.ctxA, EntornoDeSalon.solo(PermisosDeRestaurantes.CUENTA_VER),
                cuenta.id(), e.cemita, 1, null, false)).isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- CT-12: datos que se borran a los 30 dias ----

    @Test
    @DisplayName("CT-12: pasados 30 dias se vacian las notas de las lineas; el resto de la cuenta queda")
    void purgar() {
        var cuenta = abrir();
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, "sin cebolla", true);
        assertThat(e.servicio.purgarDatosViejos(e.reloj.instant())).isZero();

        e.reloj.avanzar(Duration.ofDays(31));
        var purgadas = e.servicio.purgarDatosViejos(e.reloj.instant());

        assertThat(purgadas).isEqualTo(1);
        var linea = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0);
        assertThat(linea.nota()).isNull();
        assertThat(linea.nombre()).isEqualTo("Cemita poblana");
        assertThat(e.servicio.purgarDatosViejos(e.reloj.instant())).isZero();
    }
}
