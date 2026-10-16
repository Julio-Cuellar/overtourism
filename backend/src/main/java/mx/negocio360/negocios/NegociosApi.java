package mx.negocio360.negocios;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;

/**
 * Funciones del panel del negocio (seccion 7). Los resolvedores GraphQL y los controladores REST llaman a esta interfaz
 * y nunca a los repositorios. El negocio y el actor salen del contexto, no de un argumento (SG-02), y cada funcion
 * exige su permiso a la {@link Autorizacion} (SG-04). Una edicion lleva la version que la persona vio (NG-10).
 * Un negocio cerrado o suspendido solo se lee.
 */
public interface NegociosApi {

    // ---- consultas ----

    NegocioVista miNegocio(ContextoNegocio contexto, Autorizacion autorizacion);

    List<SucursalVista> sucursales(ContextoNegocio contexto, Autorizacion autorizacion);

    HorarioVista horarioDeSucursal(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId);

    /** Las altas que pidio la cuenta (AL-04). Se permite sin negocio activo: filtra por la cuenta. */
    List<AltaVista> misAltas(UUID cuentaId);

    // ---- perfil y categorias ----

    NegocioVista actualizarNegocio(ContextoNegocio contexto, Autorizacion autorizacion, ActualizarNegocio cambios);

    NegocioVista definirSubcategorias(ContextoNegocio contexto, Autorizacion autorizacion, long version,
                                      List<String> subcategorias);

    NegocioVista definirAtributos(ContextoNegocio contexto, Autorizacion autorizacion, DefinirAtributos atributos);

    DisponibilidadVista definirDisponibilidad(ContextoNegocio contexto, Autorizacion autorizacion, String nivel,
                                              String nota);

    /** Publica u oculta el negocio en la app del turista. Publicar exige ubicacion y horario (PU-01). */
    NegocioVista publicarNegocio(ContextoNegocio contexto, Autorizacion autorizacion, boolean publicar);

    // ---- ubicacion y horario ----

    SucursalVista actualizarSucursal(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId,
                                     ActualizarSucursal cambios);

    /** Reemplaza la semana completa en una sola transaccion (HO-05). */
    HorarioVista definirHorario(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId, long version,
                                List<DiaHorarioComando> semana);

    HorarioVista agregarExcepcionHorario(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId,
                                         ExcepcionComando excepcion);

    HorarioVista quitarExcepcionHorario(ContextoNegocio contexto, Autorizacion autorizacion, UUID excepcionId);

    // ---- reseñas del negocio ----

    /** Las reseñas publicadas del negocio, de la mas nueva a la mas antigua, con el resumen. Pide negocios.resena.ver. */
    PaginaResenas resenas(ContextoNegocio contexto, Autorizacion autorizacion, FiltroResenas filtro, int primeros,
                          String despues);

    /** Una respuesta por reseña, sin editarla (RS-04). Pide negocios.resena.responder. */
    ResenaVista responderResena(ContextoNegocio contexto, Autorizacion autorizacion, UUID resenaId, String texto);

    // ---- catalogos (R1: publicos) ----

    CatalogoVista catalogos();
}
