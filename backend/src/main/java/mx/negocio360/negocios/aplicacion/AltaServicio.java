package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioAltas;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.Negocio;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.NombreNegocio;
import mx.negocio360.negocios.dominio.Sucursal;

/**
 * Alta automatica del negocio (AL-01, AL-02, D-90): si el giro y el nombre sirven, nace activo, sin publicar y con su
 * sucursal principal, y se publica {@code negocio.registrado}; si no, se guarda el rechazo y se publica
 * {@code negocio.registro_rechazado}. Procesar el mismo pedido dos veces no cambia nada (NG-03). Java puro: la
 * transaccion la pone quien lo envuelve ({@code FronteraTransaccional}).
 */
public final class AltaServicio implements AltasApi {

    static final String LISTENER = "negocios.alta";
    static final String REGISTRADO = "negocio.registrado";
    static final String RECHAZADO = "negocio.registro_rechazado";

    private final RepositorioAltas altas;
    private final RepositorioNegocios negocios;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public AltaServicio(RepositorioAltas altas, RepositorioNegocios negocios, PublicadorEventos eventos, Clock reloj) {
        this.altas = Objects.requireNonNull(altas, "altas");
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ResultadoAlta registrarAlta(NegocioRegistroSolicitadoV1 pedido) {
        var ahora = reloj.instant();
        if (!altas.marcarEventoProcesado(pedido.eventoId(), LISTENER, ahora)) {
            return ResultadoAlta.YA_PROCESADO;
        }
        var giro = pedido.giro().strip().toUpperCase(java.util.Locale.ROOT);
        String motivo = null;
        NombreNegocio nombre = null;
        try {
            nombre = NombreNegocio.de(pedido.nombre());
        } catch (NegociosException e) {
            motivo = e.codigo().name();
        }
        if (motivo == null && !altas.giroActivo(giro)) {
            motivo = CodigoError.GIRO_INVALIDO.name();
        }
        var estado = motivo == null ? EstadoAlta.REGISTRADO : EstadoAlta.RECHAZADO;
        var guardado = altas.insertarAlta(new RepositorioAltas.Registro(pedido.negocioId(), pedido.cuentaId(),
                nombreParaElRegistro(pedido.nombre()), recortar(giro, 40), estado, motivo, pedido.eventoId(), ahora));
        if (!guardado) {
            // Otro evento con el mismo negocio ya lo habia resuelto: no se crea ni se publica nada mas.
            return ResultadoAlta.YA_PROCESADO;
        }
        if (motivo != null) {
            eventos.publicar(FabricaSobres.de(pedido.negocioId(), ahora, pedido.correlacionId(), pedido.eventoId(),
                    RECHAZADO, 1, Map.of("solicitado_por", pedido.cuentaId().toString(),
                            "nombre", nombreParaElRegistro(pedido.nombre()), "giro", recortar(giro, 40),
                            "motivo", motivo)));
            return ResultadoAlta.RECHAZADO;
        }
        var negocio = Negocio.nuevo(pedido.negocioId(), nombre, giro, pedido.cuentaId(), ahora);
        negocios.crear(negocio, Sucursal.principal(UuidV7.generar(ahora), pedido.negocioId(), ahora));
        eventos.publicar(FabricaSobres.de(pedido.negocioId(), ahora, pedido.correlacionId(), pedido.eventoId(),
                REGISTRADO, 1, Map.of("solicitado_por", pedido.cuentaId().toString(), "nombre", nombre.valor(),
                        "giro", giro)));
        return ResultadoAlta.REGISTRADO;
    }

    @Override
    public List<AltaVista> misAltas(UUID cuentaId) {
        return altas.altasDeCuenta(Objects.requireNonNull(cuentaId, "cuentaId"));
    }

    /** Lo que escribio la persona, recortado a 80 y sin caracteres de control, para guardarlo aunque sea invalido. */
    private static String nombreParaElRegistro(String nombre) {
        var sinControl = nombre.codePoints().filter(c -> !Character.isISOControl(c)
                && Character.getType(c) != Character.FORMAT).collect(StringBuilder::new,
                StringBuilder::appendCodePoint, StringBuilder::append).toString().strip();
        return recortar(sinControl, NombreNegocio.MAXIMO);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
