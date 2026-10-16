package mx.negocio360.compartido.graphql;

import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.seguridad.ConfirmacionReforzada;
import mx.negocio360.compartido.seguridad.ConfirmacionRequeridaException;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;

/**
 * SG-03, SG-04 y SG-19: la puerta de autorizacion de cada operacion. Antes de ejecutar un campo de la raiz comprueba, en
 * este orden: que haya sesion con negocio activo; que la tabla conozca el campo (si no, lo niega: falla cerrado, aunque
 * el arranque ya lo habria detectado); que la persona tenga el permiso, que para un comando es el de su variante; y, si
 * el permiso es sensible, que la confirmacion reforzada lo confirme. El resolvedor no se ejecuta si algo falla. Los errores
 * salen sin decir que permiso faltaba, y el de confirmacion solo despues de comprobar el permiso, para no revelar que
 * existe un flujo a quien no tiene acceso a el.
 *
 * <p>Solo se vigilan los campos de la raiz: la autorizacion es de la operacion, no de cada campo del arbol de respuesta.
 * Cada campo negado falla por su cuenta, asi que una consulta con varios puede responder parcialmente.
 */
public final class AutorizacionDeOperacionesInstrumentation extends SimplePerformantInstrumentation {

    private final TablaDeOperaciones tabla;
    private final ConfirmacionReforzada confirmacion;

    public AutorizacionDeOperacionesInstrumentation(TablaDeOperaciones tabla, ConfirmacionReforzada confirmacion) {
        this.tabla = Objects.requireNonNull(tabla, "tabla");
        this.confirmacion = Objects.requireNonNull(confirmacion, "confirmacion");
    }

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> original, InstrumentationFieldFetchParameters parametros,
                                                InstrumentationState estado) {
        var paso = parametros.getExecutionStepInfo();
        if (paso.getPath().getLevel() != 1) {
            return original;
        }
        var tipo = tipoDeRaiz(parametros.getExecutionContext().getGraphQLSchema(), paso.getObjectType().getName());
        if (tipo == null) {
            return original;
        }
        var campo = paso.getFieldDefinition().getName();
        return entorno -> {
            autorizar(tipo, campo, entorno);
            return original.get(entorno);
        };
    }

    private static TipoDeOperacion tipoDeRaiz(GraphQLSchema esquema, String nombreDelTipo) {
        if (esquema.getQueryType() != null && esquema.getQueryType().getName().equals(nombreDelTipo)) {
            return TipoDeOperacion.QUERY;
        }
        if (esquema.getMutationType() != null && esquema.getMutationType().getName().equals(nombreDelTipo)) {
            return TipoDeOperacion.MUTATION;
        }
        if (esquema.getSubscriptionType() != null && esquema.getSubscriptionType().getName().equals(nombreDelTipo)) {
            return TipoDeOperacion.SUBSCRIPTION;
        }
        return null;
    }

    private void autorizar(TipoDeOperacion tipo, String campo, DataFetchingEnvironment entorno) {
        var entrada = entradaDe(entorno);
        if (campo.startsWith("__")) {
            entrada.exigirContexto();
            return;
        }
        var declarada = tabla.operacion(tipo, campo);
        if (declarada.isEmpty()) {
            // Lo que la tabla no conoce se niega; sin sesion se dice lo mismo que para lo protegido, para no revelar nada.
            entrada.exigirContexto();
            throw new PermisoDenegadoException(campo);
        }
        var operacion = declarada.get();
        var permiso = operacion.permiso();
        var acceso = operacion.acceso();
        if (operacion.argumentoDeComando() != null) {
            var variante = varianteDe(entorno.getArgument(operacion.argumentoDeComando()));
            if (variante != null && tabla.esVariantePublica(variante)) {
                acceso = Acceso.PUBLICA;
            } else {
                // Una variante sin unica, o que la tabla no conoce, solo se puede negar a quien tiene sesion con negocio.
                entrada.exigirContexto();
                permiso = variante == null ? null : tabla.permisoDeVariante(variante).orElse(null);
                if (permiso == null) {
                    throw new PermisoDenegadoException(variante == null ? "comando sin una variante unica" : variante);
                }
            }
        }
        if (acceso == Acceso.PUBLICA) {
            return;
        }
        if (acceso == Acceso.CUENTA) {
            entrada.exigirCuenta();
            return;
        }
        var contexto = entrada.exigirContexto();
        if (permiso == null) {
            return;
        }
        if (!entrada.autorizacion().puede(permiso)) {
            throw new PermisoDenegadoException(permiso);
        }
        if (tabla.sensible(permiso) && !confirmacion.confirmada(entrada.exigirCuenta(), contexto.negocioId(), permiso)) {
            throw new ConfirmacionRequeridaException(permiso);
        }
    }

    /**
     * La entrada de la operacion: la que dejaron en el contexto de GraphQL los interceptores de entrada (el de HTTP y el de
     * WebSocket). Esta puerta corre antes de que Spring restaure ningun estado en el hilo, por eso no puede depender de el.
     * Sin entrada en el contexto la peticion es anonima y la puerta niega: falla cerrado.
     */
    private static EntradaGraphQl entradaDe(DataFetchingEnvironment entorno) {
        Object delContexto = entorno.getGraphQlContext().get(EntradaGraphQl.CLAVE);
        return delContexto instanceof EntradaGraphQl entrada ? entrada : EntradaGraphQl.anonima(null);
    }

    /** La unica variante con valor del comando, o nulo; el tipo {@code @oneOf} ya lo garantiza, pero no se confia en ello. */
    private static String varianteDe(Object entrada) {
        if (entrada instanceof Map<?, ?> mapa) {
            var presentes = mapa.entrySet().stream().filter(e -> e.getValue() != null).map(e -> String.valueOf(e.getKey()))
                    .toList();
            if (presentes.size() == 1) {
                return presentes.get(0);
            }
        }
        return null;
    }
}
