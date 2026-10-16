package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

/**
 * SG-20: una linea de registro por operacion con quien, en que negocio, que operacion y como salio. Nunca el texto de la
 * consulta, ni las variables, ni el nombre de la operacion que escribe el cliente (podria traer saltos de linea para
 * falsificar renglones o datos personales).
 */
@SpringBootTest
class RegistroDeOperacionesTest {

    private static final String LOGGER = "auditoria.graphql";
    private static final Pattern LINEA = Pattern.compile(
            "cuenta=(\\S+) negocio=(\\S+) operacion=(\\S+) resultado=(OK|ERROR) errores=(\\S+) ms=(\\d+)");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-1111-7000-8000-00000000a001");

    @Autowired
    private WebGraphQlHandler manejador;

    @AfterEach
    void limpiar() {
        SesionDePrueba.salir();
    }

    private static void entraComo() {
        SesionDePrueba.entrar(NEGOCIO);
    }

    private List<String> ejecutar(String consulta, String nombre, Map<String, Object> variables) {
        try (var captura = new CapturaDeLogs(LOGGER)) {
            var cuerpo = new java.util.HashMap<String, Object>();
            cuerpo.put("query", consulta);
            cuerpo.put("variables", variables);
            if (nombre != null) {
                cuerpo.put("operationName", nombre);
            }
            var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(),
                    new LinkedMultiValueMap<>(), null, SesionDePrueba.atributos(), cuerpo, "prueba", Locale.ROOT);
            manejador.handleRequest(peticion).block();
            return captura.lineas();
        }
    }

    private static java.util.regex.Matcher unica(List<String> lineas) {
        assertThat(lineas).hasSize(1);
        var coincidencia = LINEA.matcher(lineas.get(0));
        assertThat(coincidencia.find()).as("formato de la linea: %s", lineas.get(0)).isTrue();
        return coincidencia;
    }

    @Test
    @DisplayName("una operacion que sale bien: cuenta, negocio, tipo y campos de la raiz, resultado OK y duracion")
    void operacionExitosa_registraQuienQueYComoSalio() {
        entraComo();

        var linea = unica(ejecutar("{ arbolDePrueba { valor } lentoDePrueba(milisegundos: 1) }", null, Map.of()));

        assertThat(linea.group(1)).isEqualTo(SesionDePrueba.CUENTA.toString());
        assertThat(linea.group(2)).isEqualTo(NEGOCIO.toString());
        assertThat(linea.group(3)).isEqualTo("query:arbolDePrueba,lentoDePrueba");
        assertThat(linea.group(4)).isEqualTo("OK");
        assertThat(linea.group(5)).isEqualTo("-");
        assertThat(Long.parseLong(linea.group(6))).isBetween(0L, 5_000L);
    }

    @Test
    @DisplayName("una peticion anonima (la app del turista) se registra sin cuenta ni negocio, y sin el id de dispositivo")
    void anonima_seRegistraSinCuentaNiNegocio() {
        SesionDePrueba.entrarAnonimo("0192f39f-3333-7000-8000-00000000c001");

        var lineas = ejecutar("{ arbolDePrueba { valor } }", null, Map.of());

        var linea = unica(lineas);
        assertThat(linea.group(1)).isEqualTo("-");
        assertThat(linea.group(2)).isEqualTo("-");
        assertThat(lineas.get(0)).doesNotContain("0192f39f-3333");
    }

    @Test
    @DisplayName("una sesion que todavia no eligio negocio se registra con su cuenta y sin negocio")
    void sinNegocio_seRegistraConLaCuentaSola() {
        SesionDePrueba.entrarSinNegocio();

        var linea = unica(ejecutar("{ arbolDePrueba { valor } }", null, Map.of()));

        assertThat(linea.group(1)).isEqualTo(SesionDePrueba.CUENTA.toString());
        assertThat(linea.group(2)).isEqualTo("-");
    }

    @Test
    @DisplayName("se registra el campo de la raiz, no el alias que escribio el cliente")
    void alias_noSeRegistra() {
        entraComo();

        var lineas = ejecutar("{ miAliasUnico: arbolDePrueba { valor } }", null, Map.of());

        assertThat(unica(lineas).group(3)).isEqualTo("query:arbolDePrueba");
        assertThat(lineas.get(0)).doesNotContain("miAliasUnico");
    }

    @Test
    @DisplayName("nunca se registran el texto de la consulta, las variables ni el nombre de la operacion")
    void datosDelCliente_nuncaSeRegistran() {
        entraComo();
        var nombreMalicioso = "MiConsulta\nINFO cuenta=falsa negocio=falso operacion=falsa resultado=OK errores=- ms=0";

        var lineas = ejecutar("query MiConsulta($secreto: String) { arbolDePrueba { valor } }", nombreMalicioso,
                Map.of("secreto", "ABC123-sensible"));

        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0)).doesNotContain("ABC123", "sensible", "MiConsulta", "falsa", "\n");
    }

    @Test
    @DisplayName("un error del resolvedor se registra con su codigo, no con su mensaje")
    void errorDeResolvedor_seRegistraConElCodigo() {
        entraComo();

        var linea = unica(ejecutar("{ fallaInternaDePrueba }", null, Map.of()));

        assertThat(linea.group(3)).isEqualTo("query:fallaInternaDePrueba");
        assertThat(linea.group(4)).isEqualTo("ERROR");
        assertThat(linea.group(5)).isEqualTo("ERROR_INTERNO");
    }

    @Test
    @DisplayName("varios errores distintos se listan una vez cada uno")
    void erroresVarios_seListanSinRepetir() {
        entraComo();

        var linea = unica(ejecutar("{ a: permisoDenegadoDePrueba b: permisoDenegadoDePrueba c: fallaInternaDePrueba }",
                null, Map.of()));

        assertThat(linea.group(5)).isEqualTo("PERMISO_DENEGADO,ERROR_INTERNO");
    }

    @Test
    @DisplayName("un limite excedido se registra con la operacion que se pidio y su codigo")
    void limiteExcedido_seRegistra() {
        entraComo();

        var linea = unica(ejecutar("{ arbolDePrueba { hijo { hijo { hijo { hijo { hijo { hijo { valor } } } } } } } }",
                null, Map.of()));

        assertThat(linea.group(3)).isEqualTo("query:arbolDePrueba");
        assertThat(linea.group(5)).isEqualTo("CONSULTA_MUY_PROFUNDA");
    }

    @Test
    @DisplayName("una consulta que no valida no se ejecuta: operacion no_ejecutada y CONSULTA_INVALIDA")
    void consultaInvalida_seRegistraComoNoEjecutada() {
        entraComo();

        var lineas = ejecutar("{ campoSecreto }", null, Map.of());

        var linea = unica(lineas);
        assertThat(linea.group(3)).isEqualTo("no_ejecutada");
        assertThat(linea.group(4)).isEqualTo("ERROR");
        assertThat(linea.group(5)).isEqualTo("CONSULTA_INVALIDA");
        assertThat(lineas.get(0)).doesNotContain("campoSecreto");
    }

    @Test
    @DisplayName("una consulta demasiado larga se registra sin su texto")
    void consultaMuyLarga_seRegistraSinElTexto() {
        entraComo();

        var lineas = ejecutar("{ arbolDePrueba { valor } }" + " ".repeat(10_001), null, Map.of());

        var linea = unica(lineas);
        assertThat(linea.group(3)).isEqualTo("no_ejecutada");
        assertThat(linea.group(5)).isEqualTo("CONSULTA_MUY_LARGA");
        assertThat(lineas.get(0)).doesNotContain("arbolDePrueba");
    }

    @Test
    @DisplayName("sin sesion la cuenta y el negocio se registran como guion")
    void sinSesion_cuentaYNegocioSonGuion() {
        var linea = unica(ejecutar("{ arbolDePrueba { valor } }", null, Map.of()));

        assertThat(linea.group(1)).isEqualTo("-");
        assertThat(linea.group(2)).isEqualTo("-");
    }
}
