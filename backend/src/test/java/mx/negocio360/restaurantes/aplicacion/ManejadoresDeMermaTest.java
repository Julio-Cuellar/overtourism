package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CT-03 y CT-11: anular una linea ya preparada y cancelar una cuenta con platillos preparados van por preparar y
 * confirmar, con el permiso de gerente en las dos puntas (D-126) y con el resumen que advierte de la merma.
 */
class ManejadoresDeMermaTest {

    private final EntornoDeCuentas e = new EntornoDeCuentas().armado();
    /** Quien tenga el permiso de gerente en el contexto, tal como lo ve el despachador. */
    private final AutorizacionDeRestaurantes sesion = (contexto, permiso) -> e.gerente.puede(permiso);
    private final AutorizacionDeRestaurantes sinPermisos = (contexto, permiso) -> false;
    private final ManejadorAnularLineaPreparada anular = new ManejadorAnularLineaPreparada(e.servicio, sesion);
    private final ManejadorCancelarCuentaPreparada cancelar = new ManejadorCancelarCuentaPreparada(e.servicio, sesion);

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    /** Una cuenta con una cemita y una horchata, enviada y con la cemita ya lista. */
    private CuentaVista conCemitaLista() {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, null, false);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false);
        var cocina = e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id()).stream()
                .filter(c -> c.estacion() == Estacion.COCINA).findFirst().orElseThrow();
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, cocina.id());
        return e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id());
    }

    @Test
    @DisplayName("D-126: los dos manejadores declaran restaurantes.linea.anular_preparada para la puerta y para confirmar")
    void declaranElPermiso() {
        var lineaId = UUID.randomUUID();

        assertThat(anular.permisos(new AnularLineaPreparadaComando(lineaId, MotivoDeAnulacion.AGOTADO, null)))
                .containsExactly(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        assertThat(cancelar.permisos(new CancelarCuentaPreparadaComando(lineaId, MotivoDeAnulacion.AGOTADO, null)))
                .containsExactly(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        assertThat(anular.tipo()).isEqualTo("anular_linea_preparada");
        assertThat(cancelar.tipo()).isEqualTo("cancelar_cuenta_preparada");
    }

    @Test
    @DisplayName("CT-03: preparar anular una linea lista muestra la merma y no escribe nada")
    void anular_preparar() {
        var cuenta = conCemitaLista();
        var comando = new AnularLineaPreparadaComando(cuenta.lineas().get(0).id(), MotivoDeAnulacion.ERROR_DEL_PEDIDO, null);

        var resumen = anular.preparar(e.ctxA, comando);

        assertThat(resumen).contains("2 × Cemita poblana").contains("lista").contains("merma")
                .contains("Cancelado: error en el pedido");
        assertThat(e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).lineas().get(0).estado().name()).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("CT-03: confirmar anula la linea ya lista; si cambio entre los dos pasos, COMANDO_CAMBIO")
    void anular_confirmar() {
        var cuenta = conCemitaLista();
        var comando = new AnularLineaPreparadaComando(cuenta.lineas().get(0).id(), MotivoDeAnulacion.ERROR_DEL_PEDIDO, null);
        var resumen = anular.preparar(e.ctxA, comando);

        falla(() -> anular.confirmar(e.ctxA, comando, resumen + " (cambio)"), CodigoError.COMANDO_CAMBIO);
        var resultado = anular.confirmar(e.ctxA, comando, resumen);

        assertThat(resultado.recursoId()).isEqualTo(cuenta.id());
        assertThat(e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).total()).isEqualByComparingTo("35.00");
    }

    @Test
    @DisplayName("D-126: sin el permiso de gerente no se prepara ni se confirma, y nada se escribe")
    void anular_sinPermiso() {
        var cuenta = conCemitaLista();
        var comando = new AnularLineaPreparadaComando(cuenta.lineas().get(0).id(), MotivoDeAnulacion.AGOTADO, null);
        var sin = new ManejadorAnularLineaPreparada(e.servicio, sinPermisos);

        assertThatThrownBy(() -> sin.preparar(e.ctxA, comando)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> sin.confirmar(e.ctxA, comando, "x")).isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("CT-03: la linea debe existir, estar enviada y llevar motivo; OTRO pide texto")
    void anular_validaciones() {
        var cuenta = conCemitaLista();
        var lineaId = cuenta.lineas().get(0).id();

        falla(() -> anular.preparar(e.ctxA, new AnularLineaPreparadaComando(UUID.randomUUID(), MotivoDeAnulacion.AGOTADO,
                null)), CodigoError.LINEA_NO_ENCONTRADA);
        falla(() -> anular.preparar(e.ctxA, new AnularLineaPreparadaComando(null, MotivoDeAnulacion.AGOTADO, null)),
                CodigoError.LINEA_NO_ENCONTRADA);
        falla(() -> anular.preparar(e.ctxA, new AnularLineaPreparadaComando(lineaId, null, null)),
                CodigoError.MOTIVO_REQUERIDO);
        falla(() -> anular.preparar(e.ctxA, new AnularLineaPreparadaComando(lineaId, MotivoDeAnulacion.OTRO, " ")),
                CodigoError.MOTIVO_REQUERIDO);
    }

    @Test
    @DisplayName("CT-11: preparar cancelar cuenta cuenta las lineas y las ya preparadas; confirmar la cancela y libera la mesa")
    void cancelar_flujo() {
        var cuenta = conCemitaLista();
        var comando = new CancelarCuentaPreparadaComando(cuenta.id(), MotivoDeAnulacion.OTRO, "se fueron");

        var resumen = cancelar.preparar(e.ctxA, comando);
        assertThat(resumen).contains("2 línea(s)").contains("1 ya preparada(s)").contains("merma");
        falla(() -> cancelar.confirmar(e.ctxA, comando, "otro resumen"), CodigoError.COMANDO_CAMBIO);
        var resultado = cancelar.confirmar(e.ctxA, comando, resumen);

        assertThat(resultado.recursoId()).isEqualTo(cuenta.id());
        assertThat(e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id()).estado()).isEqualTo(EstadoDeCuenta.CANCELADA);
        assertThat(e.salon.salon(e.ctxA, e.mesero).disponibilidad().libres()).isEqualTo(2);
    }

    @Test
    @DisplayName("CT-11: sin permiso de gerente no se prepara; una cuenta inexistente o de otro negocio no se cancela")
    void cancelar_requisitos() {
        var cuenta = conCemitaLista();
        var comando = new CancelarCuentaPreparadaComando(cuenta.id(), MotivoDeAnulacion.AGOTADO, null);

        assertThatThrownBy(() -> new ManejadorCancelarCuentaPreparada(e.servicio, sinPermisos).preparar(e.ctxA, comando))
                .isInstanceOf(PermisoDenegadoException.class);
        falla(() -> cancelar.preparar(e.ctxA, new CancelarCuentaPreparadaComando(null, MotivoDeAnulacion.AGOTADO, null)),
                CodigoError.CUENTA_NO_ENCONTRADA);
        falla(() -> cancelar.preparar(new ContextoNegocio(EntornoDeCuentas.NEGOCIO_B, "beto"), comando),
                CodigoError.CUENTA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("MN-10: un platillo en una cuenta abierta o por cobrar esta en uso; anulado o con la cuenta cancelada, no")
    void usoPorCuentas() {
        var uso = new UsoPorCuentas(e.almacen);
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        assertThat(uso.enCuentaAbierta(EntornoDeCuentas.NEGOCIO_A, e.cemita)).isFalse();

        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 1, null, false);
        assertThat(uso.enCuentaAbierta(EntornoDeCuentas.NEGOCIO_A, e.cemita)).isTrue();
        assertThat(uso.enCuentaAbierta(EntornoDeCuentas.NEGOCIO_A, e.horchata)).isFalse();
        assertThat(uso.enCuentaAbierta(EntornoDeCuentas.NEGOCIO_B, e.cemita)).isFalse();

        e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), MotivoDeAnulacion.SIN_COMENSALES, null);
        assertThat(uso.enCuentaAbierta(EntornoDeCuentas.NEGOCIO_A, e.cemita)).isFalse();
    }
}
