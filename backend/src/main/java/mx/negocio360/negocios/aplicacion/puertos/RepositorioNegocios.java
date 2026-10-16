package mx.negocio360.negocios.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.dominio.Disponibilidad;
import mx.negocio360.negocios.dominio.Negocio;
import mx.negocio360.negocios.dominio.Sucursal;

/**
 * El negocio y lo que cuelga de el: subcategorias, atributos y disponibilidad. Todo se hace en el negocio del contexto:
 * la base no muestra otro (SN-02). Las ediciones llevan la version esperada y fallan con {@code CONCURRENCIA} (NG-10).
 */
public interface RepositorioNegocios {

    /** Los codigos de dieta y de accesibilidad que eligio el negocio, en el orden del catalogo. */
    record AtributosElegidos(List<String> dieta, List<String> accesibilidad) {

        public AtributosElegidos {
            dieta = List.copyOf(dieta);
            accesibilidad = List.copyOf(accesibilidad);
        }
    }

    /** Guarda un negocio nuevo con su sucursal principal en la misma transaccion (NG-08). */
    void crear(Negocio negocio, Sucursal principal);

    Optional<Negocio> negocio(UUID id);

    Optional<Sucursal> principal(UUID negocioId);

    /**
     * Guarda los campos editables del negocio (perfil, publicacion y atributos numericos) y sube la version, si la
     * actual es {@code versionEsperada}. @throws mx.negocio360.negocios.dominio.NegociosException CONCURRENCIA
     */
    void actualizar(Negocio nuevo, long versionEsperada);

    /** Los codigos de las subcategorias elegidas, en el orden del catalogo. */
    List<String> subcategorias(UUID negocioId);

    /** Reemplaza el conjunto completo de subcategorias. */
    void reemplazarSubcategorias(UUID negocioId, List<String> codigos, Instant ahora);

    AtributosElegidos atributos(UUID negocioId);

    /** Reemplaza el conjunto completo de dieta y accesibilidad. */
    void reemplazarAtributos(UUID negocioId, List<String> dieta, List<String> accesibilidad, Instant ahora);

    Optional<Disponibilidad> disponibilidad(UUID negocioId);

    /** Crea o reemplaza la disponibilidad del negocio. */
    void guardarDisponibilidad(Disponibilidad disponibilidad);

    /** Quita la disponibilidad declarada (restablecimiento de la demo, DM-04). */
    void borrarDisponibilidad(UUID negocioId);

    /** Quien cambio que: el nombre de los campos, nunca sus valores (SN-05). */
    void registrarHistorial(UUID negocioId, String accion, List<String> campos, String actor, Instant ahora);
}
