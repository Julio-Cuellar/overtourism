package mx.negocio360.clientes.aplicacion;

import java.util.List;
import mx.negocio360.clientes.dominio.FinalidadAviso;

/** Publicacion de una version nueva del aviso de privacidad del negocio (L-01, L-02). */
public record PublicarAvisoComando(String responsableNombre, String responsableDomicilio,
                                   List<FinalidadAviso> finalidades, String textoSimplificado,
                                   String textoIntegral, String contactoArco) implements Comando {
}
