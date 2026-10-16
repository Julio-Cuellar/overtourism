package mx.negocio360.negocios.web;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.MUTATION;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.SUBSCRIPTION;

import java.util.List;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.negocios.aplicacion.PermisosDeNegocios;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Negocios (SG-21) y lo que exige cada una (SG-03 y SG-04). El panel pide el
 * mismo permiso que ya exige su servicio: la puerta de la plataforma lo comprueba antes y el servicio vuelve a hacerlo,
 * dos barreras por si una falla. Solo hay dos formas sin negocio activo: lo que es de la cuenta (sus altas y el aviso de
 * alta, que existen justo cuando todavia no hay negocio) y la superficie publica del turista, que se publica a proposito
 * sobre vistas de solo lectura y no tiene sesion (D-98, D-99). Publicar una resena la manda el turista sin cuenta: es la
 * unica variante de comando publica, y el modulo comprueba el dispositivo y el modo demostracion (RS-09, RS-11, DM-03).
 */
@Component
public final class OperacionesDeNegocios implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(
                // Panel del locatario
                OperacionDeclarada.de(QUERY, "miNegocio", PermisosDeNegocios.PERFIL_VER),
                OperacionDeclarada.de(QUERY, "sucursales", PermisosDeNegocios.PERFIL_VER),
                OperacionDeclarada.de(QUERY, "horarioDeSucursal", PermisosDeNegocios.PERFIL_VER),
                OperacionDeclarada.de(QUERY, "resenas", PermisosDeNegocios.RESENA_VER),
                OperacionDeclarada.de(MUTATION, "actualizarNegocio", PermisosDeNegocios.PERFIL_EDITAR),
                OperacionDeclarada.de(MUTATION, "definirSubcategorias", PermisosDeNegocios.PERFIL_EDITAR),
                OperacionDeclarada.de(MUTATION, "definirAtributos", PermisosDeNegocios.PERFIL_EDITAR),
                OperacionDeclarada.de(MUTATION, "definirDisponibilidad", PermisosDeNegocios.DISPONIBILIDAD_EDITAR),
                OperacionDeclarada.de(MUTATION, "publicarNegocio", PermisosDeNegocios.NEGOCIO_PUBLICAR),
                OperacionDeclarada.de(MUTATION, "actualizarSucursal", PermisosDeNegocios.SUCURSAL_GESTIONAR),
                OperacionDeclarada.de(MUTATION, "definirHorario", PermisosDeNegocios.HORARIO_EDITAR),
                OperacionDeclarada.de(MUTATION, "agregarExcepcionHorario", PermisosDeNegocios.HORARIO_EDITAR),
                OperacionDeclarada.de(MUTATION, "quitarExcepcionHorario", PermisosDeNegocios.HORARIO_EDITAR),
                OperacionDeclarada.de(MUTATION, "responderResena", PermisosDeNegocios.RESENA_RESPONDER),
                // De la cuenta: existen cuando todavia no hay negocio activo
                OperacionDeclarada.deCuenta(QUERY, "misAltasDeNegocio"),
                OperacionDeclarada.deCuenta(SUBSCRIPTION, "altaDeNegocio"),
                // Superficie publica del turista (D-98, D-99): sin sesion, sobre vistas de solo lectura
                OperacionDeclarada.publica(QUERY, "negociosPublicos"),
                OperacionDeclarada.publica(QUERY, "negocioPublico"),
                OperacionDeclarada.publica(QUERY, "resenasDeNegocio"));
    }

    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(ComandoDeclarado.publico("publicarResena"));
    }
}
