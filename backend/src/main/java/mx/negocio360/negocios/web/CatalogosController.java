package mx.negocio360.negocios.web;

import java.time.Duration;
import java.util.Objects;
import mx.negocio360.negocios.CatalogoVista;
import mx.negocio360.negocios.NegociosApi;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * R1: {@code GET /catalogos/categorias}. Publico y sin datos de nadie: la persona elige su giro antes de tener sesion y el
 * turista usa la misma lista para filtrar. Es una lista corta que casi nunca cambia, asi que lleva {@code ETag} y se
 * puede guardar en el celular. La tasa por IP (SG-10) llega con el filtro de entrada (IDN-4).
 */
@RestController
public class CatalogosController {

    private final NegociosApi negocios;

    public CatalogosController(NegociosApi negocios) {
        this.negocios = Objects.requireNonNull(negocios, "negocios");
    }

    @GetMapping("/catalogos/categorias")
    public ResponseEntity<CatalogoVista> categorias(WebRequest peticion) {
        var catalogo = negocios.catalogos();
        var etiqueta = "\"" + Integer.toHexString(catalogo.hashCode()) + "\"";
        if (peticion.checkNotModified(etiqueta)) {
            return null;
        }
        return ResponseEntity.ok().eTag(etiqueta)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(catalogo);
    }
}
