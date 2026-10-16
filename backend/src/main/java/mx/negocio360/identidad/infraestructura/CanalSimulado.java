package mx.negocio360.identidad.infraestructura;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.dominio.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Canal que no envia nada: escribe el codigo en la consola y guarda en memoria el ultimo de cada correo, para que el
 * desarrollo y la demo puedan usarlo (AS-10, D-71, D-77). Es la unica excepcion a SI-15: escribe el codigo, pero en
 * un solo registro con nombre propio ({@link #NOMBRE_DEL_REGISTRO}) y con el correo enmascarado. SOLO DESARROLLO Y
 * DEMO: con el perfil {@code produccion} no se carga y hay que reemplazarlo por el canal real (IDN-DOC.1), que es otra
 * implementacion de {@link CanalDeCodigos}.
 */
public final class CanalSimulado implements CanalDeCodigos {

    /** Propiedad que hay que poner en {@code true} para que existan las piezas de desarrollo (SI-13). */
    public static final String PROPIEDAD_DE_HABILITACION = "identidad.canal-simulado.habilitado";

    /** Nombre del unico registro que puede llevar un codigo; las pruebas de SI-15 lo permiten solo a el. */
    public static final String NOMBRE_DEL_REGISTRO = "identidad.canal.simulado";

    private static final Logger REGISTRO = LoggerFactory.getLogger(NOMBRE_DEL_REGISTRO);

    private final Map<String, String> ultimoPorCorreo = new ConcurrentHashMap<>();

    @Override
    public void enviar(String destino, String codigo) {
        ultimoPorCorreo.put(destino, codigo);
        REGISTRO.info("[CANAL SIMULADO] Código de verificación para {}: {}", Correo.enmascarar(destino), codigo);
    }

    public Optional<String> ultimoCodigoDe(String correo) {
        return Optional.ofNullable(ultimoPorCorreo.get(correo));
    }
}
