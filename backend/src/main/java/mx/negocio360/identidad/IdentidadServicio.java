package mx.negocio360.identidad;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.aplicacion.PoliticaDeAcceso;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCodigos;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioSesiones;
import mx.negocio360.identidad.dominio.CodigoAcceso;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.Cuenta;
import mx.negocio360.identidad.dominio.DireccionIp;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.dominio.NombreVisible;
import mx.negocio360.identidad.dominio.Sesion;
import mx.negocio360.identidad.dominio.Telefono;

/**
 * Casos de uso de acceso (IDN-1): codigo, sesion, cierre y nombre. Los metodos son {@code synchronized} porque con
 * adaptadores en memoria la atomicidad la da este bloqueo; con Postgres la darán la transaccion y el bloqueo de fila
 * (IDN-1.1), y un codigo de un solo uso se consumira con una actualizacion condicional.
 */
public final class IdentidadServicio implements IdentidadApi {

    private static final Pattern CODIGO_VALIDO = Pattern.compile("\\d{6}");
    private static final int MAXIMO_DE_CODIGOS = 1_000_000;
    private static final int LARGO_MAXIMO_DEL_DISPOSITIVO = 80;

    private final SecureRandom aleatorio = new SecureRandom();
    private final RepositorioCuentas cuentas;
    private final RepositorioCodigos codigos;
    private final RepositorioSesiones sesiones;
    private final CanalDeCodigos canal;
    private final LimiteDeTasa limite;
    private final Clock reloj;
    private final PoliticaDeAcceso politica;

    public IdentidadServicio(RepositorioCuentas cuentas, RepositorioCodigos codigos, RepositorioSesiones sesiones,
                             CanalDeCodigos canal, LimiteDeTasa limite, Clock reloj, PoliticaDeAcceso politica) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.codigos = Objects.requireNonNull(codigos, "codigos");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.canal = Objects.requireNonNull(canal, "canal");
        this.limite = Objects.requireNonNull(limite, "limite");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.politica = Objects.requireNonNull(politica, "politica");
    }

    @Override
    public synchronized void solicitarCodigo(String telefono, String ip) {
        var numero = telefonoNormalizado(telefono);
        var ahora = reloj.instant();
        exigirCupo("ip:" + DireccionIp.claveDeLimite(ip), politica.solicitudesPorIp(), ahora);
        exigirCupo("tel:" + numero, politica.solicitudesPorTelefono(), ahora);
        var claro = nuevoCodigo();
        enviar(numero, claro);
        codigos.guardar(CodigoAcceso.emitir(UuidV7.generar(ahora), numero, claro, ahora, politica.codigoVigencia()));
    }

    /**
     * Envía antes de guardar: si el canal falla no queda un código nuevo que tape al anterior, que la persona
     * pudo haber recibido. Cualquier fallo del canal es ERROR_TEMPORAL y no se copia su mensaje, que puede llevar
     * el teléfono o el código.
     */
    private void enviar(String numero, String claro) {
        try {
            canal.enviar(numero, claro);
        } catch (RuntimeException e) {
            throw new IdentidadException(CodigoError.ERROR_TEMPORAL);
        }
    }

    @Override
    public synchronized SesionIniciada iniciarSesion(String telefono, String codigo, String nombre,
                                                     String dispositivo, String ip) {
        var numero = telefonoNormalizado(telefono);
        var ahora = reloj.instant();
        var guardado = codigos.ultimoDe(numero).orElseThrow(() -> new IdentidadException(CodigoError.CODIGO_INVALIDO));
        verificarCodigo(guardado, codigo, ahora);
        var cuenta = cuentaParaIniciar(numero, nombre, guardado, ahora);
        codigos.guardar(guardado.usado(ahora));
        return abrirSesion(cuenta, dispositivo, ip, ahora);
    }

    @Override
    public synchronized Optional<SesionAutenticada> autenticar(String token) {
        return identificar(token).map(i -> new SesionAutenticada(i.cuenta().id(), i.cuenta().nombre(),
                i.sesion().negocioId(), i.sesion().expiraEn()));
    }

    @Override
    public synchronized PerfilVista yo(String token) {
        var identificada = identificarOFallar(token);
        return new PerfilVista(vista(identificada.cuenta()), identificada.sesion().negocioId(), List.of());
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

    private record Identificada(Cuenta cuenta, Sesion sesion) {
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
        var renovada = sesion.orElseThrow().renovada(ahora, politica.inactividad());
        sesiones.guardar(renovada);
        return Optional.of(new Identificada(cuenta.get(), renovada));
    }

    private Identificada identificarOFallar(String token) {
        return identificar(token).orElseThrow(() -> new IdentidadException(CodigoError.NO_AUTENTICADO));
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

    /** Orden: usado, agotado, vencido y, por ultimo, la comparacion. Un fallo de comparacion suma un intento. */
    private void verificarCodigo(CodigoAcceso guardado, String intento, Instant ahora) {
        if (guardado.usadoEn() != null) {
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
        if (guardado.agotado(politica.intentosMaximos())) {
            throw new IdentidadException(CodigoError.CODIGO_AGOTADO);
        }
        if (guardado.vencido(ahora)) {
            throw new IdentidadException(CodigoError.CODIGO_EXPIRADO);
        }
        var formaValida = intento != null && CODIGO_VALIDO.matcher(intento).matches();
        if (!formaValida || !guardado.coincide(intento)) {
            codigos.guardar(guardado.conIntentoFallido());
            throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
        }
    }

    private Cuenta cuentaParaIniciar(String numero, String nombre, CodigoAcceso guardado, Instant ahora) {
        var existente = cuentas.porTelefono(numero);
        if (existente.isPresent()) {
            var cuenta = existente.get();
            if (cuenta.estado() != EstadoCuenta.ACTIVA) {
                codigos.guardar(guardado.usado(ahora));
                throw new IdentidadException(CodigoError.CODIGO_INVALIDO);
            }
            return cuentas.guardar(cuenta.conUltimoAcceso(ahora));
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IdentidadException(CodigoError.NOMBRE_REQUERIDO);
        }
        var valido = NombreVisible.normalizar(nombre)
                .orElseThrow(() -> new IdentidadException(CodigoError.NOMBRE_INVALIDO));
        return cuentas.guardar(Cuenta.nueva(UuidV7.generar(ahora), valido, numero, ahora));
    }

    private SesionIniciada abrirSesion(Cuenta cuenta, String dispositivo, String ip, Instant ahora) {
        var token = TokenOpaco.nuevo();
        var sesion = Sesion.abrir(TokenOpaco.hash(token), cuenta.id(), dispositivoLimpio(dispositivo),
                DireccionIp.truncada(ip).orElse(null), ahora, politica.inactividad(), politica.topeAbsoluto());
        revocarExcedentes(cuenta, ahora);
        sesiones.guardar(sesion);
        return new SesionIniciada(token, sesion.expiraEn(), vista(cuenta), null, List.of());
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
        return new CuentaVista(cuenta.id(), cuenta.nombre(), cuenta.telefono());
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
