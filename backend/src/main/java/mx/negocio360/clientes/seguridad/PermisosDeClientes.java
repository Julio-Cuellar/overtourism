package mx.negocio360.clientes.seguridad;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.springframework.stereotype.Component;

/**
 * Permisos que declara Clientes (AZ-02, D-70). Los derechos del responsable (publicar el aviso, anonimizar, atender
 * solicitudes ARCO y exportar los datos de una persona) son sensibles: ademas del permiso piden una confirmacion
 * reforzada (D-73). Quien tiene cada permiso lo deciden los roles de Identidad, que son datos.
 */
@Component
public final class PermisosDeClientes implements CatalogoDePermisos {

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado("clientes.sugerir", "Buscar clientes al vender", false),
            new PermisoDeclarado("clientes.lista.ver", "Ver la lista de clientes", false),
            new PermisoDeclarado("clientes.panorama.ver", "Ver los conteos de clientes", false),
            new PermisoDeclarado("clientes.ficha.ver", "Abrir la ficha completa de un cliente", false),
            new PermisoDeclarado("clientes.cliente.registrar", "Registrar clientes", false),
            new PermisoDeclarado("clientes.cliente.editar", "Editar clientes", false),
            new PermisoDeclarado("clientes.consentimiento.registrar", "Registrar o revocar consentimientos", false),
            new PermisoDeclarado("clientes.aviso.publicar", "Publicar el aviso de privacidad", true),
            new PermisoDeclarado("clientes.cliente.anonimizar", "Anonimizar a un cliente", true),
            new PermisoDeclarado("clientes.arco.gestionar", "Atender solicitudes ARCO", true),
            new PermisoDeclarado("clientes.datos.exportar", "Exportar los datos de un cliente", true));

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
