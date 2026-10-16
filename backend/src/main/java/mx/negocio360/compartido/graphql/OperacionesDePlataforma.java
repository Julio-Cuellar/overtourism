package mx.negocio360.compartido.graphql;

import java.util.List;

/**
 * Los campos de GraphQL que son de la plataforma y no de un modulo: el par {@code prepararComando} y
 * {@code confirmarComando} (D-55). El permiso de cada comando lo decide su variante, que declara el modulo dueño;
 * confirmar es publica porque algunos comandos los manda quien no tiene cuenta (la resena del turista): el token de
 * confirmacion esta ligado a quien lo preparo (la cuenta o el dispositivo) y al comando, y lo comprueba el modulo dueño
 * del comando; un comando que pide permiso exige su sesion para prepararse y para confirmarse en su manejador.
 */
public final class OperacionesDePlataforma implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(OperacionDeclarada.porComando(TipoDeOperacion.MUTATION, "prepararComando", "input"),
                OperacionDeclarada.publica(TipoDeOperacion.MUTATION, "confirmarComando"));
    }
}
