package mx.negocio360.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mx.negocio360.identidad.IdentidadServicio;
import mx.negocio360.identidad.SesionIniciada;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.aplicacion.puertos.CanalNoDisponibleException;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.infraestructura.LimiteDeTasaEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioCodigosEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioCuentasEnMemoria;
import mx.negocio360.identidad.infraestructura.RepositorioSesionesEnMemoria;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Servicio de Identidad armado con adaptadores en memoria, un reloj manual y un canal que graba los codigos. */
public final class Escenario {

    public static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");
    public static final String TEL_A = "222 111 0001";
    public static final String TEL_A_E164 = "+522221110001";
    public static final String TEL_B = "+52 222 111 0002";
    public static final String TEL_B_E164 = "+522221110002";
    public static final String IP = "189.203.10.57";
    public static final String DISPOSITIVO = "Chrome en Windows";
    /** Un caracter mas que el maximo del nombre visible; es literal porque las anotaciones exigen constantes. */
    public static final String NOMBRE_DE_81 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public final RelojMutable reloj = new RelojMutable(AHORA);
    public final CanalGrabador canal = new CanalGrabador();
    public final RepositorioCuentasEnMemoria cuentas = new RepositorioCuentasEnMemoria();
    public final RepositorioCodigosEnMemoria codigos = new RepositorioCodigosEnMemoria();
    public final RepositorioSesionesEnMemoria sesiones = new RepositorioSesionesEnMemoria();
    public final LimiteDeTasaEnMemoria limite = new LimiteDeTasaEnMemoria();
    public final PoliticaDeAcceso politica = PoliticaDeAcceso.porDefecto();
    public final IdentidadServicio api = new IdentidadServicio(cuentas, codigos, sesiones, canal, limite, reloj, politica);

    /** Pide un codigo y devuelve el que recibio el telefono. */
    public String pedirCodigo(String telefono) {
        api.solicitarCodigo(telefono, IP);
        return canal.ultimoCodigo();
    }

    /** Pide el codigo e inicia sesion en un solo paso. */
    public SesionIniciada entrar(String telefono, String nombre) {
        return api.iniciarSesion(telefono, pedirCodigo(telefono), nombre, DISPOSITIVO, IP);
    }

    /** Exige que la accion falle con IdentidadException y el codigo esperado. */
    public static IdentidadException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(IdentidadException.class);
        var excepcion = (IdentidadException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }

    /** Reloj de pruebas que solo avanza cuando la prueba lo manda. */
    public static final class RelojMutable extends Clock {

        private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
        private Instant instante;

        public RelojMutable(Instant inicial) {
            this.instante = inicial;
        }

        public void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    /** Canal que guarda lo enviado y puede simular una caida. */
    public static final class CanalGrabador implements CanalDeCodigos {

        public record Envio(String telefono, String codigo) {
        }

        private final List<Envio> envios = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean caido;

        public void caido(boolean valor) {
            this.caido = valor;
        }

        public List<Envio> envios() {
            synchronized (envios) {
                return List.copyOf(envios);
            }
        }

        public String ultimoCodigo() {
            synchronized (envios) {
                return envios.get(envios.size() - 1).codigo();
            }
        }

        @Override
        public void enviar(String telefono, String codigo) {
            if (caido) {
                throw new CanalNoDisponibleException("El canal de pruebas esta caido");
            }
            envios.add(new Envio(telefono, codigo));
        }
    }
}
