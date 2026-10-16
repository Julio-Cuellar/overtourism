package mx.negocio360.identidad.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R5: Identidad lee {@code negocio.registrado} v1 con un lector tolerante; lo que no entiende lo ignora. */
class NegocioRegistradoV1Test {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final UUID NEGOCIO = UuidV7.generar(AHORA);
    private static final UUID CUENTA = UuidV7.generar(AHORA);

    private static SobreEvento sobre(String tipo, int version, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(AHORA), tipo, version, NEGOCIO, "negocios", AHORA, UuidV7.generar(AHORA),
                null, datos);
    }

    private static Map<String, Object> datosValidos() {
        return new HashMap<>(Map.of("solicitado_por", CUENTA.toString(), "nombre", "Fonda Doña Chela", "giro", "RESTAURANTE"));
    }

    @Test
    @DisplayName("un negocio.registrado v1 completo se lee con el negocio del sobre y los datos del contrato")
    void de_eventoValido_seLee() {
        var sobre = sobre("negocio.registrado", 1, datosValidos());

        var evento = NegocioRegistradoV1.de(sobre).orElseThrow();

        assertThat(evento.negocioId()).isEqualTo(NEGOCIO);
        assertThat(evento.solicitadoPor()).isEqualTo(CUENTA);
        assertThat(evento.nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(evento.giro()).isEqualTo("RESTAURANTE");
        assertThat(evento.eventoId()).isEqualTo(sobre.id());
        assertThat(evento.correlacionId()).isEqualTo(sobre.correlacionId());
    }

    @Test
    @DisplayName("solo vale el aviso que publica el modulo de negocios: otro origen se devuelve vacio")
    void de_otroOrigen_vacio() {
        var ahora = AHORA;
        var deOtro = new SobreEvento(UuidV7.generar(ahora), "negocio.registrado", 1, NEGOCIO, "clientes", ahora,
                UuidV7.generar(ahora), null, datosValidos());

        assertThat(NegocioRegistradoV1.de(deOtro)).isEmpty();
    }

    @Test
    @DisplayName("R5: los campos que el modulo de negocios agregue despues se ignoran")
    void de_camposNuevos_seIgnoran() {
        var datos = datosValidos();
        datos.put("direccion", "5 de Mayo 12");
        datos.put("etiquetas", java.util.List.of("centro"));

        assertThat(NegocioRegistradoV1.de(sobre("negocio.registrado", 1, datos))).isPresent();
    }

    @Test
    @DisplayName("otro tipo de evento u otra version no son suyos: se devuelven vacios")
    void de_otroTipoOVersion_vacio() {
        assertThat(NegocioRegistradoV1.de(sobre("negocio.cerrado", 1, datosValidos()))).isEmpty();
        assertThat(NegocioRegistradoV1.de(sobre("negocio.registrado", 2, datosValidos()))).isEmpty();
    }

    @Test
    @DisplayName("un evento mal formado (falta un campo, un id roto o un tipo equivocado) se descarta sin lanzar")
    void de_malFormado_vacio() {
        for (var campo : new String[] {"solicitado_por", "nombre", "giro"}) {
            var datos = datosValidos();
            datos.remove(campo);
            assertThat(NegocioRegistradoV1.de(sobre("negocio.registrado", 1, datos))).as("sin %s", campo).isEmpty();
        }
        var idRoto = datosValidos();
        idRoto.put("solicitado_por", "no-es-un-uuid");
        assertThat(NegocioRegistradoV1.de(sobre("negocio.registrado", 1, idRoto))).isEmpty();
        var numero = datosValidos();
        numero.put("nombre", 42);
        assertThat(NegocioRegistradoV1.de(sobre("negocio.registrado", 1, numero))).isEmpty();
    }
}
