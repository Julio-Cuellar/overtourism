package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.Disparador;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** C-01: al activar, el negocio recibe una copia del catalogo base y de las reglas por defecto. */
class ActivarContabilidadTest {

    private final Escenario e = new Escenario();

    private static String huellaCatalogo(java.util.List<Cuenta> cuentas) {
        return cuentas.stream().map(c -> c.codigo() + ":" + c.tipo() + ":" + c.naturaleza() + ":" + c.afectable())
                .sorted().toList().toString();
    }

    private String huellaReglas(ContextoNegocio contexto) {
        var porId = e.api.catalogo(contexto).stream()
                .collect(java.util.stream.Collectors.toMap(Cuenta::id, Cuenta::codigo));
        return e.api.reglas(contexto).stream()
                .map(r -> r.disparador() + "/" + r.condicion() + ":" + porId.get(r.cuentaCargoId()) + ">"
                        + porId.get(r.cuentaAbonoId()) + ":" + r.tasaIva())
                .sorted().toList().toString();
    }

    @Test
    @DisplayName("C-01: un restaurante, un museo y una miscelanea quedan con los mismos codigos y reglas")
    void activar_tresGiros_mismoCatalogoYReglas() {
        var restaurante = new ContextoNegocio(UUID.randomUUID(), "fonda");
        var museo = new ContextoNegocio(UUID.randomUUID(), "museo");
        var miscelanea = new ContextoNegocio(UUID.randomUUID(), "miscelanea");

        for (var contexto : java.util.List.of(restaurante, museo, miscelanea)) {
            e.confirmar(contexto, Fixtures.activar());
        }

        assertThat(huellaCatalogo(e.api.catalogo(museo))).isEqualTo(huellaCatalogo(e.api.catalogo(restaurante)));
        assertThat(huellaCatalogo(e.api.catalogo(miscelanea))).isEqualTo(huellaCatalogo(e.api.catalogo(restaurante)));
        assertThat(huellaReglas(museo)).isEqualTo(huellaReglas(restaurante));
        assertThat(huellaReglas(miscelanea)).isEqualTo(huellaReglas(restaurante));
    }

    @Test
    @DisplayName("C-01: el catalogo base trae las 11 cuentas afectables del diseno, cada una bajo su cuenta padre")
    void activar_catalogoBase_tieneLasCuentasDelDiseno() {
        e.activar(e.ctxA);

        var cuentas = e.api.catalogo(e.ctxA);
        var afectables = cuentas.stream().filter(Cuenta::afectable).map(Cuenta::codigo).sorted().toList();
        assertThat(afectables).containsExactly("101.01", "102.01", "103.01", "105.01", "109.01", "208.01", "209.01",
                "401.01", "402.01", "601.01", "704.01");
        var porId = cuentas.stream().collect(java.util.stream.Collectors.toMap(Cuenta::id, c -> c));
        for (var cuenta : cuentas.stream().filter(Cuenta::afectable).toList()) {
            var padre = porId.get(cuenta.padreId());
            assertThat(padre).as("padre de %s", cuenta.codigo()).isNotNull();
            assertThat(padre.afectable()).isFalse();
            assertThat(cuenta.codigo()).startsWith(padre.codigo() + ".");
        }
        assertThat(cuentas).allMatch(c -> c.negocioId().equals(e.negocioA));
    }

    @Test
    @DisplayName("C-01: las reglas por defecto usan la tasa y la fuente capturadas al activar, no un valor de memoria")
    void activar_reglasPorDefecto_usanLaTasaCapturada() {
        e.confirmar(e.ctxA, Fixtures.activar(new BigDecimal("0.08"), "Tasa de prueba, verificada el 2026-10-15"));

        var reglas = e.api.reglas(e.ctxA);
        var ventas = reglas.stream().filter(r -> r.disparador() == Disparador.VENTA).toList();
        assertThat(ventas).extracting(ReglaContable::condicion)
                .containsExactlyInAnyOrder("EFECTIVO", "TARJETA", "TRANSFERENCIA");
        assertThat(ventas).allSatisfy(r -> {
            assertThat(r.tasaIva()).isEqualByComparingTo("0.08");
            assertThat(r.fuente()).isEqualTo("Tasa de prueba, verificada el 2026-10-15");
            assertThat(r.cuentaImpuestoId()).isNotNull();
        });
        assertThat(reglas).extracting(ReglaContable::disparador)
                .contains(Disparador.CUPON, Disparador.CORTE_FALTANTE, Disparador.CORTE_SOBRANTE);
    }

    @Test
    @DisplayName("C-01: cada negocio tiene su propia copia; otro negocio no la ve")
    void activar_copiaPorNegocio() {
        e.activar(e.ctxA);

        assertThat(e.api.catalogo(e.ctxB)).isEmpty();
        assertThat(e.api.reglas(e.ctxB)).isEmpty();
    }

    @Test
    @DisplayName("C-01: una tasa invalida al activar falla con TASA_INVALIDA y no copia nada")
    void activar_tasaInvalida_falla() {
        fallaCon(() -> e.api.preparar(e.ctxA, Fixtures.activar(new BigDecimal("1.50"), "x")),
                CodigoError.TASA_INVALIDA);

        assertThat(e.api.catalogo(e.ctxA)).isEmpty();
    }

    @Test
    @DisplayName("C-01 y F-01: una tasa sin fuente falla con TASA_INVALIDA")
    void activar_tasaSinFuente_falla() {
        fallaCon(() -> e.api.preparar(e.ctxA, Fixtures.activar(new BigDecimal("0.16"), " ")),
                CodigoError.TASA_INVALIDA);
    }

    @Test
    @DisplayName("C-01: activar dos veces no duplica el catalogo")
    void activar_dosVeces_noDuplica() {
        e.activar(e.ctxA);
        var cuantas = e.api.catalogo(e.ctxA).size();

        e.activar(e.ctxA);

        assertThat(e.api.catalogo(e.ctxA)).hasSize(cuantas);
    }
}
