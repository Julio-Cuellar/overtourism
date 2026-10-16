package mx.negocio360.restaurantes.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Optional;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.contratos.NegocioActualizadoV1;
import mx.negocio360.restaurantes.contratos.NegocioEstadoCambiadoV1;
import mx.negocio360.restaurantes.contratos.NegocioRegistradoV1;
import mx.negocio360.restaurantes.dominio.Restaurante;

/**
 * Lo que Restaurantes sabe de su negocio (DM-01, PB-01): crea su extension al escuchar {@code negocio.registrado} de un
 * negocio de giro RESTAURANTE y sigue su publicacion ({@code negocio.actualizado}) y su estado
 * ({@code negocio.estado_cambiado}). Es idempotente por evento (R3), no se deja pisar por un evento mas viejo (no hay orden
 * global) y nunca lanza por datos malos.
 */
public final class SeguimientoDeNegocios {

    private static final String CONSUMIDOR = "restaurantes.negocios";
    private static final String ORIGEN = "negocios";
    private static final String GIRO = "RESTAURANTE";

    private final RepositorioRestaurantes restaurantes;
    private final ObjectMapper mapeador = MapeadorEventos.crear();

    /**
     * Arma el seguimiento.
     *
     * @param restaurantes extension de restaurante y eventos procesados
     */
    public SeguimientoDeNegocios(RepositorioRestaurantes restaurantes) {
        this.restaurantes = Objects.requireNonNull(restaurantes, "restaurantes");
    }

    /**
     * Procesa un evento del bus.
     *
     * @param evento sobre recibido
     * @return que se hizo con el
     */
    public ResultadoSeguimiento procesar(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        var tipo = evento.tipo();
        if (!NegocioRegistradoV1.TIPO.equals(tipo) && !NegocioActualizadoV1.TIPO.equals(tipo)
                && !NegocioEstadoCambiadoV1.TIPO.equals(tipo)) {
            return ResultadoSeguimiento.IGNORADO;
        }
        if (restaurantes.yaProcesado(CONSUMIDOR, evento.id())) {
            return ResultadoSeguimiento.YA_PROCESADO;
        }
        var resultado = switch (tipo) {
            case NegocioRegistradoV1.TIPO -> leer(evento, NegocioRegistradoV1.class)
                    .map(v -> alRegistrarse(evento, v)).orElse(ResultadoSeguimiento.DATOS_INVALIDOS);
            case NegocioActualizadoV1.TIPO -> leer(evento, NegocioActualizadoV1.class)
                    .map(v -> alActualizarse(evento, v)).orElse(ResultadoSeguimiento.DATOS_INVALIDOS);
            default -> leer(evento, NegocioEstadoCambiadoV1.class)
                    .map(v -> alCambiarElEstado(evento, v)).orElse(ResultadoSeguimiento.DATOS_INVALIDOS);
        };
        if (resultado == ResultadoSeguimiento.APLICADO) {
            restaurantes.marcarProcesado(CONSUMIDOR, evento.id());
        }
        return resultado;
    }

    private ResultadoSeguimiento alRegistrarse(SobreEvento evento, NegocioRegistradoV1 v) {
        if (!GIRO.equals(v.giro())) {
            return ResultadoSeguimiento.IGNORADO;
        }
        var existente = restaurantes.porNegocio(evento.negocioId());
        if (existente.isPresent()) {
            return evento.ocurridoEn().isBefore(existente.get().actualizadoEn()) ? ResultadoSeguimiento.OBSOLETO
                    : ResultadoSeguimiento.IGNORADO;
        }
        restaurantes.guardar(Restaurante.nuevo(evento.negocioId(), evento.ocurridoEn()));
        return ResultadoSeguimiento.APLICADO;
    }

    private ResultadoSeguimiento alActualizarse(SobreEvento evento, NegocioActualizadoV1 v) {
        if (!GIRO.equals(v.giro())) {
            return ResultadoSeguimiento.IGNORADO;
        }
        var actual = restaurantes.porNegocio(evento.negocioId())
                .orElseGet(() -> Restaurante.nuevo(evento.negocioId(), evento.ocurridoEn()));
        if (evento.ocurridoEn().isBefore(actual.actualizadoEn())) {
            return ResultadoSeguimiento.OBSOLETO;
        }
        restaurantes.guardar(actual.conPublicado(v.publicado(), evento.ocurridoEn()));
        return ResultadoSeguimiento.APLICADO;
    }

    private ResultadoSeguimiento alCambiarElEstado(SobreEvento evento, NegocioEstadoCambiadoV1 v) {
        Optional<Restaurante> actual = restaurantes.porNegocio(evento.negocioId());
        if (actual.isEmpty()) {
            return ResultadoSeguimiento.IGNORADO;
        }
        if (evento.ocurridoEn().isBefore(actual.get().actualizadoEn())) {
            return ResultadoSeguimiento.OBSOLETO;
        }
        restaurantes.guardar(actual.get().conEstado(v.estadoNuevo(), evento.ocurridoEn()));
        return ResultadoSeguimiento.APLICADO;
    }

    /** Lee los datos con el contrato v1; vacio si no los publico Negocios o no cumplen el contrato. */
    private <T> Optional<T> leer(SobreEvento evento, Class<T> contrato) {
        if (!ORIGEN.equals(evento.origen())) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapeador.convertValue(evento.datos(), contrato));
        } catch (IllegalArgumentException contratoRoto) {
            return Optional.empty();
        }
    }
}
