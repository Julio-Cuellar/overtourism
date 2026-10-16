package mx.negocio360.negocios.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios.AtributosElegidos;
import mx.negocio360.negocios.dominio.Disponibilidad;
import mx.negocio360.negocios.dominio.ExcepcionHorario;
import mx.negocio360.negocios.dominio.HorarioSucursal;
import mx.negocio360.negocios.dominio.Negocio;
import mx.negocio360.negocios.dominio.Resena;
import mx.negocio360.negocios.dominio.SemanaHorario;
import mx.negocio360.negocios.dominio.Sucursal;

/**
 * Arma los eventos de Negocios (seccion 8.3). Ningun evento lleva datos personales ni el contenido de lo que se escribio
 * (EV-01): solo ids, codigos, nombres de campos y los datos publicos del negocio. Los campos opcionales que no aplican
 * se omiten (R5). Los decimales viajan como cadena.
 */
final class FabricaEventos {

    static final String ACTUALIZADO = "negocio.actualizado";
    static final String SUCURSAL_CAMBIADA = "negocio.sucursal_cambiada";
    static final String HORARIO_CAMBIADO = "negocio.horario_cambiado";
    static final String RESENA_PUBLICADA = "negocio.resena_publicada";
    static final String RESENA_RESPONDIDA = "negocio.resena_respondida";
    static final String DISPONIBILIDAD_ACTUALIZADA = "negocio.disponibilidad_actualizada";

    private FabricaEventos() {
    }

    /** B8: cambio el perfil, las subcategorias, los atributos o la publicacion. */
    static SobreEvento actualizado(Negocio negocio, List<String> campos, List<String> subcategorias,
                                   AtributosElegidos atributos, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("campos", List.copyOf(campos));
        datos.put("nombre", negocio.nombre());
        datos.put("giro", negocio.giro());
        datos.put("subcategorias", List.copyOf(subcategorias));
        datos.put("publicado", negocio.publicado());
        datos.put("dieta", atributos.dieta());
        datos.put("accesibilidad", atributos.accesibilidad());
        if (negocio.duracionVisitaMin() != null) {
            datos.put("duracion_visita_min", negocio.duracionVisitaMin());
        }
        if (negocio.costoMedioMxn() != null) {
            datos.put("costo_medio_mxn", negocio.costoMedioMxn());
        }
        return FabricaSobres.de(negocio.id(), ahora, UuidV7.generar(ahora), null, ACTUALIZADO, 1, datos);
    }

    /** B10: cambio la ubicacion de una sucursal. La direccion no viaja; si, el municipio y las coordenadas. */
    static SobreEvento sucursalCambiada(Sucursal sucursal, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("sucursal_id", sucursal.id().toString());
        datos.put("accion", "ACTUALIZADA");
        datos.put("es_principal", sucursal.esPrincipal());
        datos.put("activa", sucursal.activa());
        if (sucursal.municipio() != null) {
            datos.put("municipio", sucursal.municipio());
        }
        if (sucursal.entidadFederativa() != null) {
            datos.put("entidad_federativa", sucursal.entidadFederativa());
        }
        if (sucursal.tieneUbicacion()) {
            datos.put("latitud", sucursal.latitud().stripTrailingZeros().toPlainString());
            datos.put("longitud", sucursal.longitud().stripTrailingZeros().toPlainString());
        }
        return FabricaSobres.de(sucursal.negocioId(), ahora, UuidV7.generar(ahora), null, SUCURSAL_CAMBIADA, 1, datos);
    }

    /** B11: cambio el horario semanal o una excepcion. Las excepciones son las de ayer en adelante. */
    static SobreEvento horarioCambiado(UUID negocioId, UUID sucursalId, String zonaHoraria, SemanaHorario semana,
                                       List<ExcepcionHorario> excepciones, LocalDate hoy, Instant ahora) {
        var dias = new ArrayList<Object>();
        for (var dia : semana.dias()) {
            var tramos = dia.tramos().stream().map(t -> (Object) Map.of("abre", HorarioSucursal.texto(t.abre()),
                    "cierra", HorarioSucursal.texto(t.cierra()))).toList();
            dias.add(Map.of("dia", dia.dia(), "tramos", tramos));
        }
        var proximas = new ArrayList<Object>();
        for (var e : excepciones) {
            if (e.fecha().isBefore(hoy.minusDays(1))) {
                continue;
            }
            var fila = new LinkedHashMap<String, Object>();
            fila.put("fecha", e.fecha().toString());
            fila.put("cerrado", e.cerrado());
            if (!e.cerrado()) {
                fila.put("abre", HorarioSucursal.texto(e.abre()));
                fila.put("cierra", HorarioSucursal.texto(e.cierra()));
            }
            proximas.add(fila);
        }
        var datos = new LinkedHashMap<String, Object>();
        datos.put("sucursal_id", sucursalId.toString());
        datos.put("zona_horaria", zonaHoraria);
        datos.put("semana", dias);
        datos.put("excepciones", proximas);
        return FabricaSobres.de(negocioId, ahora, UuidV7.generar(ahora), null, HORARIO_CAMBIADO, 1, datos);
    }

    /** B15: cambio el nivel de disponibilidad. La nota no viaja: es texto libre de una persona. */
    static SobreEvento disponibilidadActualizada(Disponibilidad nueva, Disponibilidad.Nivel anterior, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        if (anterior != null) {
            datos.put("nivel_anterior", anterior.name());
        }
        datos.put("nivel_nuevo", nueva.nivel().name());
        datos.put("fuente", nueva.fuente().name());
        datos.put("vence_en", nueva.venceEn().toString());
        return FabricaSobres.de(nueva.negocioId(), ahora, UuidV7.generar(ahora), null, DISPONIBILIDAD_ACTUALIZADA, 1,
                datos);
    }

    /** B13: se publico, se oculto o se restituyo una reseña. Ni el comentario ni el texto de nadie viajan (EV-01). */
    static SobreEvento resenaPublicada(Resena resena, String accion, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("resena_id", resena.id().toString());
        datos.put("visitante_id", resena.visitanteId().toString());
        datos.put("accion", accion);
        datos.put("calificacion", resena.calificacion());
        datos.put("tiene_comentario", resena.comentario() != null);
        return FabricaSobres.de(resena.negocioId(), ahora, UuidV7.generar(ahora), null, RESENA_PUBLICADA, 1, datos);
    }

    /** B14: el negocio respondio una reseña. La respuesta no viaja. */
    static SobreEvento resenaRespondida(Resena resena, Instant ahora) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("resena_id", resena.id().toString());
        datos.put("visitante_id", resena.visitanteId().toString());
        return FabricaSobres.de(resena.negocioId(), ahora, UuidV7.generar(ahora), null, RESENA_RESPONDIDA, 1, datos);
    }
}
