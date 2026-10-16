package mx.negocio360.clientes.graphql;

import java.util.List;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TipoDeOperacion;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Clientes (SG-21, G1 a G8 del mapa) y el permiso de cada una (SG-03). Los
 * permisos son los de {@code PermisosDeClientes}; los sensibles (ARCO, anonimizar, publicar el aviso) ademas exigen la
 * confirmacion reforzada. {@code avisoPrivacidadVigente} pide el permiso de registrar clientes: quien los da de alta
 * necesita el aviso para ponerlo a su disposicion (L-01); es una decision a confirmar.
 */
@Component
public final class OperacionesDeClientes implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        var consulta = TipoDeOperacion.QUERY;
        return List.of(
                OperacionDeclarada.de(consulta, "clientes", "clientes.lista.ver"),
                OperacionDeclarada.de(consulta, "cliente", "clientes.ficha.ver"),
                OperacionDeclarada.de(consulta, "historialCliente", "clientes.ficha.ver"),
                OperacionDeclarada.de(consulta, "consentimientosCliente", "clientes.ficha.ver"),
                OperacionDeclarada.de(consulta, "sugerirClientes", "clientes.sugerir"),
                OperacionDeclarada.de(consulta, "panoramaClientes", "clientes.panorama.ver"),
                OperacionDeclarada.de(consulta, "avisoPrivacidadVigente", "clientes.cliente.registrar"),
                OperacionDeclarada.de(consulta, "solicitudesArco", "clientes.arco.gestionar"));
    }

    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(
                new ComandoDeclarado("registrarCliente", "clientes.cliente.registrar"),
                new ComandoDeclarado("editarCliente", "clientes.cliente.editar"),
                new ComandoDeclarado("registrarConsentimiento", "clientes.consentimiento.registrar"),
                new ComandoDeclarado("anonimizarCliente", "clientes.cliente.anonimizar"),
                new ComandoDeclarado("publicarAvisoPrivacidad", "clientes.aviso.publicar"),
                new ComandoDeclarado("registrarSolicitudArco", "clientes.arco.gestionar"),
                new ComandoDeclarado("ampliarSolicitudArco", "clientes.arco.gestionar"),
                new ComandoDeclarado("resolverSolicitudArco", "clientes.arco.gestionar"));
    }
}
