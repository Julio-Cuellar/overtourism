package mx.negocio360.clientes.dominio;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso de privacidad de un negocio, por version (L-01, L-02). El contenido no se edita: un cambio crea una version
 * nueva y cierra la anterior con {@link #cerradoEn(Instant)}.
 *
 * <p>La validacion es estructural: comprueba que esten las partes que pide el art. 15 y que quepan en la tabla. No
 * puede juzgar si el texto es juridicamente suficiente: eso lo revisa una persona con formacion juridica.
 */
public record AvisoPrivacidad(UUID id, UUID negocioId, int version, String responsableNombre,
                              String responsableDomicilio, List<FinalidadAviso> finalidades,
                              String textoSimplificado, String textoIntegral, String contactoArco,
                              Instant vigenteDesde, Instant vigenteHasta, String publicadoPor) {

    public static final int MAX_RESPONSABLE = 120;
    public static final int MAX_DOMICILIO = 200;
    public static final int MAX_CONTACTO_ARCO = 200;
    public static final int MAX_CODIGO_FINALIDAD = 60;
    public static final int MAX_FINALIDADES = 20;
    public static final int MAX_TEXTO_SIMPLIFICADO = 2_000;
    public static final int MAX_TEXTO_INTEGRAL = 20_000;

    public AvisoPrivacidad {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        if (version < 1) {
            throw new IllegalArgumentException("La version del aviso debe ser 1 o mayor: " + version);
        }
        responsableNombre = texto(responsableNombre, MAX_RESPONSABLE, false);
        responsableDomicilio = texto(responsableDomicilio, MAX_DOMICILIO, false);
        finalidades = finalidadesValidas(finalidades);
        textoSimplificado = texto(textoSimplificado, MAX_TEXTO_SIMPLIFICADO, true);
        textoIntegral = texto(textoIntegral, MAX_TEXTO_INTEGRAL, true);
        contactoArco = texto(contactoArco, MAX_CONTACTO_ARCO, false);
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(publicadoPor, "publicadoPor");
        if (vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            throw new IllegalArgumentException("Un aviso no puede cerrarse antes de empezar");
        }
    }

    /** Aviso recien publicado: vigente desde {@code ahora} y sin fecha de cierre. */
    public static AvisoPrivacidad nuevo(UUID id, UUID negocioId, int version, String responsableNombre,
                                        String responsableDomicilio, List<FinalidadAviso> finalidades,
                                        String textoSimplificado, String textoIntegral, String contactoArco,
                                        String publicadoPor, Instant ahora) {
        return new AvisoPrivacidad(id, negocioId, version, responsableNombre, responsableDomicilio, finalidades,
                textoSimplificado, textoIntegral, contactoArco, ahora, null, publicadoPor);
    }

    public boolean vigente() {
        return vigenteHasta == null;
    }

    /** La misma version, cerrada porque otra la reemplaza. Solo cambia la fecha de cierre. */
    public AvisoPrivacidad cerradoEn(Instant cierre) {
        return new AvisoPrivacidad(id, negocioId, version, responsableNombre, responsableDomicilio, finalidades,
                textoSimplificado, textoIntegral, contactoArco, vigenteDesde, cierre, publicadoPor);
    }

    /** Los textos largos admiten saltos de linea y tabulaciones; el resto rechaza todo caracter prohibido. */
    private static String texto(String valor, int maximo, boolean permitirSaltos) {
        var recortado = CaracteresSeguros.recortar(valor);
        if (recortado == null || recortado.length() > maximo
                || CaracteresSeguros.contieneProhibidos(recortado, permitirSaltos)) {
            throw new ClientesException(CodigoError.AVISO_INCOMPLETO);
        }
        return recortado;
    }

    private static List<FinalidadAviso> finalidadesValidas(List<FinalidadAviso> finalidades) {
        if (finalidades == null || finalidades.isEmpty() || finalidades.size() > MAX_FINALIDADES) {
            throw new ClientesException(CodigoError.AVISO_INCOMPLETO);
        }
        var vistos = new HashSet<String>();
        for (var finalidad : finalidades) {
            var codigo = finalidad == null ? null : finalidad.codigo();
            if (codigo == null || codigo.isEmpty() || codigo.length() > MAX_CODIGO_FINALIDAD
                    || CaracteresSeguros.contieneProhibidos(codigo, false)
                    || !vistos.add(codigo.toLowerCase(Locale.ROOT))) {
                throw new ClientesException(CodigoError.AVISO_INCOMPLETO);
            }
        }
        return List.copyOf(finalidades);
    }
}
