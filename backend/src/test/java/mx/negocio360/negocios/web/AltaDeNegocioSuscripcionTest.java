package mx.negocio360.negocios.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import mx.negocio360.negocios.infraestructura.AvisosDeAlta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** W1: la suscripcion altaDeNegocio. Solo para quien tiene sesion, aunque no tenga negocio activo; se cierra sola al avisar. */
class AltaDeNegocioSuscripcionTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");

    private final AltasApi altas = new AltasApi() {
        @Override
        public ResultadoAlta registrarAlta(NegocioRegistroSolicitadoV1 pedido) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AltaVista> misAltas(UUID cuentaId) {
            return List.of();
        }
    };
    private final AvisosDeAlta avisos = new AvisosDeAlta(altas);
    private final NegociosGraphQlController controlador = new NegociosGraphQlController(
            Mockito.mock(NegociosApi.class), avisos);

    private static SobreEvento rechazo(UUID negocio, UUID cuenta) {
        return new SobreEvento(UuidV7.generar(AHORA), "negocio.registro_rechazado", 1, negocio, "negocios", AHORA,
                UuidV7.generar(AHORA), null, Map.of("solicitado_por", cuenta.toString(), "nombre", "Farmacia",
                        "giro", "FARMACIA", "motivo", "GIRO_INVALIDO"));
    }

    @Test
    @DisplayName("sin sesion no hay suscripcion: NO_AUTENTICADO")
    void sinSesion_noSuscribe() {
        assertThatThrownBy(() -> controlador.altaDeNegocio(EntradaGraphQl.anonima(null)))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    @DisplayName("el aviso llega a la cuenta, el flujo se completa solo y la suscripcion se libera")
    void avisa_ySeCompleta() {
        var cuenta = UuidV7.generar(AHORA);
        var negocio = UuidV7.generar(AHORA);
        var recibidos = new CopyOnWriteArrayList<AvisosDeAlta.Aviso>();
        var completo = new AtomicBoolean();
        controlador.altaDeNegocio(EntradaGraphQl.de(cuenta, null, mx.negocio360.compartido.seguridad.Autorizacion
                .de(java.util.Set.of()), null)).subscribe(recibidos::add, e -> { }, () -> completo.set(true));
        assertThat(avisos.suscritas()).isEqualTo(1);

        avisos.alRecibir(rechazo(negocio, cuenta));

        assertThat(recibidos).containsExactly(new AvisosDeAlta.Aviso(negocio, AltaVista.EstadoAlta.RECHAZADO,
                "GIRO_INVALIDO"));
        assertThat(completo).isTrue();
        assertThat(avisos.suscritas()).isZero();
    }

    @Test
    @DisplayName("si la app cancela, la suscripcion se libera y ya no recibe avisos")
    void cancelar_liberaLaSuscripcion() {
        var cuenta = UuidV7.generar(AHORA);
        var recibidos = new CopyOnWriteArrayList<AvisosDeAlta.Aviso>();
        var disposable = controlador.altaDeNegocio(EntradaGraphQl.de(cuenta, null,
                mx.negocio360.compartido.seguridad.Autorizacion.de(java.util.Set.of()), null)).subscribe(recibidos::add);

        disposable.dispose();
        avisos.alRecibir(rechazo(UuidV7.generar(AHORA), cuenta));

        assertThat(recibidos).isEmpty();
        assertThat(avisos.suscritas()).isZero();
    }
}
