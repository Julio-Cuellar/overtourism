package mx.negocio360.negocios.aplicacion.puertos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.dominio.ExcepcionHorario;
import mx.negocio360.negocios.dominio.ResumenCalificacion;
import mx.negocio360.negocios.dominio.SemanaHorario;

/**
 * Lectura de la vista publica {@code negocios.v_negocio_publico}, con el rol de solo lectura (SN-14). Solo ve negocios
 * publicados y activos.
 */
public interface RepositorioPublico {

    /** Lo que pide una busqueda: el texto ya viene limpio y los topes aplicados. */
    record Criterio(String texto, String giro, List<String> subcategorias, List<String> dieta,
                    List<String> accesibilidad, Double latitud, Double longitud, Double radioKm,
                    boolean ordenarPorDistancia) {
    }

    record Etiqueta(String codigo, String nombre) {
    }

    record SubcategoriaPublica(String codigo, String nombre, String giro) {
    }

    /** Una fila de la vista, con sus listas ya leidas. */
    record Fila(UUID id, String nombre, String giro, String giroNombre, String descripcion, String telefono,
                String correo, String zonaHoraria, boolean simulado, Integer duracionVisitaMin, Integer costoMedioMxn,
                String calle, String numero, String colonia, String municipio, String entidadFederativa,
                String codigoPostal, String referencias, BigDecimal latitud, BigDecimal longitud,
                List<SubcategoriaPublica> subcategorias, List<Etiqueta> dieta, List<Etiqueta> accesibilidad,
                SemanaHorario semana, List<ExcepcionHorario> excepciones, String disponibilidadNivel,
                String disponibilidadNota, Instant disponibilidadDeclaradaEn, Instant disponibilidadVenceEn,
                ResumenCalificacion calificacion, Double distanciaKm) {
    }

    Optional<Fila> porId(UUID id);

    /** Una pagina de la busqueda, a partir de {@code desplazamiento} filas. */
    List<Fila> buscar(Criterio criterio, int limite, int desplazamiento);
}
