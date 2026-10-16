package mx.negocio360.clientes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import mx.negocio360.clientes.aplicacion.ConsentimientoSolicitado;
import mx.negocio360.clientes.aplicacion.ManejadorComando;
import mx.negocio360.clientes.aplicacion.ManejadorRegistrarCliente;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.clientes.aplicacion.ResultadoComando;
import mx.negocio360.clientes.aplicacion.PlantillasClientes;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.eventos.ClienteConsentimientoCambiado;
import mx.negocio360.clientes.eventos.ClienteRegistrado;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.clientes.fakes.Escenario;
import mx.negocio360.compartido.GestorTransaccionalFalso;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cada {@code confirmar} de un manejador es una sola transaccion que incluye sus eventos (ServicioPreparacion, contrato
 * transaccional). El decorador pone la transaccion fuera del manejador, que sigue siendo Java puro.
 */
class ManejadorTransaccionalTest {

    private final GestorTransaccionalFalso gestor = new GestorTransaccionalFalso();

    /** Manejador de mentira que anota si corrio dentro de una transaccion y puede fallar. */
    private static final class ManejadorEspia implements ManejadorComando<RegistrarClienteComando> {
        final List<String> llamadas = new ArrayList<>();
        RuntimeException fallo;

        @Override
        public String tipo() {
            return "espia";
        }

        @Override
        public Class<RegistrarClienteComando> claseComando() {
            return RegistrarClienteComando.class;
        }

        @Override
        public String preparar(ContextoNegocio contexto, RegistrarClienteComando comando) {
            llamadas.add("preparar:" + TransactionSynchronizationManager.isActualTransactionActive());
            return "resumen";
        }

        @Override
        public ResultadoComando confirmar(ContextoNegocio contexto, RegistrarClienteComando comando) {
            llamadas.add("confirmar:" + TransactionSynchronizationManager.isActualTransactionActive());
            if (fallo != null) {
                throw fallo;
            }
            return ResultadoComando.ok(PlantillasClientes.CLIENTE_GUARDADO, null);
        }
    }

    @Test
    @DisplayName("confirmar corre en una transaccion y la confirma cuando el manejador termina bien")
    void confirmar_correEnTransaccionYConfirma() {
        var espia = new ManejadorEspia();
        var manejador = ManejadorTransaccional.de(espia, gestor.plantilla());

        manejador.confirmar(new Escenario().ctxA, null);

        assertThat(espia.llamadas).containsExactly("confirmar:true");
        assertThat(gestor.historia()).containsExactly("inicio", "commit");
    }

    @Test
    @DisplayName("si confirmar falla, la transaccion se deshace y el error llega tal cual")
    void confirmar_siFalla_deshaceYPropagaElError() {
        var espia = new ManejadorEspia();
        espia.fallo = new ClientesException(CodigoError.TELEFONO_DUPLICADO);
        var manejador = ManejadorTransaccional.de(espia, gestor.plantilla());

        assertThatThrownBy(() -> manejador.confirmar(new Escenario().ctxA, null))
                .isSameAs(espia.fallo);

        assertThat(gestor.historia()).containsExactly("inicio", "rollback");
    }

    @Test
    @DisplayName("preparar no abre transaccion: no escribe nada")
    void preparar_noAbreTransaccion() {
        var espia = new ManejadorEspia();
        var manejador = ManejadorTransaccional.de(espia, gestor.plantilla());

        var resumen = manejador.preparar(new Escenario().ctxA, null);

        assertThat(resumen).isEqualTo("resumen");
        assertThat(espia.llamadas).containsExactly("preparar:false");
        assertThat(gestor.historia()).isEmpty();
    }

    @Test
    @DisplayName("el decorador conserva el tipo y la clase del comando del manejador")
    void decorador_conservaTipoYClaseDelComando() {
        var manejador = ManejadorTransaccional.de(new ManejadorEspia(), gestor.plantilla());

        assertThat(manejador.tipo()).isEqualTo("espia");
        assertThat(manejador.claseComando()).isEqualTo(RegistrarClienteComando.class);
    }

    @Test
    @DisplayName("registrar un cliente dentro del decorador lleva al bus cliente.registrado y un cambio por consentimiento")
    void registrarCliente_conElDecorador_llevaLosEventosAlBus() {
        var escenario = new Escenario();
        escenario.conAviso(escenario.ctxA);
        var enElBus = new ArrayList<Object>();
        ApplicationEventPublisher bus = enElBus::add;
        var manejador = ManejadorTransaccional.de(new ManejadorRegistrarCliente(escenario.clientes, escenario.avisos,
                escenario.consentimientos, new PublicadorEventosEnBus(bus), escenario.reloj), gestor.plantilla());
        var comando = new RegistrarClienteComando("Ana", null, null, null, null, null, 1, List.of(
                new ConsentimientoSolicitado(Finalidad.CONTACTO_MENSAJERIA, MedioConsentimiento.VERBAL),
                new ConsentimientoSolicitado(Finalidad.PROMOCIONES, MedioConsentimiento.PANEL)));

        manejador.confirmar(escenario.ctxA, comando);

        assertThat(enElBus).hasSize(3).allSatisfy(o -> assertThat(o).isInstanceOf(SobreEvento.class));
        assertThat(enElBus).extracting(o -> ((SobreEvento) o).tipo()).containsExactly(ClienteRegistrado.TIPO,
                ClienteConsentimientoCambiado.TIPO, ClienteConsentimientoCambiado.TIPO);
    }

    @Test
    @DisplayName("sin el decorador, publicar falla cerrado: el evento no se pierde en silencio")
    void registrarCliente_sinTransaccion_fallaCerrado() {
        var escenario = new Escenario();
        escenario.conAviso(escenario.ctxA);
        var enElBus = new ArrayList<Object>();
        ApplicationEventPublisher bus = enElBus::add;
        var desnudo = new ManejadorRegistrarCliente(escenario.clientes, escenario.avisos, escenario.consentimientos,
                new PublicadorEventosEnBus(bus), escenario.reloj);

        assertThatThrownBy(() -> desnudo.confirmar(escenario.ctxA,
                new RegistrarClienteComando("Ana", null, null, null, null, null, 1, List.of())))
                .isInstanceOf(IllegalStateException.class);

        assertThat(enElBus).isEmpty();
    }
}
