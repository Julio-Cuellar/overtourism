package mx.negocio360.negocios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** W1: el aviso en vivo del alta. Es un atajo: la verdad esta en misAltasDeNegocio. */
class AvisosDeAltaTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");

    private final List<AltaVista> altasDeLaCuenta = new ArrayList<>();
    private final AltasApi altas = new AltasApi() {
        @Override
        public ResultadoAlta registrarAlta(NegocioRegistroSolicitadoV1 pedido) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AltaVista> misAltas(UUID cuentaId) {
            return altasDeLaCuenta;
        }
    };
    private final AvisosDeAlta avisos = new AvisosDeAlta(altas);

    /** Un suscriptor que anota lo que recibe. */
    private static final class Anotador implements AvisosDeAlta.Suscriptor {
        final List<AvisosDeAlta.Aviso> recibidos = new ArrayList<>();
        boolean completo;

        @Override
        public void aviso(AvisosDeAlta.Aviso aviso) {
            recibidos.add(aviso);
        }

        @Override
        public void completo() {
            completo = true;
        }
    }

    private static SobreEvento membresia(UUID negocio, UUID cuenta, String origen) {
        return new SobreEvento(UuidV7.generar(AHORA), "identidad.membresia_agregada", 1, negocio, "identidad", AHORA,
                UuidV7.generar(AHORA), null, Map.of("cuenta_id", cuenta.toString(), "rol", "DUENO", "origen", origen));
    }

    private static SobreEvento rechazo(UUID negocio, UUID cuenta) {
        return new SobreEvento(UuidV7.generar(AHORA), "negocio.registro_rechazado", 1, negocio, "negocios", AHORA,
                UuidV7.generar(AHORA), null, Map.of("solicitado_por", cuenta.toString(), "nombre", "Farmacia",
                        "giro", "FARMACIA", "motivo", "GIRO_INVALIDO"));
    }

    @Test
    @DisplayName("la membresia del dueno inicial avisa REGISTRADO a su cuenta y cierra la suscripcion")
    void membresiaDelDuenoInicial_avisaRegistrado() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        altasDeLaCuenta.add(new AltaVista(negocio, EstadoAlta.REGISTRADO, null));
        var yo = new Anotador();
        avisos.suscribir(cuenta, yo);

        avisos.alRecibir(membresia(negocio, cuenta, "DUENO_INICIAL"));

        assertThat(yo.recibidos).containsExactly(new AvisosDeAlta.Aviso(negocio, EstadoAlta.REGISTRADO, null));
        assertThat(yo.completo).isTrue();
    }

    @Test
    @DisplayName("el rechazo avisa RECHAZADO con su motivo")
    void rechazo_avisaRechazado() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        var yo = new Anotador();
        avisos.suscribir(cuenta, yo);

        avisos.alRecibir(rechazo(negocio, cuenta));

        assertThat(yo.recibidos).containsExactly(new AvisosDeAlta.Aviso(negocio, EstadoAlta.RECHAZADO,
                "GIRO_INVALIDO"));
        assertThat(yo.completo).isTrue();
    }

    @Test
    @DisplayName("solo se avisa a la cuenta que pidio el alta")
    void otraCuenta_noRecibeNada() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        altasDeLaCuenta.add(new AltaVista(negocio, EstadoAlta.REGISTRADO, null));
        var ajena = new Anotador();
        avisos.suscribir(UuidV7.generar(AHORA), ajena);

        avisos.alRecibir(membresia(negocio, cuenta, "DUENO_INICIAL"));

        assertThat(ajena.recibidos).isEmpty();
        assertThat(ajena.completo).isFalse();
    }

    @Test
    @DisplayName("una invitacion aceptada no es un alta: no avisa nada")
    void invitacion_noAvisa() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        altasDeLaCuenta.add(new AltaVista(negocio, EstadoAlta.REGISTRADO, null));
        var yo = new Anotador();
        avisos.suscribir(cuenta, yo);

        avisos.alRecibir(membresia(negocio, cuenta, "INVITACION"));

        assertThat(yo.recibidos).isEmpty();
    }

    @Test
    @DisplayName("si la alta no esta REGISTRADO en la base, la membresia no avisa: la verdad es la consulta")
    void sinAltaRegistrada_noAvisa() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        var yo = new Anotador();
        avisos.suscribir(cuenta, yo);

        avisos.alRecibir(membresia(negocio, cuenta, "DUENO_INICIAL"));

        assertThat(yo.recibidos).isEmpty();
    }

    @Test
    @DisplayName("una segunda suscripcion de la misma cuenta cierra la anterior (una por cuenta)")
    void segundaSuscripcion_reemplazaALaPrimera() {
        var cuenta = UuidV7.generar(AHORA);
        var primera = new Anotador();
        var segunda = new Anotador();

        avisos.suscribir(cuenta, primera);
        avisos.suscribir(cuenta, segunda);

        assertThat(primera.completo).isTrue();
        assertThat(segunda.completo).isFalse();
        assertThat(avisos.suscritas()).isEqualTo(1);
    }

    @Test
    @DisplayName("cancelar la suscripcion la quita y ya no recibe avisos")
    void cancelar_laQuita() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        var yo = new Anotador();
        var suscripcion = avisos.suscribir(cuenta, yo);

        suscripcion.close();
        avisos.alRecibir(rechazo(negocio, cuenta));

        assertThat(yo.recibidos).isEmpty();
        assertThat(avisos.suscritas()).isZero();
    }

    @Test
    @DisplayName("un suscriptor que falla no rompe al listener y se da de baja")
    void suscriptorQueFalla_noRompeElListener() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);
        avisos.suscribir(cuenta, new AvisosDeAlta.Suscriptor() {
            @Override
            public void aviso(AvisosDeAlta.Aviso aviso) {
                throw new IllegalStateException("socket roto");
            }

            @Override
            public void completo() {
            }
        });

        avisos.alRecibir(rechazo(negocio, cuenta));

        assertThat(avisos.suscritas()).isZero();
    }
}
