package mx.negocio360.compartido.graphql.comandos;

import java.util.Map;
import java.util.Set;
import mx.negocio360.compartido.graphql.EntradaGraphQl;

/**
 * Un modulo que atiende variantes de {@code ComandoInput} (D-55, D-125). La plataforma tiene un unico dueno de
 * {@code prepararComando} y {@code confirmarComando} ({@link ComandosGraphQlController}) porque GraphQL admite un solo
 * resolvedor por campo; cada modulo se registra con este contrato y el despachador reparte por variante al preparar y
 * por el prefijo del token al confirmar.
 *
 * <p>La puerta de la plataforma ya comprobo el permiso de la <b>variante</b> antes de llegar aqui (SG-04); el servicio del
 * modulo vuelve a exigir los permisos del comando, tambien al confirmar y antes de gastar el token (D-126).
 */
public interface ModuloDeComandos {

    /**
     * Prefijo de los tokens de este modulo, por ejemplo {@code "ventas."}. El despachador lo antepone al preparar y lo
     * quita al confirmar. Un solo modulo puede declarar el prefijo vacio: es el heredado de Negocios, cuyos tokens
     * existian sin prefijo; los tokens que no tienen un punto son suyos (el alfabeto de un token nunca lleva punto).
     *
     * @return prefijo terminado en punto, o vacio
     */
    String prefijo();

    /**
     * Las variantes de {@code ComandoInput} que atiende. Ninguna puede repetirse entre modulos.
     *
     * @return nombres de las variantes
     */
    Set<String> variantes();

    /**
     * Si sus comandos se confirman sin cuenta ni negocio activo. Solo lo pide Negocios, por la resena del turista; los
     * demas exigen sesion con negocio activo para preparar y para confirmar. Cada modulo lo declara de forma explicita.
     *
     * @return verdadero si el token de confirmacion basta, sin sesion
     */
    boolean confirmaSinSesion();

    /**
     * Prepara una variante.
     *
     * @param entrada  sesion de la peticion (negocio, permisos, dispositivo)
     * @param variante la unica variante con valor
     * @param campos   sus campos, tal como llegaron
     * @return token <b>sin prefijo</b>, resumen, vencimiento y avisos
     */
    PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos);

    /**
     * Confirma un comando preparado.
     *
     * @param entrada       sesion de la peticion
     * @param tokenSinPrefijo token tal como lo devolvio {@link #preparar}
     * @return resultado, con los errores de dominio como excepciones de negocio del modulo
     */
    ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo);
}
