package mx.negocio360.identidad;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.aplicacion.EquipoServicio;
import mx.negocio360.identidad.aplicacion.PoliticaDeAcceso;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCodigos;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioSesiones;
import mx.negocio360.identidad.dominio.CodigoAcceso;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.Correo;
import mx.negocio360.identidad.dominio.Cuenta;
import mx.negocio360.identidad.dominio.DireccionIp;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.Identificador;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.dominio.NombreVisible;
import mx.negocio360.identidad.dominio.Preregistro;
import mx.negocio360.identidad.dominio.Sesion;
import mx.negocio360.identidad.dominio.Telefono;

/**
 * Casos de uso de acceso (IDN-1): registro en dos pasos, codigo, sesion, cierre y nombre. Todos los codigos llegan al
 * correo de la persona (D-85). Los metodos son {@code synchronized} porque con
 * adaptadores en memoria la atomicidad la da este bloqueo; con Postgres la darán la transaccion y el bloqueo de fila
 * (IDN-1.1), y un codigo de un solo uso se consumira con una actualizacion condicional.
 */
public final class IdentidadServicio implements IdentidadApi, EquipoApi {

    private static final Pattern CODIGO_VALIDO = Pattern.compile("\\d{6}");
    private static final int MAXIMO_DE_CODIGOS = 1_000_000;
    private static final int LARGO_MAXIMO_DEL_DISPOSITIVO = 80;
    /** Version del aviso de privacidad del personal que se acepta al registrarse. */
    private static final int VERSION_DEL_AVISO = 1;

    private final SecureRandom aleatorio = new SecureRandom();
    private final RepositorioCuentas cuentas;
    private final RepositorioCodigos codigos;
    private final RepositorioPreregistros preregistros;
    private final RepositorioSesiones sesiones;
    private final CanalDeCodigos canal;
    private final LimiteDeTasa limite;
    private final Clock reloj;
    private final PoliticaDeAcceso politica;
    private final EquipoServicio equipo;

    public IdentidadServicio(RepositorioCuentas cuentas, RepositorioCodigos codigos,
                             RepositorioPreregistros preregistros, RepositorioSesiones sesiones, CanalDeCodigos canal,
                             LimiteDeTasa limite, Clock reloj, PoliticaDeAcceso politica, EquipoServicio equipo) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.codigos = Objects.requireNonNull(codigos, "codigos");
        this.preregistros = Objects.requireNonNull(preregistros, "preregistros");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.canal = Objects.requireNonNull(canal, "canal");
        this.limite = Objects.requireNonNull(limite, "limite");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.politica = Objects.requireNonNull(politica, "politica");
        this.equipo = Objects.requireNonNull(equipo, "equipo");
    }

    @Override
    public synchronized void solicitarCodigo(String identificador, String ip) {
        var quien = Identificador.de(identificador);
        var ahora = reloj.instant();
        exigirCupo("ip:" + DireccionIp.claveDeLimite(ip), politica.solicitudesPorIp(), ahora);
        // El tope tambien cuenta por lo que se escribio, exista o no la cuenta (SI-02, AS-03).
        exigirCupo("id:" + quien.valor(), politica.solicitudesPorCorreo(), ahora);
        var destino = correoDe(quien);
        if (destino.isEmpty()) {
            return;
        }
        var correo = destino.get();
        // El tope por correo lo comparten el celular, el correo y el id: nadie le llena el correo a otra persona. Agotarlo
        // no es un error, porque un 429 solo ocurriria si la cuenta existe (SI-02): se omite el envio.
        if (!limite.intentar(TokenOpaco.hash("dest:" + correo), politica.solicitudesPorCorreo(),
                politica.ventanaDeSolicitudes(), ahora)) {
            return;
        }
        var claro = nuevoCodigo();
        enviar(correo, claro);
        codigos.guardar(CodigoAcceso.emitir(UuidV7.generar(ahora), correo, claro, ahora, politica.codigoVigencia()));
    }

    /**
     * Envía antes de guardar: si el canal falla no queda un código nuevo que tape al anterior, que la persona
     * pudo haber recibido. Cualquier fallo del canal es ERROR_TEMPORAL y no se copia su mensaje, que puede llevar
     * el correo o el código.
     */
    private void enviar(String correo, String claro) {
        try {
            canal.enviar(correo, claro);
        } catch (RuntimeException e) {
            throw new IdentidadException(CodigoError.ERROR_TEMPORAL);
        }
    }

    @Override
    public synchronized SesionIniciada iniciarSesion(String identificador, String codigo, String dispositivo,
                                                     String ip) {
        var quien = Identificador.de(identificador);
        var ahora = reloj.instant();
        var correo = correoDe(quien).orElseThrow(() -> new IdentidadException(CodigoError.CODIGO_INVALIDO));
        var guardado = codigos.ultimoDe(correo).orElseThrow(() -> new IdentidadException(CodigoError.CODIGO_INVALIDO));
        verificarCodigo(guardado, codigo, ahora);
        var cuenta = cuentaParaIniciar(correo, guardado, ahora);
        codigos.guardar(guardado.usado(ahora));
        return abrirSesion(cuenta, dispositivo, ip, ahora);
    }

    @Override
    public synchronized PreregistroCreado registrar(SolicitudDeRegistro solicitud, String ip) {
        var numero = telefonoNormalizado(solicitud.telefono());
        var nombre = nombreObligatorio(solicitud.nombre());
        var apellidos = NombreVisible.normalizar(solicitud.apellidos())
                .orElseThrow(() -> new IdentidadException(CodigoError.APELLIDOS_INVALIDOS));
        var correo = Correo.normalizar(solicitud.correo())
                .orElseThrow(() -> new IdentidadException(CodigoError.CORREO_INVALIDO));
        if (!solicitud.aceptaAviso()) {
            throw new IdentidadException(CodigoError.AVISO_REQUERIDO);
        }
        // D-79: el negocio que se pide junto con el registro tambien se valida antes de tocar nada.
        var pedido = solicitud.negocio();
        String negocioNombre = null;
        String negocioGiro = null;
        if (pedido != null) {
            negocioGiro = Giro.de(pedido.giro()).orElseThrow(() -> new IdentidadException(CodigoError.GIRO_INVALIDO))
                    .name();
            negocioNombre = NombreVisible.normalizar(pedido.nombre())
                    .orElseThrow(() -> new IdentidadException(CodigoError.NOMBRE_INVALIDO));
        }
        var ahora = reloj.instant();
        exigirCupo("ip:" + DireccionIp.claveDeLimite(ip), politica.solicitudesPorIp(), ahora);
        exigirCupo("reg:" + correo, politica.solicitudesPorCorreo(), ahora);
        // Sin haber probado nada todavia se dice si el correo o el celular ya tienen cuenta, como en Clinerya; los topes
        // de arriba acotan cuanto se puede sondear.
        if (cuentas.porCorreo(correo).isPresent()) {
            throw new IdentidadException(CodigoError.CORREO_NO_DISPONIBLE);
        }
        if (cuentas.porTelefono(numero).isPresent()) {
            throw new IdentidadException(CodigoError.CUENTA_YA_EXISTE);
        }
        var claro = nuevoCodigo();
        var preregistro = Preregistro.nuevo(UuidV7.generar(ahora), correo, numero, nombre, apellidos, VERSION_DEL_AVISO,
                negocioNombre, negocioGiro, claro, ahora, politica.preregistroVigencia());
        // Envia antes de guardar: si el canal falla no se pierde el pre-registro anterior ni su codigo.
        enviar(correo, claro);
        preregistros.guardar(preregistro);
        return new PreregistroCreado(correo, preregistro.venceEn());
    }

    @Override
    public synchronized SesionIniciada verificarRegistro(String correoEscrito, String codigo, String dispositivo,
                                                         String ip) {
        var correo = Correo.normalizar(correoEscrito)
                .orElseThrow(() -> new IdentidadException(CodigoError.CORREO_INVALIDO));
        var ahora = reloj.instant();
        exigirCupo("ver:" + DireccionIp.claveDeLimite(ip), politica.solicitudesPorIp(), ahora);
        // Sin pre-registro, vencido, agotado o con otro codigo la respuesta es la misma: no se delata cual correo tiene uno.
        var preregistro = preregistros.porCorreo(correo)
                .orElseThrow(() -> new IdentidadException(CodigoError.CODIGO_INVALIDO));
        if (preregistro.vencido(ahora)) {
            preregistros.eliminar(preregistro.id());
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        if (preregistro.agotado(politica.intentosMaximos())) {
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        var formaValida = codigo != null && CODIGO_VALIDO.matcher(codigo).matches();
        if (!formaValida || !preregistro.coincide(codigo)) {
            preregistros.guardar(preregistro.conIntentoFallido());
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        // Con el codigo ya comprobado, quien llega aqui es dueño del correo. Si mientras tanto el correo o el celular
        // se usaron en otra cuenta, el pre-registro ya no puede cumplirse y se borra.
        if (cuentas.porCorreo(correo).isPresent()) {
            preregistros.eliminar(preregistro.id());
            throw new IdentidadException(CodigoError.CORREO_NO_DISPONIBLE);
        }
        if (cuentas.porTelefono(preregistro.telefono()).isPresent()) {
            preregistros.eliminar(preregistro.id());
            throw new IdentidadException(CodigoError.CUENTA_YA_EXISTE);
        }
        // Consumirlo es lo que decide quien gana si dos verificaciones llegan juntas.
        if (!preregistros.eliminar(preregistro.id())) {
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        var cuenta = cuentas.guardar(Cuenta.verificada(UuidV7.generar(ahora), preregistro.nombre(),
                preregistro.apellidos(), preregistro.telefono(), correo, preregistro.avisoVersion(),
                preregistro.avisoAceptadoEn(), ahora));
        if (preregistro.pideNegocio()) {
            equipo.solicitarNegocio(cuenta.id(), preregistro.negocioNombre(), preregistro.negocioGiro());
        }
        return abrirSesion(cuenta, dispositivo, ip, ahora);
    }

    @Override
    public synchronized void reenviarCodigoDeRegistro(String correoEscrito, String ip) {
        var correo = Correo.normalizar(correoEscrito)
                .orElseThrow(() -> new IdentidadException(CodigoError.CORREO_INVALIDO));
        var ahora = reloj.instant();
        exigirCupo("ip:" + DireccionIp.claveDeLimite(ip), politica.solicitudesPorIp(), ahora);
        // El tope cuenta aunque no haya pre-registro, y lo comparte con el formulario: no delata cuales correos tienen uno.
        exigirCupo("reg:" + correo, politica.solicitudesPorCorreo(), ahora);
        var existente = preregistros.porCorreo(correo);
        if (existente.isEmpty()) {
            return;
        }
        var claro = nuevoCodigo();
        var renovado = existente.get().conCodigoNuevo(claro, ahora, politica.preregistroVigencia());
        enviar(correo, claro);
        preregistros.guardar(renovado);
    }

    @Override
    public synchronized Optional<SesionAutenticada> autenticar(String token) {
        return identificar(token).map(i -> new SesionAutenticada(i.cuenta().id(), i.cuenta().nombre(),
                i.negocioActivo(), i.vinculo() == null ? null : i.vinculo().rol().codigo(),
                i.vinculo() == null ? Set.of() : i.vinculo().rol().permisos(), i.sesion().expiraEn()));
    }

    @Override
    public synchronized PerfilVista yo(String token) {
        var identificada = identificarOFallar(token);
        var vinculo = identificada.vinculo();
        return new PerfilVista(vista(identificada.cuenta()), identificada.negocioActivo(),
                vinculo == null ? null : vinculo.rol().codigo(),
                vinculo == null ? List.of() : vinculo.rol().permisos().stream().sorted().toList(),
                equipo.negociosDe(identificada.cuenta().id()), equipo.pendientesDe(identificada.cuenta().id()));
    }

    @Override
    public synchronized SesionIniciada elegirNegocio(String token, UUID negocioId) {
        var identificada = identificarOFallar(token);
        var elegido = equipo.vinculo(identificada.cuenta().id(), negocioId)
                .orElseThrow(() -> new IdentidadException(CodigoError.NEGOCIO_NO_DISPONIBLE));
        var ahora = reloj.instant();
        var nuevoToken = TokenOpaco.nuevo();
        var nueva = identificada.sesion().paraNegocio(TokenOpaco.hash(nuevoToken), elegido.negocio().negocioId(), ahora,
                politica.inactividad());
        sesiones.guardar(identificada.sesion().revocada(ahora));
        sesiones.guardar(nueva);
        return new SesionIniciada(nuevoToken, nueva.expiraEn(), vista(identificada.cuenta()), nueva.negocioId(),
                equipo.negociosDe(identificada.cuenta().id()), equipo.pendientesDe(identificada.cuenta().id()));
    }

    @Override
    public synchronized NegocioSolicitado solicitarNegocio(String token, String nombre, String giro) {
        var cuenta = identificarOFallar(token).cuenta();
        // Tope por cuenta: sin el, un script autenticado inundaria al modulo de negocios con pedidos.
        exigirCupo("neg:" + cuenta.id(), politica.solicitudesPorCorreo(), reloj.instant());
        return equipo.solicitarNegocio(cuenta.id(), nombre, giro);
    }

    @Override
    public synchronized List<RolVista> roles(String token) {
        return equipo.roles(actorDe(identificarOFallar(token)));
    }

    @Override
    public synchronized List<PermisoVista> permisos(String token) {
        return equipo.permisos(actorDe(identificarOFallar(token)));
    }

    @Override
    public synchronized List<MiembroVista> equipo(String token) {
        return equipo.equipo(actorDe(identificarOFallar(token)));
    }

    @Override
    public synchronized MiembroVista cambiarRol(String token, UUID cuentaId, String rolCodigo) {
        return equipo.cambiarRol(actorDe(identificarOFallar(token)), cuentaId, rolCodigo);
    }

    @Override
    public synchronized MiembroVista suspender(String token, UUID cuentaId) {
        return equipo.suspender(actorDe(identificarOFallar(token)), cuentaId);
    }

    @Override
    public synchronized MiembroVista reactivar(String token, UUID cuentaId) {
        return equipo.reactivar(actorDe(identificarOFallar(token)), cuentaId);
    }

    @Override
    public synchronized void revocar(String token, UUID cuentaId) {
        equipo.revocar(actorDe(identificarOFallar(token)), cuentaId);
    }

    @Override
    public synchronized void salir(String token) {
        equipo.salir(actorDe(identificarOFallar(token)));
    }

    @Override
    public synchronized void cerrarSesion(String token) {
        var identificada = identificarOFallar(token);
        sesiones.guardar(identificada.sesion().revocada(reloj.instant()));
    }

    @Override
    public synchronized void cerrarTodasLasSesiones(String token) {
        var identificada = identificarOFallar(token);
        var ahora = reloj.instant();
        sesiones.vigentesDe(identificada.cuenta().id(), ahora).forEach(s -> sesiones.guardar(s.revocada(ahora)));
    }

    @Override
    public synchronized CuentaVista actualizarNombre(String token, String nombre) {
        var identificada = identificarOFallar(token);
        var normalizado = NombreVisible.normalizar(nombre)
                .orElseThrow(() -> new IdentidadException(CodigoError.NOMBRE_INVALIDO));
        return vista(cuentas.guardar(identificada.cuenta().conNombre(normalizado)));
    }

    // ---- pasos privados ----

    /** La cuenta y la sesion de un token, y la membresia con su rol si la sesion tiene un negocio activo (si no, nulo). */
    private record Identificada(Cuenta cuenta, Sesion sesion, EquipoServicio.Vinculo vinculo) {

        /** El negocio activo efectivo: el de la membresia con que se actua, o nulo si no hay. */
        UUID negocioActivo() {
            return vinculo == null ? null : vinculo.negocio().negocioId();
        }
    }

    /** El negocio activo y el rol con que se actua; sin negocio activo no hay nada que administrar. */
    private static EquipoServicio.Actor actorDe(Identificada identificada) {
        var vinculo = identificada.vinculo();
        if (vinculo == null) {
            throw new IdentidadException(CodigoError.NEGOCIO_NO_DISPONIBLE);
        }
        return new EquipoServicio.Actor(identificada.cuenta().id(), vinculo.negocio().negocioId(), vinculo.rol());
    }

    private Optional<Identificada> identificar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        var ahora = reloj.instant();
        var sesion = sesiones.porHash(TokenOpaco.hash(token)).filter(s -> s.vigente(ahora));
        var cuenta = sesion.flatMap(s -> cuentas.porId(s.cuentaId())).filter(c -> c.estado() == EstadoCuenta.ACTIVA);
        if (cuenta.isEmpty()) {
            return Optional.empty();
        }
        var actual = sesion.orElseThrow();
        // D-80: sin negocio elegido, el unico negocio de la persona es su negocio activo. Sale de las membresias del
        // servidor, no de lo que mande el cliente. Con dos o mas (o ninguno) no se activa nada: todo queda negado.
        var elegido = actual.negocioId();
        if (elegido == null) {
            var suyos = equipo.negociosDe(cuenta.get().id());
            elegido = suyos.size() == 1 ? suyos.get(0).id() : null;
        }
        var vinculo = elegido == null ? null : equipo.vinculo(cuenta.get().id(), elegido).orElse(null);
        // AM-06: si la membresia del negocio elegido ya no esta activa, la sesion deja de servir en ese instante.
        if (actual.negocioId() != null && vinculo == null) {
            return Optional.empty();
        }
        var renovada = actual.renovada(ahora, politica.inactividad());
        sesiones.guardar(renovada);
        return Optional.of(new Identificada(cuenta.get(), renovada, vinculo));
    }

    private Identificada identificarOFallar(String token) {
        return identificar(token).orElseThrow(() -> new IdentidadException(CodigoError.NO_AUTENTICADO));
    }

    /** Correo al que llega el codigo de esa identidad: el de la cuenta, se haya escrito el celular, el correo o el id. */
    private Optional<String> correoDe(Identificador quien) {
        return switch (quien.tipo()) {
            case TELEFONO -> cuentas.porTelefono(quien.valor()).map(Cuenta::correo);
            case CORREO -> cuentas.porCorreo(quien.valor()).map(Cuenta::correo);
            case ID -> cuentas.porId(UUID.fromString(quien.valor())).map(Cuenta::correo);
        };
    }

    private static String nombreObligatorio(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IdentidadException(CodigoError.NOMBRE_REQUERIDO);
        }
        return NombreVisible.normalizar(nombre).orElseThrow(() -> new IdentidadException(CodigoError.NOMBRE_INVALIDO));
    }

    private static String telefonoNormalizado(String telefono) {
        return Telefono.normalizar(telefono).orElseThrow(() -> new IdentidadException(CodigoError.TELEFONO_INVALIDO));
    }

    private void exigirCupo(String clave, int maximo, Instant ahora) {
        if (!limite.intentar(TokenOpaco.hash(clave), maximo, politica.ventanaDeSolicitudes(), ahora)) {
            throw new IdentidadException(CodigoError.TASA_EXCEDIDA);
        }
    }

    private String nuevoCodigo() {
        return String.format(Locale.ROOT, "%06d", aleatorio.nextInt(MAXIMO_DE_CODIGOS));
    }

    /**
     * Usado, agotado, vencido o incorrecto es lo mismo para quien escribe: CODIGO_INVALIDO. Si no, quien prueba
     * identificadores sabria cuales tienen cuenta, porque con una cuenta inexistente la respuesta siempre es esa. Un
     * fallo de comparacion suma un intento.
     */
    private void verificarCodigo(CodigoAcceso guardado, String intento, Instant ahora) {
        if (guardado.usadoEn() != null || guardado.agotado(politica.intentosMaximos()) || guardado.vencido(ahora)) {
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        var formaValida = intento != null && CODIGO_VALIDO.matcher(intento).matches();
        if (!formaValida || !guardado.coincide(intento)) {
            codigos.guardar(guardado.conIntentoFallido());
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
    }

    /** La cuenta de ese correo; si esta suspendida el codigo se gasta y el error es el de un codigo incorrecto. */
    private Cuenta cuentaParaIniciar(String correo, CodigoAcceso guardado, Instant ahora) {
        var cuenta = cuentas.porCorreo(correo)
                .orElseThrow(() -> new IdentidadException(CodigoError.CODIGO_INVALIDO));
        if (cuenta.estado() != EstadoCuenta.ACTIVA) {
            codigos.guardar(guardado.usado(ahora));
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        return cuentas.guardar(cuenta.conUltimoAcceso(ahora));
    }

    private SesionIniciada abrirSesion(Cuenta cuenta, String dispositivo, String ip, Instant ahora) {
        var token = TokenOpaco.nuevo();
        var negocios = equipo.negociosDe(cuenta.id());
        // Con un solo negocio la persona entra directo a el; con varios (o ninguno) elige despues.
        var activo = negocios.size() == 1 ? negocios.get(0).id() : null;
        var sesion = Sesion.abrir(TokenOpaco.hash(token), cuenta.id(), dispositivoLimpio(dispositivo),
                DireccionIp.truncada(ip).orElse(null), ahora, politica.inactividad(), politica.topeAbsoluto())
                .conNegocio(activo);
        revocarExcedentes(cuenta, ahora);
        sesiones.guardar(sesion);
        return new SesionIniciada(token, sesion.expiraEn(), vista(cuenta), activo, negocios,
                equipo.pendientesDe(cuenta.id()));
    }

    /** AS-09: deja espacio para la sesion nueva revocando las mas antiguas. */
    private void revocarExcedentes(Cuenta cuenta, Instant ahora) {
        var vigentes = sesiones.vigentesDe(cuenta.id(), ahora);
        var sobran = vigentes.size() - (politica.sesionesMaximas() - 1);
        for (var i = 0; i < sobran; i++) {
            sesiones.guardar(vigentes.get(i).revocada(ahora));
        }
    }

    private static CuentaVista vista(Cuenta cuenta) {
        return new CuentaVista(cuenta.id(), cuenta.nombre(), cuenta.apellidos(), cuenta.telefono(), cuenta.correo());
    }

    /**
     * Cambia por espacio los caracteres de control y de formato (los que invierten el texto), y corta por
     * caracteres completos: cortar por unidades de UTF-16 podría dejar la mitad de un emoji.
     */
    private static String dispositivoLimpio(String dispositivo) {
        if (dispositivo == null) {
            return null;
        }
        var limpio = dispositivo.codePoints()
                .map(c -> esInvisible(c) ? ' ' : c)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString().strip();
        if (limpio.isEmpty()) {
            return null;
        }
        var largo = limpio.codePointCount(0, limpio.length());
        return largo > LARGO_MAXIMO_DEL_DISPOSITIVO
                ? limpio.substring(0, limpio.offsetByCodePoints(0, LARGO_MAXIMO_DEL_DISPOSITIVO))
                : limpio;
    }

    private static boolean esInvisible(int codigo) {
        return Character.isISOControl(codigo) || Character.getType(codigo) == Character.FORMAT;
    }
}
