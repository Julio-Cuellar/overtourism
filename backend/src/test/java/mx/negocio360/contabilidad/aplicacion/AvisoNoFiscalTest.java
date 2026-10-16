package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.YearMonth;
import java.util.Arrays;
import mx.negocio360.contabilidad.ContabilidadApi;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F-03: Contabilidad no emite CFDI ni calcula declaraciones; sus reportes son apoyo para la persona contadora y lo
 * dicen. Hoy el unico reporte es la balanza; la exportacion CSV espera la pregunta 8 del diseno.
 */
class AvisoNoFiscalTest {

    @Test
    @DisplayName("F-03: la balanza trae el aviso de que no es herramienta fiscal")
    void balanza_traeElAviso() {
        var e = new Escenario();
        e.activar(e.ctxA);

        var balanza = e.api.balanza(e.ctxA, YearMonth.from(Fixtures.HOY));

        assertThat(balanza.aviso()).isEqualTo(AvisoFiscal.TEXTO);
        assertThat(AvisoFiscal.TEXTO).contains("no emite CFDI", "no calcula declaraciones", "persona contadora");
    }

    @Test
    @DisplayName("F-03: la API publica no tiene operaciones de facturas ni declaraciones")
    void api_sinOperacionesFiscales() {
        var nombres = Arrays.stream(ContabilidadApi.class.getMethods()).map(Method::getName)
                .map(String::toLowerCase).toList();

        assertThat(nombres).noneMatch(n -> n.contains("cfdi") || n.contains("factura") || n.contains("declaracion")
                || n.contains("timbr"));
    }
}
