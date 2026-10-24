package mx.negocio360.negocios.web;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.graphql.comandos.PreparacionVista;
import mx.negocio360.compartido.graphql.comandos.ResultadoVista;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import org.springframework.stereotype.Component;

/**
 * La variante {@code publicarResena} de {@code ComandoInput} (RS-09) para el despachador de la plataforma (D-125). Antes la
 * resolvia {@code PublicoGraphQlController}; el comportamiento es el mismo: el autor es el id de dispositivo del encabezado
 * {@code X-Dispositivo-Id} (RS-11), sin cuenta, y los errores de la confirmacion salen como resultado con codigo y texto
 * fijos (D-23). Es el unico modulo que confirma sin sesion, y el unico con el prefijo vacio: sus tokens existian sin
 * prefijo y siguen siendolo.
 */
@Component
public final class ComandosDeNegocios implements ModuloDeComandos {

    private final ResenasPublicasApi resenas;

    /**
     * Arma el modulo de comandos.
     *
     * @param resenas API publica de reseñas
     */
    public ComandosDeNegocios(ResenasPublicasApi resenas) {
        this.resenas = resenas;
    }

    @Override
    public String prefijo() {
        return "";
    }

    @Override
    public Set<String> variantes() {
        return Set.of("publicarResena");
    }

    @Override
    public boolean confirmaSinSesion() {
        return true;
    }

    @Override
    public PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos) {
        var comentario = campos.get("comentario") instanceof String c ? c : null;
        var preparada = resenas.prepararResena(entrada.dispositivo(), uuid(campos.get("negocioId")),
                entero(campos.get("calificacion")), comentario);
        return new PreparacionVista(preparada.token(), preparada.resumen(), preparada.expiraEn(),
                preparada.advertencias());
    }

    /** Los errores de la confirmacion salen como resultado con codigo y texto fijos, como pide el contrato (D-23). */
    @Override
    public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
        try {
            var publicada = resenas.confirmarResena(entrada.dispositivo(), tokenSinPrefijo);
            return new ResultadoVista(true, "OK", "Tu reseña quedó publicada.", publicada.resenaId(), java.util.List.of());
        } catch (NegociosException e) {
            return ResultadoVista.fallo(e.codigo().name(), e.codigo().mensaje());
        }
    }

    private static UUID uuid(Object valor) {
        try {
            return UUID.fromString(String.valueOf(valor));
        } catch (IllegalArgumentException e) {
            throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
        }
    }

    private static int entero(Object valor) {
        if (valor instanceof Number n) {
            return n.intValue();
        }
        throw new NegociosException(CodigoError.CALIFICACION_INVALIDA);
    }
}
