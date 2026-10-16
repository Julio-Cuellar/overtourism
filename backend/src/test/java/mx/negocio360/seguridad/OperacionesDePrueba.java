package mx.negocio360.seguridad;

import java.util.List;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TipoDeOperacion;
import org.springframework.stereotype.Component;

/**
 * Solo pruebas: declara los campos de {@code prueba-seguridad.graphqls}, igual que un modulo declara los suyos. Sin
 * esto la aplicacion de pruebas no arrancaria: un campo sin declarar impide el arranque (SG-21).
 */
@Component
class OperacionesDePrueba implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        var q = TipoDeOperacion.QUERY;
        return List.of(OperacionDeclarada.deSesion(q, "contextoDePrueba"), OperacionDeclarada.deSesion(q, "arbolDePrueba"),
                OperacionDeclarada.deSesion(q, "lentoDePrueba"), OperacionDeclarada.deSesion(q, "fallaInternaDePrueba"),
                OperacionDeclarada.deSesion(q, "permisoDenegadoDePrueba"),
                OperacionDeclarada.deSesion(q, "noAutenticadoDePrueba"),
                OperacionDeclarada.deSesion(q, "sinNegocioDePrueba"), OperacionDeclarada.deSesion(q, "uuidDePrueba"),
                OperacionDeclarada.deSesion(q, "asincronoQueFallaDePrueba"),
                OperacionDeclarada.deSesion(q, "errorDeNegocioDePrueba"), OperacionDeclarada.deSesion(q, "nuloDePrueba"),
                OperacionDeclarada.de(q, "soloConPermisoDePrueba", "clientes.sugerir"),
                OperacionDeclarada.de(q, "sensibleDePrueba", "clientes.cliente.anonimizar"),
                OperacionDeclarada.porComando(TipoDeOperacion.MUTATION, "comandoDePrueba", "input"),
                OperacionDeclarada.deSesion(TipoDeOperacion.SUBSCRIPTION, "eventosDePrueba"),
                OperacionDeclarada.de(TipoDeOperacion.SUBSCRIPTION, "sensibleSuscripcionDePrueba",
                        "clientes.cliente.anonimizar"),
                OperacionDeclarada.deSesion(TipoDeOperacion.SUBSCRIPTION, "suscripcionQueFallaDePrueba"));
    }
}
