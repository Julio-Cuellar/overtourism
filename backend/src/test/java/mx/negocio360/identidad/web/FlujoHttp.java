package mx.negocio360.identidad.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Los pasos HTTP del registro y del acceso que repiten las pruebas de los controladores (D-84, D-85): formulario,
 * codigo que "llego" al correo (se lee del canal simulado), verificacion e inicio de sesion. Un solo contador reparte
 * celulares, IP y correos nuevos a todas las pruebas, porque comparten el contexto de Spring y con el sus topes.
 */
final class FlujoHttp {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(40000);

    private final MockMvc mvc;

    FlujoHttp(MockMvc mvc) {
        this.mvc = mvc;
    }

    static String telefono() {
        return String.format("22290%05d", SECUENCIA.incrementAndGet());
    }

    static String ipNueva() {
        var n = SECUENCIA.incrementAndGet();
        return "10.7." + (n / 250) + "." + (n % 250 + 1);
    }

    static String correoDe(String telefono) {
        return "u" + telefono + "@example.com";
    }

    static RequestPostProcessor desde(String ip) {
        return peticion -> {
            peticion.setRemoteAddr(ip);
            return peticion;
        };
    }

    static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    /** El cuerpo del formulario de registro; un campo nulo se manda como {@code null}. */
    static String formulario(String nombre, String apellidos, String correo, String telefono, String aviso) {
        return "{\"nombre\":" + texto(nombre) + ",\"apellidos\":" + texto(apellidos) + ",\"correo\":" + texto(correo)
                + ",\"telefono\":" + texto(telefono) + ",\"aceptaAviso\":" + aviso + "}";
    }

    /** Un formulario valido de Ana para ese celular, con un negocio si se da su giro. */
    static String formularioDe(String telefono, String giroDelNegocio) {
        var negocio = giroDelNegocio == null ? ""
                : ",\"negocio\":{\"nombre\":\"Fonda Doña Chela\",\"giro\":\"" + giroDelNegocio + "\"}";
        return "{\"nombre\":\"Ana\",\"apellidos\":\"López Pérez\",\"correo\":\"" + correoDe(telefono)
                + "\",\"telefono\":\"" + telefono + "\",\"aceptaAviso\":true" + negocio + "}";
    }

    ResultActions registrar(String json, String ip) throws Exception {
        return mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON).content(json).with(desde(ip)));
    }

    ResultActions verificar(String correo, String codigo, String ip) throws Exception {
        return mvc.perform(post("/registro/verificacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":" + texto(correo) + ",\"codigo\":" + texto(codigo) + "}").with(desde(ip)));
    }

    ResultActions reenviar(String correo, String ip) throws Exception {
        return mvc.perform(post("/registro/reenvio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":" + texto(correo) + "}").with(desde(ip)));
    }

    ResultActions pedirCodigo(String identificador, String ip) throws Exception {
        return mvc.perform(post("/auth/codigo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + identificador + "\"}").with(desde(ip)));
    }

    ResultActions iniciar(String identificador, String codigo, String ip) throws Exception {
        return mvc.perform(post("/auth/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + identificador + "\",\"codigo\":\"" + codigo + "\"}").with(desde(ip)));
    }

    /** El ultimo codigo que el canal simulado "mando" a ese correo. */
    String codigoDe(String correo) throws Exception {
        var cuerpo = mvc.perform(get("/dev/canal/codigo").param("correo", correo)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.codigo");
    }

    /** Los dos pasos del registro con un formulario valido; devuelve el cuerpo de la sesion que abre la verificacion. */
    String registrarCuenta(String telefono, String giroDelNegocio, String ip) throws Exception {
        registrar(formularioDe(telefono, giroDelNegocio), ip).andExpect(status().isAccepted());
        return verificar(correoDe(telefono), codigoDe(correoDe(telefono)), ip).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** Registra una cuenta nueva sin negocio y devuelve su token. */
    String tokenDeRegistro(String telefono, String ip) throws Exception {
        return JsonPath.read(registrarCuenta(telefono, null, ip), "$.token");
    }

    /** Entra a una cuenta que ya existe con ese identificador: pide el codigo, lo lee del canal y abre la sesion. */
    String tokenDeAcceso(String identificador, String correo, String ip) throws Exception {
        pedirCodigo(identificador, ip).andExpect(status().isAccepted());
        var cuerpo = iniciar(identificador, codigoDe(correo), ip).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }
}
