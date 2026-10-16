package mx.negocio360.negocios.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R6 (SN-15): las rutas de administracion de reseñas viven en un controlador aparte, solo existen si hay un token de
 * servicio configurado (D-108: el servicio de administracion es otro proyecto) y exigen ese token en cada llamada.
 */
@SpringBootTest(properties = {"negocios.demo.activo=true", "negocios.admin.token=token-de-servicio-de-prueba"})
@AutoConfigureMockMvc
class AdministracionControllerTest {

    private static final String RUTA = "/admin/negocios/{negocio}/resenas/{resena}/ocultar";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private NegociosApi panel;
    @Autowired
    private ResenasPublicasApi resenas;
    @Autowired
    private AltasApi altas;

    /** Un negocio publico con una reseña: devuelve el negocio y la reseña. */
    private UUID[] negocioConResena() {
        var ahora = java.time.Instant.parse("2026-10-16T16:00:00Z");
        var pedido = new NegocioRegistroSolicitadoV1(UuidV7.generar(ahora), UuidV7.generar(ahora), "Fonda Zq" + UUID
                .randomUUID().toString().substring(0, 8), "RESTAURANTE", UuidV7.generar(ahora), UuidV7.generar(ahora));
        altas.registrarAlta(pedido);
        var contexto = new ContextoNegocio(pedido.negocioId(), "dueno");
        var todo = Autorizacion.de(java.util.Set.of("negocios.perfil.ver", "negocios.perfil.editar",
                "negocios.sucursal.gestionar", "negocios.horario.editar", "negocios.negocio.publicar"));
        var sucursal = panel.sucursales(contexto, todo).get(0);
        panel.actualizarSucursal(contexto, todo, sucursal.id(), new ActualizarSucursal(0, null, null, null, null, null,
                null, null, null, new BigDecimal("19.04"), new BigDecimal("-98.19"), null));
        panel.definirHorario(contexto, todo, sucursal.id(), 0,
                List.of(new DiaHorarioComando(4, List.of(new TramoComando("08:00", "17:00")))));
        panel.publicarNegocio(contexto, todo, true);
        var dispositivo = UUID.randomUUID().toString();
        var resena = resenas.confirmarResena(dispositivo,
                resenas.prepararResena(dispositivo, pedido.negocioId(), 5, "x").token()).resenaId();
        return new UUID[] {pedido.negocioId(), resena};
    }

    @Test
    @DisplayName("sin el token de servicio, o con uno equivocado, responde 401 y no oculta nada")
    void sinToken_o_tokenMalo_401() throws Exception {
        var ids = negocioConResena();

        mvc.perform(post(RUTA, ids[0], ids[1]).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA, ids[0], ids[1]).header("X-Servicio-Admin", "otro-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA, ids[0], ids[1]).header("Authorization", "Bearer token-de-servicio-de-prueba")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"X\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(resenas.resenasDeNegocio(ids[0], 20, null).resenas()).hasSize(1);
    }

    @Test
    @DisplayName("con el token correcto oculta la reseña y la deja fuera de lo publico; restituir la regresa")
    void conToken_oculta_yRestituye() throws Exception {
        var ids = negocioConResena();

        mvc.perform(post(RUTA, ids[0], ids[1]).header("X-Servicio-Admin", "token-de-servicio-de-prueba")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"CONTENIDO_INDEBIDO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("OCULTA"))
                .andExpect(jsonPath("$.id").value(ids[1].toString()));
        assertThat(resenas.resenasDeNegocio(ids[0], 20, null).resenas()).isEmpty();

        mvc.perform(post("/admin/negocios/{negocio}/resenas/{resena}/restituir", ids[0], ids[1])
                .header("X-Servicio-Admin", "token-de-servicio-de-prueba"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"));
        assertThat(resenas.resenasDeNegocio(ids[0], 20, null).resenas()).hasSize(1);
    }

    @Test
    @DisplayName("una reseña que no existe da 404 con su codigo y sin repetir lo que se mando")
    void inexistente_404() throws Exception {
        var ids = negocioConResena();

        mvc.perform(post(RUTA, ids[0], UUID.randomUUID()).header("X-Servicio-Admin", "token-de-servicio-de-prueba")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"X\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RESENA_NO_ENCONTRADA"));
    }
}
