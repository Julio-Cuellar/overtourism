package mx.negocio360.restaurantes.web;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.MUTATION;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;

import java.util.List;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Restaurantes (SG-21) y lo que exige cada una (SG-03 y SG-04). El panel pide el
 * mismo permiso que ya exige su servicio: la puerta de la plataforma lo comprueba antes y el servicio vuelve a hacerlo. Las
 * operaciones sin sesion son el menu publico del turista (D-170), de solo lectura, y las cuatro del comensal por QR (R6), que
 * resuelven el negocio por el token de la mesa. Las variantes
 * de comando ({@code guardarReceta}, {@code darDeBajaPlatillo}) viajan por el despachador de comandos de la plataforma
 * (D-125, D-209) y se declaran en {@link #comandos()}.
 */
@Component
public final class OperacionesDeRestaurantes implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(
                OperacionDeclarada.de(QUERY, "categoriasDeMenu", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "platillos", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "platillo", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "resumenDeMenu", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "consumosFallidos", PermisosDeRestaurantes.CONSUMO_VER),
                OperacionDeclarada.de(QUERY, "receta", PermisosDeRestaurantes.RECETA_VER),
                OperacionDeclarada.de(QUERY, "costoDeReceta", PermisosDeRestaurantes.COSTO_VER),
                // Salon (R4)
                OperacionDeclarada.de(QUERY, "salon", PermisosDeRestaurantes.SALON_VER),
                OperacionDeclarada.de(QUERY, "qrDeMesa", PermisosDeRestaurantes.SALON_CONFIGURAR),
                // Cuentas y tablero de cocina (R5)
                OperacionDeclarada.de(QUERY, "cuenta", PermisosDeRestaurantes.CUENTA_VER),
                OperacionDeclarada.de(QUERY, "cuentasAbiertas", PermisosDeRestaurantes.CUENTA_VER),
                OperacionDeclarada.de(QUERY, "cuentaParaCobro", PermisosDeRestaurantes.CUENTA_VER),
                OperacionDeclarada.de(QUERY, "comandas", PermisosDeRestaurantes.COMANDA_VER),
                // Superficie publica del turista: sin sesion, sobre lo publicado
                OperacionDeclarada.publica(QUERY, "menuPublico"),
                // El comensal por QR (R6): sin sesion; las unicas de Restaurantes que escriben sin ella (D-210), limitadas por token y dispositivo
                OperacionDeclarada.publica(QUERY, "mesaPorQr"),
                OperacionDeclarada.publica(QUERY, "estadoDePedidoQr"),
                OperacionDeclarada.publica(MUTATION, "pedirPorQr"),
                OperacionDeclarada.publica(MUTATION, "pedirCuentaPorQr"),
                OperacionDeclarada.de(MUTATION, "crearCategoria", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "editarCategoria", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "crearPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "editarPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "publicarPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "cambiarModoDeInventario", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "marcarAgotado", PermisosDeRestaurantes.MENU_AGOTAR),
                OperacionDeclarada.de(MUTATION, "desactivarReceta", PermisosDeRestaurantes.RECETA_EDITAR),
                OperacionDeclarada.de(MUTATION, "crearZona", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "darDeBajaZona", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "crearMesa", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "editarMesa", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "darDeBajaMesa", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "configurarModoDeServicio", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "regenerarQrDeMesa", PermisosDeRestaurantes.SALON_CONFIGURAR),
                OperacionDeclarada.de(MUTATION, "bloquearMesa", PermisosDeRestaurantes.SERVICIO_OPERAR),
                OperacionDeclarada.de(MUTATION, "abrirServicio", PermisosDeRestaurantes.SERVICIO_OPERAR),
                OperacionDeclarada.de(MUTATION, "cerrarServicio", PermisosDeRestaurantes.SERVICIO_OPERAR),
                OperacionDeclarada.de(MUTATION, "pausarQrDeMesa", PermisosDeRestaurantes.SERVICIO_OPERAR),
                OperacionDeclarada.de(MUTATION, "habilitarQr", PermisosDeRestaurantes.SERVICIO_OPERAR),
                OperacionDeclarada.de(MUTATION, "abrirCuenta", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "agregarLinea", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "quitarLineaPendiente", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "enviarACocina", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "pedirCuenta", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "reabrirCuenta", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "confirmarPresencia", PermisosDeRestaurantes.CUENTA_ABRIR),
                OperacionDeclarada.de(MUTATION, "anularLinea", PermisosDeRestaurantes.LINEA_ANULAR),
                OperacionDeclarada.de(MUTATION, "cancelarCuenta", PermisosDeRestaurantes.CUENTA_CANCELAR),
                // El servicio exige ademas restaurantes.linea.anular_preparada (CT-15)
                OperacionDeclarada.de(MUTATION, "liberarMesa", PermisosDeRestaurantes.SERVICIO_OPERAR),
                // Hasta lista pide preparar y a entregada, entregar (CD-02): la puerta pide ver y el servicio decide el paso
                OperacionDeclarada.de(MUTATION, "avanzarComanda", PermisosDeRestaurantes.COMANDA_VER),
                OperacionDeclarada.de(MUTATION, "devolverComanda", PermisosDeRestaurantes.COMANDA_PREPARAR));
    }

    /** Las variantes de {@code ComandoInput} de Restaurantes y el permiso que pide la puerta de la plataforma (SG-04). */
    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(
                new ComandoDeclarado("guardarReceta", PermisosDeRestaurantes.RECETA_EDITAR),
                new ComandoDeclarado("darDeBajaPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                new ComandoDeclarado("anularLineaPreparada", PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA),
                new ComandoDeclarado("cancelarCuentaPreparada", PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA));
    }
}
