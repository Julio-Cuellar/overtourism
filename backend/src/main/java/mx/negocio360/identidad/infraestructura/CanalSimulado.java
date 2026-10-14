package mx.negocio360.identidad.infraestructura;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;

/**
 * Canal que no envia nada: guarda en memoria el ultimo codigo de cada telefono para que el desarrollo y la demo
 * puedan leerlo (AS-10, D-71). Nunca escribe el codigo en los registros (SI-15). SOLO DESARROLLO Y DEMO: con el perfil
 * {@code produccion} no se carga y hay que reemplazarlo por el canal real (IDN-DOC.1).
 */
public final class CanalSimulado implements CanalDeCodigos {

    /** Propiedad que hay que poner en {@code true} para que existan las piezas de desarrollo (SI-13). */
    public static final String PROPIEDAD_DE_HABILITACION = "identidad.canal-simulado.habilitado";

    private final Map<String, String> ultimoPorTelefono = new ConcurrentHashMap<>();

    @Override
    public void enviar(String telefono, String codigo) {
        ultimoPorTelefono.put(telefono, codigo);
    }

    public Optional<String> ultimoCodigoDe(String telefono) {
        return Optional.ofNullable(ultimoPorTelefono.get(telefono));
    }
}
