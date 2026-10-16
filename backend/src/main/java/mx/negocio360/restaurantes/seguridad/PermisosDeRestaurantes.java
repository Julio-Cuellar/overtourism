package mx.negocio360.restaurantes.seguridad;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.springframework.stereotype.Component;

/**
 * Permisos que declara Restaurantes (AZ-02, D-70, seccion 11 del diseno). Los 18 de P1; el de analisis llega con P2.
 * Ninguno es sensible: un permiso sensible queda negado por completo hasta que exista la confirmacion reforzada (D-73,
 * IDN-3.2), y anular una linea ya preparada se protege con motivo, permiso y preparar → confirmar. Quien tiene cada
 * permiso lo deciden los roles de Identidad, que son datos (migracion V037).
 */
@Component
public final class PermisosDeRestaurantes implements CatalogoDePermisos {

    public static final String MENU_VER = "restaurantes.menu.ver";
    public static final String MENU_EDITAR = "restaurantes.menu.editar";
    public static final String MENU_AGOTAR = "restaurantes.menu.agotar";
    public static final String RECETA_VER = "restaurantes.receta.ver";
    public static final String RECETA_EDITAR = "restaurantes.receta.editar";
    public static final String COSTO_VER = "restaurantes.costo.ver";
    public static final String SALON_VER = "restaurantes.salon.ver";
    public static final String SALON_CONFIGURAR = "restaurantes.salon.configurar";
    public static final String SERVICIO_OPERAR = "restaurantes.servicio.operar";
    public static final String CUENTA_VER = "restaurantes.cuenta.ver";
    public static final String CUENTA_ABRIR = "restaurantes.cuenta.abrir";
    public static final String CUENTA_CANCELAR = "restaurantes.cuenta.cancelar";
    public static final String LINEA_ANULAR = "restaurantes.linea.anular";
    public static final String LINEA_ANULAR_PREPARADA = "restaurantes.linea.anular_preparada";
    public static final String COMANDA_VER = "restaurantes.comanda.ver";
    public static final String COMANDA_PREPARAR = "restaurantes.comanda.preparar";
    public static final String COMANDA_ENTREGAR = "restaurantes.comanda.entregar";
    public static final String CONSUMO_VER = "restaurantes.consumo.ver";

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado(MENU_VER, "Ver el menú y los platillos", false),
            new PermisoDeclarado(MENU_EDITAR, "Crear, editar, publicar y dar de baja platillos", false),
            new PermisoDeclarado(MENU_AGOTAR, "Marcar un platillo como agotado", false),
            new PermisoDeclarado(RECETA_VER, "Ver las recetas, sin costos", false),
            new PermisoDeclarado(RECETA_EDITAR, "Guardar recetas", false),
            new PermisoDeclarado(COSTO_VER, "Ver costos y márgenes de los platillos", false),
            new PermisoDeclarado(SALON_VER, "Ver las mesas y las alertas del salón", false),
            new PermisoDeclarado(SALON_CONFIGURAR, "Crear zonas y mesas y regenerar el QR de una mesa", false),
            new PermisoDeclarado(SERVICIO_OPERAR,
                    "Abrir y cerrar el servicio, bloquear mesas, pausar el QR y liberar una mesa", false),
            new PermisoDeclarado(CUENTA_VER, "Ver las cuentas abiertas", false),
            new PermisoDeclarado(CUENTA_ABRIR,
                    "Abrir cuentas, agregar líneas, enviar a cocina, pedir la cuenta y confirmar presencia", false),
            new PermisoDeclarado(CUENTA_CANCELAR, "Cancelar una cuenta con motivo", false),
            new PermisoDeclarado(LINEA_ANULAR, "Anular una línea con motivo mientras su comanda no esté lista", false),
            new PermisoDeclarado(LINEA_ANULAR_PREPARADA, "Anular una línea ya lista o entregada", false),
            new PermisoDeclarado(COMANDA_VER, "Ver el tablero de comandas", false),
            new PermisoDeclarado(COMANDA_PREPARAR, "Avanzar una comanda hasta lista", false),
            new PermisoDeclarado(COMANDA_ENTREGAR, "Marcar una comanda como entregada", false),
            new PermisoDeclarado(CONSUMO_VER, "Ver los consumos de insumos que fallaron", false));

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
