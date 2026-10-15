package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** J-01: cada caja.cerrada crea un corte contable; diferencia = efectivo contado - efectivo esperado. */
class CorteContableTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @Test
    @DisplayName("J-01: esperado 1,850.00 y contado 1,834.50 dan un corte con diferencia -15.50")
    void corte_registraLaDiferencia() {
        var corteId = UUID.randomUUID();
        var sesion = UUID.randomUUID();

        var resultado = e.api.contabilizarCorte(Eventos.corte(e.negocioA, corteId, sesion, "1850.00", "1834.50",
                null, Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(Resultado.APLICADO);
        var corte = e.api.cortes(e.ctxA).get(0);
        assertThat(corte.corteId()).isEqualTo(corteId);
        assertThat(corte.cajaSesionId()).isEqualTo(sesion);
        assertThat(corte.efectivoEsperado()).isEqualByComparingTo("1850.00");
        assertThat(corte.efectivoContado()).isEqualByComparingTo("1834.50");
        assertThat(corte.diferencia()).isEqualByComparingTo("-15.50");
        assertThat(corte.cerradaEn()).isEqualTo(Fixtures.AHORA);
    }

    @Test
    @DisplayName("J-01 y E-02: el mismo corte en otro evento queda OMITIDO con DUPLICADO_NATURAL")
    void corte_repetido_unSoloCorte() {
        var corteId = UUID.randomUUID();
        var sesion = UUID.randomUUID();
        e.api.contabilizarCorte(Eventos.corte(e.negocioA, corteId, sesion, "100.00", "100.00", null,
                Fixtures.AHORA));

        var segundo = e.api.contabilizarCorte(Eventos.corte(e.negocioA, corteId, sesion, "100.00", "100.00", null,
                Fixtures.AHORA));

        assertThat(segundo.motivo()).isEqualTo("DUPLICADO_NATURAL");
        assertThat(e.api.cortes(e.ctxA)).hasSize(1);
    }

    @Test
    @DisplayName("J-01 y E-04: un corte sin efectivo contado queda pendiente con DATOS_INVALIDOS")
    void corte_datosInvalidos_quedaPendiente() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("corte_id", UUID.randomUUID().toString());
        datos.put("caja_sesion_id", UUID.randomUUID().toString());
        datos.put("efectivo_esperado", "100.00");
        datos.put("cerrada_en", Fixtures.AHORA.toString());

        var resultado = e.api.contabilizarCorte(Eventos.sobre("caja.cerrada", 1, e.negocioA, "caja", Fixtures.AHORA,
                UUID.randomUUID(), datos));

        assertThat(resultado.motivo()).isEqualTo("DATOS_INVALIDOS");
        assertThat(e.api.cortes(e.ctxA)).isEmpty();
    }

    @Test
    @DisplayName("J-01: los cortes de un negocio no se ven desde otro")
    void corte_otroNegocio_noLoVe() {
        e.activar(e.ctxB);
        e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), UUID.randomUUID(), "1.00", "1.00", null,
                Fixtures.AHORA));

        assertThat(e.api.cortes(e.ctxB)).isEmpty();
    }
}
