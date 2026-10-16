package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.AtributosVista;
import mx.negocio360.negocios.CalificacionVista;
import mx.negocio360.negocios.CercaDe;
import mx.negocio360.negocios.DiaVista;
import mx.negocio360.negocios.DisponibilidadPublicaVista;
import mx.negocio360.negocios.EtiquetaVista;
import mx.negocio360.negocios.ExcepcionVista;
import mx.negocio360.negocios.FiltroPublico;
import mx.negocio360.negocios.HorarioPublicoVista;
import mx.negocio360.negocios.NegocioPublicoVista;
import mx.negocio360.negocios.NegociosPublicosApi;
import mx.negocio360.negocios.OrdenPublico;
import mx.negocio360.negocios.PaginaPublica;
import mx.negocio360.negocios.SubcategoriaVista;
import mx.negocio360.negocios.TramoVista;
import mx.negocio360.negocios.UbicacionPublicaVista;
import mx.negocio360.negocios.VistaDeNegocio;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPublico;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.HorarioSucursal;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.ResumenCalificacion;

/**
 * Superficie publica de lectura (seccion 9.7): no exige sesion y solo ve lo que dice la vista publica. Aplica los topes
 * (PU-05) en codigo, calcula «abierto ahora» y la vigencia de la disponibilidad en el servidor (PU-06, DI-03, DI-04) y
 * trata igual a un negocio no publico que a uno inexistente (PU-01, SG-03). Java puro.
 */
public final class PublicoServicio implements NegociosPublicosApi {

    private static final int MAXIMO_DE_ELEMENTOS_EN_FILTRO = 10;
    private static final int MAXIMO_DE_CODIGO = 40;
    private static final int FILAS_POR_LOTE = 50;
    private static final int MAXIMO_DE_LOTES = 20;

    private final RepositorioPublico publico;
    private final RepositorioNegocios negocios;
    private final Clock reloj;

    public PublicoServicio(RepositorioPublico publico, RepositorioNegocios negocios, Clock reloj) {
        this.publico = Objects.requireNonNull(publico, "publico");
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Optional<NegocioPublicoVista> negocioPublico(UUID negocioId) {
        return publico.porId(Objects.requireNonNull(negocioId, "negocioId")).map(this::vista);
    }

    @Override
    public PaginaPublica negociosPublicos(FiltroPublico filtro, int primeros, String despues) {
        var f = filtro == null ? FiltroPublico.todos() : filtro;
        var porPagina = Math.max(1, Math.min(primeros, LimitesPublicos.POR_PAGINA_MAXIMA));
        var criterio = criterio(f);
        var desplazamiento = CursorDeDesplazamiento.leer(despues);
        var soloAbiertos = Boolean.TRUE.equals(f.abiertoAhora());

        var elegidos = new ArrayList<NegocioPublicoVista>();
        var posiciones = new ArrayList<Integer>();
        var posicion = desplazamiento;
        var agotado = false;
        for (var lote = 0; lote < MAXIMO_DE_LOTES && elegidos.size() <= porPagina && !agotado; lote++) {
            var filas = publico.buscar(criterio, Math.max(FILAS_POR_LOTE, porPagina + 1), posicion);
            agotado = filas.size() < Math.max(FILAS_POR_LOTE, porPagina + 1);
            for (var fila : filas) {
                var vista = vista(fila);
                if (!soloAbiertos || vista.horario().abiertoAhora()) {
                    elegidos.add(vista);
                    posiciones.add(posicion);
                }
                posicion++;
                if (elegidos.size() > porPagina) {
                    break;
                }
            }
        }
        var hayMas = elegidos.size() > porPagina;
        var pagina = hayMas ? elegidos.subList(0, porPagina) : elegidos;
        var siguiente = hayMas ? posiciones.get(porPagina) : null;
        return new PaginaPublica(List.copyOf(pagina), hayMas, siguiente == null ? null : CursorDeDesplazamiento.codificar(siguiente));
    }

    @Override
    public Optional<VistaDeNegocio> vistaDeNegocio(UUID negocioId) {
        var negocio = negocios.negocio(Objects.requireNonNull(negocioId, "negocioId"));
        if (negocio.isEmpty()) {
            return Optional.empty();
        }
        var principal = negocios.principal(negocioId);
        return Optional.of(new VistaDeNegocio(negocioId, negocio.get().nombre(), negocio.get().giro(),
                negocio.get().estado().name(), negocio.get().zonaHoraria(),
                principal.map(p -> p.latitud()).orElse(null), principal.map(p -> p.longitud()).orElse(null)));
    }

    // ------------------------------------------------------------------------------------------------- apoyo

    private RepositorioPublico.Criterio criterio(FiltroPublico f) {
        String texto = null;
        if (f.texto() != null && !f.texto().isBlank()) {
            texto = f.texto().strip();
            if (texto.length() > LimitesPublicos.TEXTO_MAXIMO || texto.codePoints().anyMatch(Character::isISOControl)) {
                throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
            }
        }
        var giro = f.giro() == null || f.giro().isBlank() ? null : codigo(f.giro());
        Double latitud = null;
        Double longitud = null;
        Double radio = null;
        CercaDe cerca = f.cerca();
        if (cerca != null) {
            if (!(cerca.radioKm() > 0 && cerca.radioKm() <= LimitesPublicos.RADIO_MAXIMO_KM)
                    || !(Math.abs(cerca.latitud()) <= 90) || !(Math.abs(cerca.longitud()) <= 180)) {
                throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
            }
            latitud = cerca.latitud();
            longitud = cerca.longitud();
            radio = cerca.radioKm();
        }
        return new RepositorioPublico.Criterio(texto, giro, codigos(f.subcategorias()), codigos(f.dieta()),
                codigos(f.accesibilidad()), latitud, longitud, radio,
                f.orden() == OrdenPublico.DISTANCIA && cerca != null);
    }

    private static List<String> codigos(List<String> codigos) {
        if (codigos == null) {
            return List.of();
        }
        if (codigos.size() > MAXIMO_DE_ELEMENTOS_EN_FILTRO) {
            throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
        }
        return codigos.stream().map(PublicoServicio::codigo).distinct().toList();
    }

    private static String codigo(String codigo) {
        if (codigo == null || codigo.length() > MAXIMO_DE_CODIGO || !codigo.matches("[A-Za-z0-9_]+")) {
            throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
        }
        return codigo.toUpperCase(java.util.Locale.ROOT);
    }

    static CalificacionVista calificacion(ResumenCalificacion r) {
        return new CalificacionVista(r.promedio(), r.total(), r.distribucion());
    }

    private NegocioPublicoVista vista(RepositorioPublico.Fila f) {
        var ahora = reloj.instant();
        var horario = new HorarioSucursal(f.semana(), f.excepciones());
        var abierto = horario.abiertoEn(ahora, ZoneId.of(f.zonaHoraria()));
        var semana = f.semana().dias().stream().filter(d -> !d.tramos().isEmpty())
                .map(d -> new DiaVista(d.dia(), d.tramos().stream().map(t -> new TramoVista(
                        HorarioSucursal.texto(t.abre()), HorarioSucursal.texto(t.cierra()))).toList())).toList();
        var excepciones = f.excepciones().stream().map(e -> new ExcepcionVista(null, e.fecha(), e.cerrado(),
                HorarioSucursal.texto(e.abre()), HorarioSucursal.texto(e.cierra()), e.motivo())).toList();
        // DI-03 y DI-04: vencida o con el negocio cerrado ahora, sale como sin dato; nunca como libre.
        var disponibilidad = f.disponibilidadNivel() != null && abierto && f.disponibilidadVenceEn() != null
                && ahora.isBefore(f.disponibilidadVenceEn())
                ? new DisponibilidadPublicaVista(f.disponibilidadNivel(), f.disponibilidadNota(),
                        f.disponibilidadDeclaradaEn()) : null;
        return new NegocioPublicoVista(f.id(), f.nombre(), f.giro(), f.giroNombre(), f.descripcion(),
                new UbicacionPublicaVista(f.calle(), f.numero(), f.colonia(), f.municipio(), f.entidadFederativa(),
                        f.codigoPostal(), f.referencias(), f.latitud(), f.longitud()),
                new HorarioPublicoVista(semana, excepciones, abierto, f.zonaHoraria()), f.telefono(), f.correo(),
                f.subcategorias().stream().map(s -> new SubcategoriaVista(s.codigo(), s.nombre(), s.giro(),
                        !s.giro().equals(f.giro()))).toList(),
                new AtributosVista(f.dieta().stream().map(e -> new EtiquetaVista(e.codigo(), e.nombre())).toList(),
                        f.accesibilidad().stream().map(e -> new EtiquetaVista(e.codigo(), e.nombre())).toList(),
                        f.duracionVisitaMin(), f.costoMedioMxn()),
                disponibilidad, calificacion(f.calificacion()),
                f.distanciaKm() == null ? null : Math.round(f.distanciaKm() * 100.0) / 100.0, f.simulado());
    }
}
