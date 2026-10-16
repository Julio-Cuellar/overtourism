package mx.negocio360.negocios.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.negocios.ActualizarNegocio;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.AtributosVista;
import mx.negocio360.negocios.CatalogoVista;
import mx.negocio360.negocios.CompletitudVista;
import mx.negocio360.negocios.DefinirAtributos;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.DiaVista;
import mx.negocio360.negocios.DisponibilidadVista;
import mx.negocio360.negocios.EtiquetaVista;
import mx.negocio360.negocios.ExcepcionComando;
import mx.negocio360.negocios.ExcepcionVista;
import mx.negocio360.negocios.FiltroResenas;
import mx.negocio360.negocios.PaginaResenas;
import mx.negocio360.negocios.ResenaVista;
import mx.negocio360.negocios.HorarioVista;
import mx.negocio360.negocios.NegocioVista;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.SubcategoriaVista;
import mx.negocio360.negocios.SucursalVista;
import mx.negocio360.negocios.TramoVista;
import mx.negocio360.negocios.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioAltas;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioCatalogos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios.AtributosElegidos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioSucursales;
import mx.negocio360.negocios.dominio.AtributosDeclarados;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.Completitud;
import mx.negocio360.negocios.dominio.ContactoNegocio;
import mx.negocio360.negocios.dominio.DiaHorario;
import mx.negocio360.negocios.dominio.Disponibilidad;
import mx.negocio360.negocios.dominio.ExcepcionHorario;
import mx.negocio360.negocios.dominio.HorarioSucursal;
import mx.negocio360.negocios.dominio.Negocio;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.NombreNegocio;
import mx.negocio360.negocios.dominio.SemanaHorario;
import mx.negocio360.negocios.dominio.Sucursal;
import mx.negocio360.negocios.dominio.Tramo;

/**
 * Casos de uso del panel del negocio (seccion 7): perfil, subcategorias, atributos, disponibilidad, publicacion,
 * ubicacion y horario. Java puro: la transaccion la pone quien lo envuelve ({@code FronteraTransaccional}), asi que cada
 * llamada guarda el cambio, su historial y su evento juntos (R3).
 *
 * <p>El negocio y el actor salen del contexto (SG-02); cada funcion exige su permiso (SG-04); lo que no existe en este
 * negocio responde igual que lo que no existe (SG-03).
 */
public final class NegociosServicio implements NegociosApi {

    private static final int MAXIMO_SUBCATEGORIAS = 8;
    private static final int MAXIMO_DESCRIPCION = 280;
    private static final int MAXIMO_NOMBRE_SUCURSAL = 60;

    private final RepositorioNegocios negocios;
    private final RepositorioSucursales sucursales;
    private final RepositorioCatalogos catalogos;
    private final RepositorioAltas altas;
    private final ResenasServicio resenas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public NegociosServicio(RepositorioNegocios negocios, RepositorioSucursales sucursales,
                            RepositorioCatalogos catalogos, RepositorioAltas altas, ResenasServicio resenas,
                            PublicadorEventos eventos, Clock reloj) {
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.sucursales = Objects.requireNonNull(sucursales, "sucursales");
        this.catalogos = Objects.requireNonNull(catalogos, "catalogos");
        this.altas = Objects.requireNonNull(altas, "altas");
        this.resenas = Objects.requireNonNull(resenas, "resenas");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ------------------------------------------------------------------------------------------ consultas

    @Override
    public NegocioVista miNegocio(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeNegocios.PERFIL_VER);
        return negocioVista(contexto.negocioId());
    }

    @Override
    public List<SucursalVista> sucursales(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeNegocios.PERFIL_VER);
        return sucursales.sucursales(contexto.negocioId()).stream().map(NegociosServicio::sucursalVista).toList();
    }

    @Override
    public HorarioVista horarioDeSucursal(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId) {
        autorizacion.exigir(PermisosDeNegocios.PERFIL_VER);
        return horario(contexto.negocioId(), sucursal(contexto.negocioId(), sucursalId).id());
    }

    @Override
    public List<AltaVista> misAltas(UUID cuentaId) {
        return altas.altasDeCuenta(Objects.requireNonNull(cuentaId, "cuentaId"));
    }

    @Override
    public CatalogoVista catalogos() {
        return new CatalogoVista(
                catalogos.giros().stream().map(g -> new CatalogoVista.Giro(g.codigo(), g.nombre())).toList(),
                catalogos.subcategorias().stream()
                        .map(s -> new CatalogoVista.Subcategoria(s.codigo(), s.giro(), s.nombre())).toList(),
                catalogos.atributos().stream()
                        .map(a -> new CatalogoVista.Atributo(a.tipo(), a.codigo(), a.nombre())).toList());
    }

    // ------------------------------------------------------------------------------------- perfil y categorias

    @Override
    public NegocioVista actualizarNegocio(ContextoNegocio contexto, Autorizacion autorizacion,
                                          ActualizarNegocio cambios) {
        autorizacion.exigir(PermisosDeNegocios.PERFIL_EDITAR);
        var negocio = editable(contexto.negocioId());
        var ahora = reloj.instant();
        var campos = new ArrayList<String>();

        var nombre = negocio.nombre();
        if (cambios.nombre() != null) {
            nombre = NombreNegocio.de(cambios.nombre()).valor();
            marcar(campos, "nombre", !nombre.equals(negocio.nombre()));
        }
        var descripcion = negocio.descripcion();
        if (cambios.descripcion() != null) {
            descripcion = descripcion(cambios.descripcion());
            marcar(campos, "descripcion", !Objects.equals(descripcion, negocio.descripcion()));
        }
        var telefono = negocio.telefonoContacto();
        if (cambios.telefonoContacto() != null) {
            telefono = cambios.telefonoContacto().isBlank() ? null : ContactoNegocio.telefono(cambios.telefonoContacto());
            marcar(campos, "telefono_contacto", !Objects.equals(telefono, negocio.telefonoContacto()));
        }
        var correo = negocio.correoContacto();
        if (cambios.correoContacto() != null) {
            correo = cambios.correoContacto().isBlank() ? null : ContactoNegocio.correo(cambios.correoContacto());
            marcar(campos, "correo_contacto", !Objects.equals(correo, negocio.correoContacto()));
        }
        var contactoPublico = negocio.contactoPublico();
        if (cambios.contactoPublico() != null) {
            contactoPublico = cambios.contactoPublico();
            marcar(campos, "contacto_publico", contactoPublico != negocio.contactoPublico());
        }
        if (campos.isEmpty()) {
            return negocioVista(negocio.id());
        }
        var nuevo = negocio.conPerfil(nombre, descripcion, telefono, correo, contactoPublico, ahora);
        guardar(contexto, nuevo, cambios.version(), "ACTUALIZAR_NEGOCIO", campos, ahora);
        return negocioVista(negocio.id());
    }

    @Override
    public NegocioVista definirSubcategorias(ContextoNegocio contexto, Autorizacion autorizacion, long version,
                                             List<String> subcategorias) {
        autorizacion.exigir(PermisosDeNegocios.PERFIL_EDITAR);
        var negocio = editable(contexto.negocioId());
        if (subcategorias == null || subcategorias.stream().anyMatch(Objects::isNull)) {
            throw new NegociosException(CodigoError.SUBCATEGORIA_INVALIDA);
        }
        if (subcategorias.size() > MAXIMO_SUBCATEGORIAS) {
            throw new NegociosException(CodigoError.SUBCATEGORIAS_EXCEDIDAS);
        }
        var activas = catalogos.subcategorias().stream().map(RepositorioCatalogos.Subcategoria::codigo)
                .collect(Collectors.toSet());
        if (new HashSet<>(subcategorias).size() != subcategorias.size() || !activas.containsAll(subcategorias)) {
            throw new NegociosException(CodigoError.SUBCATEGORIA_INVALIDA);
        }
        var ahora = reloj.instant();
        // La version se revisa primero: si otra persona cambio el negocio no se toca nada.
        guardar(contexto, negocio.tocado(ahora), version, "DEFINIR_SUBCATEGORIAS", List.of("subcategorias"), ahora,
                () -> negocios.reemplazarSubcategorias(negocio.id(), subcategorias, ahora));
        return negocioVista(negocio.id());
    }

    @Override
    public NegocioVista definirAtributos(ContextoNegocio contexto, Autorizacion autorizacion,
                                         DefinirAtributos atributos) {
        autorizacion.exigir(PermisosDeNegocios.PERFIL_EDITAR);
        var negocio = editable(contexto.negocioId());
        var declarados = new AtributosDeclarados(
                Objects.requireNonNull(atributos.dieta(), "dieta"), Objects.requireNonNull(atributos.accesibilidad(),
                "accesibilidad"), atributos.duracionVisitaMin(), atributos.costoMedioMxn());
        var catalogo = catalogos.atributos();
        verificarCatalogo(catalogo, "DIETA", declarados.dieta());
        verificarCatalogo(catalogo, "ACCESIBILIDAD", declarados.accesibilidad());
        var ahora = reloj.instant();
        var nuevo = negocio.conAtributos(declarados.duracionVisitaMin(), declarados.costoMedioMxn(), ahora);
        guardar(contexto, nuevo, atributos.version(), "DEFINIR_ATRIBUTOS", List.of("atributos"), ahora,
                () -> negocios.reemplazarAtributos(negocio.id(), declarados.dieta(), declarados.accesibilidad(), ahora));
        return negocioVista(negocio.id());
    }

    @Override
    public DisponibilidadVista definirDisponibilidad(ContextoNegocio contexto, Autorizacion autorizacion, String nivel,
                                                     String nota) {
        autorizacion.exigir(PermisosDeNegocios.DISPONIBILIDAD_EDITAR);
        var negocio = editable(contexto.negocioId());
        var ahora = reloj.instant();
        var nueva = Disponibilidad.declarar(negocio.id(), nivel, nota, cuentaDe(contexto), ahora);
        var anterior = negocios.disponibilidad(negocio.id()).filter(d -> d.vigenteEn(ahora)).map(Disponibilidad::nivel);
        negocios.guardarDisponibilidad(nueva);
        negocios.registrarHistorial(negocio.id(), "DECLARAR_DISPONIBILIDAD", List.of("disponibilidad"),
                contexto.actor(), ahora);
        // DI-05: solo se publica cuando cambia el nivel, no en cada renovacion.
        if (anterior.isEmpty() || anterior.get() != nueva.nivel()) {
            eventos.publicar(FabricaEventos.disponibilidadActualizada(nueva, anterior.orElse(null), ahora));
        }
        return disponibilidadVista(nueva);
    }

    @Override
    public NegocioVista publicarNegocio(ContextoNegocio contexto, Autorizacion autorizacion, boolean publicar) {
        autorizacion.exigir(PermisosDeNegocios.NEGOCIO_PUBLICAR);
        var negocio = editable(contexto.negocioId());
        if (negocio.publicado() == publicar) {
            return negocioVista(negocio.id());
        }
        if (publicar) {
            var principal = principal(negocio.id());
            var faltantes = new ArrayList<String>();
            if (!principal.tieneUbicacion()) {
                faltantes.add(Completitud.Faltante.UBICACION.name());
            }
            if (!sucursales.semana(negocio.id(), principal.id()).tieneTramos()) {
                faltantes.add(Completitud.Faltante.HORARIO.name());
            }
            if (!faltantes.isEmpty()) {
                throw new NegociosException(CodigoError.PUBLICACION_INCOMPLETA, faltantes);
            }
        }
        var ahora = reloj.instant();
        // La version actual es la que se acaba de leer: publicar no es una edicion de una pantalla vieja.
        guardar(contexto, negocio.conPublicado(publicar, ahora), negocio.version(), "PUBLICAR_NEGOCIO",
                List.of("publicado"), ahora);
        return negocioVista(negocio.id());
    }

    // ------------------------------------------------------------------------------------------------ reseñas

    @Override
    public PaginaResenas resenas(ContextoNegocio contexto, Autorizacion autorizacion, FiltroResenas filtro,
                                 int primeros, String despues) {
        return resenas.pagina(contexto, autorizacion, filtro, primeros, despues);
    }

    @Override
    public ResenaVista responderResena(ContextoNegocio contexto, Autorizacion autorizacion, UUID resenaId,
                                       String texto) {
        return resenas.responder(contexto, autorizacion, resenaId, texto);
    }

    // ------------------------------------------------------------------------------------ ubicacion y horario

    @Override
    public SucursalVista actualizarSucursal(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId,
                                            ActualizarSucursal cambios) {
        autorizacion.exigir(PermisosDeNegocios.SUCURSAL_GESTIONAR);
        editable(contexto.negocioId());
        var actual = sucursal(contexto.negocioId(), sucursalId);
        var ahora = reloj.instant();
        var campos = new ArrayList<String>();

        var nombre = actual.nombre();
        if (cambios.nombre() != null) {
            nombre = NombreNegocio.de(cambios.nombre()).valor();
            if (nombre.length() > MAXIMO_NOMBRE_SUCURSAL) {
                throw new NegociosException(CodigoError.NOMBRE_INVALIDO);
            }
            marcar(campos, "nombre", !nombre.equals(actual.nombre()));
        }
        var calle = texto(cambios.calle(), actual.calle(), 80, "calle", campos);
        var numero = texto(cambios.numero(), actual.numero(), 20, "numero", campos);
        var colonia = texto(cambios.colonia(), actual.colonia(), 80, "colonia", campos);
        var municipio = texto(cambios.municipio(), actual.municipio(), 80, "municipio", campos);
        var entidad = texto(cambios.entidadFederativa(), actual.entidadFederativa(), 40, "entidad_federativa", campos);
        var referencias = texto(cambios.referencias(), actual.referencias(), 200, "referencias", campos);
        var codigoPostal = actual.codigoPostal();
        if (cambios.codigoPostal() != null) {
            codigoPostal = cambios.codigoPostal().isBlank() ? null : codigoPostal(cambios.codigoPostal());
            marcar(campos, "codigo_postal", !Objects.equals(codigoPostal, actual.codigoPostal()));
        }
        var latitud = actual.latitud();
        var longitud = actual.longitud();
        if (cambios.latitud() != null || cambios.longitud() != null) {
            coordenadas(cambios.latitud(), cambios.longitud());
            latitud = cambios.latitud();
            longitud = cambios.longitud();
            marcar(campos, "coordenadas", actual.latitud() == null || latitud.compareTo(actual.latitud()) != 0
                    || longitud.compareTo(actual.longitud()) != 0);
        }
        var telefono = actual.telefono();
        if (cambios.telefono() != null) {
            telefono = cambios.telefono().isBlank() ? null : ContactoNegocio.telefono(cambios.telefono());
            marcar(campos, "telefono", !Objects.equals(telefono, actual.telefono()));
        }
        if (campos.isEmpty()) {
            return sucursalVista(actual);
        }
        var nueva = actual.conCambios(nombre, calle, numero, colonia, municipio, entidad, codigoPostal, referencias,
                latitud, longitud, telefono, ahora);
        sucursales.actualizar(nueva, cambios.version());
        negocios.registrarHistorial(actual.negocioId(), "ACTUALIZAR_SUCURSAL", campos, contexto.actor(), ahora);
        eventos.publicar(FabricaEventos.sucursalCambiada(nueva, ahora));
        return sucursalVista(sucursal(contexto.negocioId(), sucursalId));
    }

    @Override
    public HorarioVista definirHorario(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId,
                                       long version, List<DiaHorarioComando> semana) {
        autorizacion.exigir(PermisosDeNegocios.HORARIO_EDITAR);
        var negocio = editable(contexto.negocioId());
        var sucursal = sucursal(negocio.id(), sucursalId);
        var nueva = leerSemana(semana);
        var ahora = reloj.instant();
        sucursales.reemplazarSemana(negocio.id(), sucursal.id(), version, nueva, ahora);
        negocios.registrarHistorial(negocio.id(), "DEFINIR_HORARIO", List.of("horario"), contexto.actor(), ahora);
        publicarHorario(negocio, sucursal.id(), ahora);
        return horario(negocio.id(), sucursal.id());
    }

    @Override
    public HorarioVista agregarExcepcionHorario(ContextoNegocio contexto, Autorizacion autorizacion, UUID sucursalId,
                                                ExcepcionComando excepcion) {
        autorizacion.exigir(PermisosDeNegocios.HORARIO_EDITAR);
        var negocio = editable(contexto.negocioId());
        var sucursal = sucursal(negocio.id(), sucursalId);
        var ahora = reloj.instant();
        var nueva = ExcepcionHorario.de(UuidV7.generar(ahora), Objects.requireNonNull(excepcion.fecha(), "fecha"),
                excepcion.cerrado(), excepcion.abre(), excepcion.cierra(), excepcion.motivo());
        if (!sucursales.agregarExcepcion(negocio.id(), sucursal.id(), nueva)) {
            throw new NegociosException(CodigoError.EXCEPCION_DUPLICADA);
        }
        negocios.registrarHistorial(negocio.id(), "AGREGAR_EXCEPCION_HORARIO", List.of("excepciones"),
                contexto.actor(), ahora);
        publicarHorario(negocio, sucursal.id(), ahora);
        return horario(negocio.id(), sucursal.id());
    }

    @Override
    public HorarioVista quitarExcepcionHorario(ContextoNegocio contexto, Autorizacion autorizacion, UUID excepcionId) {
        autorizacion.exigir(PermisosDeNegocios.HORARIO_EDITAR);
        var negocio = editable(contexto.negocioId());
        var sucursalId = sucursales.quitarExcepcion(negocio.id(), Objects.requireNonNull(excepcionId, "excepcionId"))
                .orElseThrow(() -> new NegociosException(CodigoError.EXCEPCION_NO_ENCONTRADA));
        var ahora = reloj.instant();
        negocios.registrarHistorial(negocio.id(), "QUITAR_EXCEPCION_HORARIO", List.of("excepciones"),
                contexto.actor(), ahora);
        publicarHorario(negocio, sucursalId, ahora);
        return horario(negocio.id(), sucursalId);
    }

    // ------------------------------------------------------------------------------------------------- apoyo

    private Negocio negocio(UUID id) {
        return negocios.negocio(id).orElseThrow(() -> new NegociosException(CodigoError.NEGOCIO_NO_DISPONIBLE));
    }

    private Negocio editable(UUID id) {
        var negocio = negocio(id);
        negocio.exigirEditable();
        return negocio;
    }

    private Sucursal principal(UUID negocioId) {
        return negocios.principal(negocioId)
                .orElseThrow(() -> new NegociosException(CodigoError.SUCURSAL_NO_ENCONTRADA));
    }

    private Sucursal sucursal(UUID negocioId, UUID sucursalId) {
        return sucursales.sucursal(negocioId, Objects.requireNonNull(sucursalId, "sucursalId"))
                .orElseThrow(() -> new NegociosException(CodigoError.SUCURSAL_NO_ENCONTRADA));
    }

    /** Guarda con la version esperada, deja el historial y publica {@code negocio.actualizado}, en ese orden. */
    private void guardar(ContextoNegocio contexto, Negocio nuevo, long version, String accion, List<String> campos,
                         java.time.Instant ahora) {
        guardar(contexto, nuevo, version, accion, campos, ahora, () -> { });
    }

    private void guardar(ContextoNegocio contexto, Negocio nuevo, long version, String accion, List<String> campos,
                         java.time.Instant ahora, Runnable adicional) {
        negocios.actualizar(nuevo, version);
        adicional.run();
        negocios.registrarHistorial(nuevo.id(), accion, campos, contexto.actor(), ahora);
        var guardado = negocio(nuevo.id());
        eventos.publicar(FabricaEventos.actualizado(guardado, campos, negocios.subcategorias(nuevo.id()),
                negocios.atributos(nuevo.id()), ahora));
    }

    private void publicarHorario(Negocio negocio, UUID sucursalId, java.time.Instant ahora) {
        var hoy = ahora.atZone(ZoneId.of(negocio.zonaHoraria())).toLocalDate();
        eventos.publicar(FabricaEventos.horarioCambiado(negocio.id(), sucursalId, negocio.zonaHoraria(),
                sucursales.semana(negocio.id(), sucursalId), sucursales.excepciones(negocio.id(), sucursalId), hoy,
                ahora));
    }

    private static void marcar(List<String> campos, String campo, boolean cambio) {
        if (cambio) {
            campos.add(campo);
        }
    }

    private static String descripcion(String entrada) {
        var limpia = entrada.strip();
        if (limpia.isEmpty()) {
            return null;
        }
        if (limpia.length() > MAXIMO_DESCRIPCION || limpia.codePoints().anyMatch(c -> Character.isISOControl(c)
                && c != '\n')) {
            throw new NegociosException(CodigoError.DESCRIPCION_INVALIDA);
        }
        return limpia;
    }

    private static String texto(String nuevo, String actual, int maximo, String campo, List<String> campos) {
        if (nuevo == null) {
            return actual;
        }
        var limpio = nuevo.strip();
        if (limpio.length() > maximo || limpio.codePoints().anyMatch(Character::isISOControl)) {
            throw new NegociosException(CodigoError.DIRECCION_INVALIDA);
        }
        var valor = limpio.isEmpty() ? null : limpio;
        marcar(campos, campo, !Objects.equals(valor, actual));
        return valor;
    }

    private static String codigoPostal(String entrada) {
        var limpio = entrada.strip();
        if (!limpio.matches("\\d{5}")) {
            throw new NegociosException(CodigoError.CODIGO_POSTAL_INVALIDO);
        }
        return limpio;
    }

    /** SU-03: la latitud y la longitud van juntas y dentro de su rango. */
    private static void coordenadas(BigDecimal latitud, BigDecimal longitud) {
        if (latitud == null || longitud == null || latitud.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || longitud.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new NegociosException(CodigoError.COORDENADAS_INVALIDAS);
        }
    }

    private static void verificarCatalogo(List<RepositorioCatalogos.Atributo> catalogo, String tipo,
                                          List<String> codigos) {
        var validos = catalogo.stream().filter(a -> a.tipo().equals(tipo)).map(RepositorioCatalogos.Atributo::codigo)
                .collect(Collectors.toSet());
        if (!validos.containsAll(codigos)) {
            throw new NegociosException(CodigoError.ATRIBUTO_INVALIDO);
        }
    }

    private static SemanaHorario leerSemana(List<DiaHorarioComando> semana) {
        if (semana == null) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
        var dias = new ArrayList<DiaHorario>();
        for (var dia : semana) {
            if (dia == null || dia.tramos() == null) {
                throw new NegociosException(CodigoError.HORARIO_INVALIDO);
            }
            var tramos = new ArrayList<Tramo>();
            for (var tramo : dia.tramos()) {
                if (tramo == null) {
                    throw new NegociosException(CodigoError.HORARIO_INVALIDO);
                }
                tramos.add(Tramo.de(tramo.abre(), tramo.cierra()));
            }
            dias.add(new DiaHorario(dia.dia(), tramos));
        }
        return new SemanaHorario(dias);
    }

    /** La cuenta del actor, si el actor es un id de cuenta; el contexto lleva un texto para la auditoria. */
    private static UUID cuentaDe(ContextoNegocio contexto) {
        var actor = contexto.actor();
        var inicio = actor.length() >= 36 ? actor.substring(actor.length() - 36) : actor;
        try {
            return UUID.fromString(inicio);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------ vistas

    private NegocioVista negocioVista(UUID negocioId) {
        var negocio = negocio(negocioId);
        var principal = principal(negocioId);
        var tieneHorario = sucursales.semana(negocioId, principal.id()).tieneTramos();
        var completitud = Completitud.de(principal.tieneUbicacion(), tieneHorario, negocio.tieneContacto(),
                negocio.logoArchivoId() != null);
        var giros = catalogos.giros().stream().collect(Collectors.toMap(RepositorioCatalogos.Giro::codigo,
                RepositorioCatalogos.Giro::nombre));
        var subcategorias = catalogos.subcategorias().stream().collect(
                Collectors.toMap(RepositorioCatalogos.Subcategoria::codigo, Function.identity()));
        var elegidas = negocios.subcategorias(negocioId).stream().map(subcategorias::get).filter(Objects::nonNull)
                .map(s -> new SubcategoriaVista(s.codigo(), s.nombre(), s.giro(), !s.giro().equals(negocio.giro())))
                .toList();
        var disponibilidad = negocios.disponibilidad(negocioId).map(NegociosServicio::disponibilidadVista).orElse(null);
        return new NegocioVista(negocio.id(), negocio.nombre(), negocio.giro(),
                giros.getOrDefault(negocio.giro(), negocio.giro()), negocio.estado().name(),
                negocio.suspendidoPor() == null ? null : negocio.suspendidoPor().name(), negocio.publicado(),
                negocio.contactoPublico(), negocio.descripcion(), negocio.telefonoContacto(),
                negocio.correoContacto(), negocio.zonaHoraria(),
                new CompletitudVista(completitud.porcentaje(),
                        completitud.faltantes().stream().map(Enum::name).toList()), sucursalVista(principal), elegidas,
                atributosVista(negocio), disponibilidad, negocio.esSimulado(), negocio.version(),
                negocio.actualizadoEn());
    }

    private AtributosVista atributosVista(Negocio negocio) {
        var elegidos = negocios.atributos(negocio.id());
        var nombres = catalogos.atributos().stream().collect(Collectors.toMap(a -> a.tipo() + ":" + a.codigo(),
                RepositorioCatalogos.Atributo::nombre));
        return new AtributosVista(etiquetas("DIETA", elegidos.dieta(), nombres),
                etiquetas("ACCESIBILIDAD", elegidos.accesibilidad(), nombres), negocio.duracionVisitaMin(),
                negocio.costoMedioMxn());
    }

    private static List<EtiquetaVista> etiquetas(String tipo, List<String> codigos, Map<String, String> nombres) {
        return codigos.stream().map(c -> new EtiquetaVista(c, nombres.getOrDefault(tipo + ":" + c, c))).toList();
    }

    private static DisponibilidadVista disponibilidadVista(Disponibilidad d) {
        return new DisponibilidadVista(d.nivel().name(), d.nota(), d.declaradaEn(), d.venceEn());
    }

    private static SucursalVista sucursalVista(Sucursal s) {
        return new SucursalVista(s.id(), s.nombre(), s.esPrincipal(), s.activa(), s.calle(), s.numero(), s.colonia(),
                s.municipio(), s.entidadFederativa(), s.codigoPostal(), s.referencias(), s.latitud(), s.longitud(),
                s.telefono(), s.version());
    }

    private HorarioVista horario(UUID negocioId, UUID sucursalId) {
        var negocio = negocio(negocioId);
        var semana = sucursales.semana(negocioId, sucursalId);
        var excepciones = sucursales.excepciones(negocioId, sucursalId);
        var abierto = new HorarioSucursal(semana, excepciones).abiertoEn(reloj.instant(),
                ZoneId.of(negocio.zonaHoraria()));
        return new HorarioVista(sucursalId,
                semana.dias().stream().filter(d -> !d.tramos().isEmpty()).map(d -> new DiaVista(d.dia(),
                        d.tramos().stream().map(t -> new TramoVista(HorarioSucursal.texto(t.abre()),
                                HorarioSucursal.texto(t.cierra()))).toList())).toList(),
                excepciones.stream().map(e -> new ExcepcionVista(e.id(), e.fecha(), e.cerrado(),
                        HorarioSucursal.texto(e.abre()), HorarioSucursal.texto(e.cierra()), e.motivo())).toList(),
                abierto, sucursales.versionDelHorario(negocioId, sucursalId));
    }
}
