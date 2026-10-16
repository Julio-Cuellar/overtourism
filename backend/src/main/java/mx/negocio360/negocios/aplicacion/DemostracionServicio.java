package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.negocios.ActualizarNegocio;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.DefinirAtributos;
import mx.negocio360.negocios.DemostracionApi;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.aplicacion.puertos.ModoDemostracion;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioDemostracion;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPreparacionesResena;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioResenas;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import mx.negocio360.negocios.dominio.Resena;
import org.springframework.transaction.support.TransactionOperations;

/**
 * La demo del stand: siembra los doce negocios simulados y restablece lo que hicieron los visitantes. Solo funciona con el
 * modo demostracion encendido (DM-03).
 *
 * <p>DM-02: cada negocio nace por el mismo camino del alta (un {@code negocio.registro_solicitado} sintetico de una cuenta de
 * demostracion) y se completa con los casos de uso del panel, no con SQL, para que la demo ejercite el codigo real. Luego se
 * marca como simulado (DM-01) y se le siembran sus reseñas (DM-05). Cada paso es su propia transaccion: una transaccion no
 * puede trabajar con dos negocios (SN-02). DM-04: restablecer borra todo lo que hay de reseñas, disponibilidad y limites en los
 * negocios simulados y vuelve a sembrar sus reseñas; no lee ni toca a ningun otro.
 */
public final class DemostracionServicio implements DemostracionApi {

    private static final Autorizacion TODO = Autorizacion.de(java.util.Set.copyOf(
            new PermisosDeNegocios().permisos().stream().map(p -> p.codigo()).toList()));

    private final ModoDemostracion modo;
    private final AltasApi altas;
    private final NegociosApi panel;
    private final RepositorioDemostracion demostracion;
    private final RepositorioNegocios negocios;
    private final RepositorioResenas resenas;
    private final RepositorioPreparacionesResena preparaciones;
    private final TransactionOperations transacciones;
    private final Clock reloj;

    public DemostracionServicio(ModoDemostracion modo, AltasApi altas, NegociosApi panel,
                                RepositorioDemostracion demostracion, RepositorioNegocios negocios,
                                RepositorioResenas resenas, RepositorioPreparacionesResena preparaciones,
                                TransactionOperations transacciones, Clock reloj) {
        this.modo = Objects.requireNonNull(modo, "modo");
        this.altas = Objects.requireNonNull(altas, "altas");
        this.panel = Objects.requireNonNull(panel, "panel");
        this.demostracion = Objects.requireNonNull(demostracion, "demostracion");
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.resenas = Objects.requireNonNull(resenas, "resenas");
        this.preparaciones = Objects.requireNonNull(preparaciones, "preparaciones");
        this.transacciones = Objects.requireNonNull(transacciones, "transacciones");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ResultadoSiembra sembrar() {
        modo.exigirActivo();
        var ids = new ArrayList<UUID>();
        var creados = 0;
        var sembradas = 0;
        for (var n : CatalogoDeDemostracion.NEGOCIOS) {
            ids.add(n.id());
            if (demostracion.esSimulado(n.id())) {
                continue;
            }
            altas.registrarAlta(new NegocioRegistroSolicitadoV1(n.id(), CatalogoDeDemostracion.CUENTA, n.nombre(),
                    n.giro(), n.eventoId(), CatalogoDeDemostracion.id(0xCC, n.indice())));
            completar(n);
            demostracion.marcarSimulado(n.id());
            sembradas += sembrarResenas(n);
            creados++;
        }
        return new ResultadoSiembra(ids, creados, sembradas);
    }

    @Override
    public ResultadoRestablecimiento restablecer() {
        modo.exigirActivo();
        var catalogo = new java.util.HashMap<UUID, CatalogoDeDemostracion.Negocio>();
        CatalogoDeDemostracion.NEGOCIOS.forEach(n -> catalogo.put(n.id(), n));
        var negociosRestablecidos = 0;
        var borradas = 0;
        var restauradas = 0;
        for (var id : demostracion.negocios()) {
            var definicion = catalogo.get(id);
            if (definicion == null) {
                continue;
            }
            var cuentas = transacciones.execute(estado -> {
                var b = resenas.borrarTodas(id);
                negocios.borrarDisponibilidad(id);
                preparaciones.borrarDeNegocio(id);
                var r = sembrarResenas(definicion);
                return new int[] {b, r};
            });
            negociosRestablecidos++;
            borradas += cuentas[0];
            restauradas += cuentas[1];
        }
        return new ResultadoRestablecimiento(negociosRestablecidos, borradas, restauradas);
    }

    // ------------------------------------------------------------------------------------------------- apoyo

    /** Perfil, ubicacion, horario, categorias, atributos, disponibilidad y publicacion, por los casos de uso del panel. */
    private void completar(CatalogoDeDemostracion.Negocio n) {
        var contexto = new ContextoNegocio(n.id(), "demostracion-" + CatalogoDeDemostracion.CUENTA);
        var sucursal = panel.sucursales(contexto, TODO).get(0);
        panel.actualizarSucursal(contexto, TODO, sucursal.id(), new ActualizarSucursal(0, null, n.calle(), n.numero(),
                n.colonia(), n.municipio(), "Puebla", n.codigoPostal(), null, n.lat(), n.lon(), null));
        var dias = new ArrayList<DiaHorarioComando>();
        for (var dia = 1; dia <= 7; dia++) {
            dias.add(new DiaHorarioComando(dia, List.of(new TramoComando(n.abre(), n.cierra()))));
        }
        panel.definirHorario(contexto, TODO, sucursal.id(), 0, dias);
        panel.actualizarNegocio(contexto, TODO, new ActualizarNegocio(version(contexto), null, n.descripcion(), null,
                null, null));
        panel.definirSubcategorias(contexto, TODO, version(contexto), n.subcategorias());
        panel.definirAtributos(contexto, TODO, new DefinirAtributos(version(contexto), n.dieta(), n.accesibilidad(),
                n.duracionMin(), n.costoMxn()));
        if (n.disponibilidad()) {
            panel.definirDisponibilidad(contexto, TODO, "MODERADA", "Datos de demostración");
        }
        panel.publicarNegocio(contexto, TODO, true);
    }

    private long version(ContextoNegocio contexto) {
        return panel.miNegocio(contexto, TODO).version();
    }

    /** Inserta las reseñas del catalogo (DM-05) y recalcula el resumen, todo en una transaccion del negocio. */
    private int sembrarResenas(CatalogoDeDemostracion.Negocio n) {
        return transacciones.execute(estado -> {
            var ahora = reloj.instant();
            var insertadas = 0;
            for (var i = 0; i < n.resenas().size(); i++) {
                var r = n.resenas().get(i);
                var creada = ahora.minus(Duration.ofDays(r.hacenDias()));
                var respondida = r.respuesta() == null ? null : creada.plus(Duration.ofHours(5));
                var resena = new Resena(UuidV7.generar(creada), n.id(), n.autor(i), r.calificacion(), r.comentario(),
                        Resena.Estado.PUBLICADA, Resena.Origen.DEMOSTRACION, r.respuesta(), null, respondida, creada,
                        null, 0);
                if (resenas.insertar(resena)) {
                    insertadas++;
                }
            }
            resenas.recalcularResumen(n.id(), ahora);
            return insertadas;
        });
    }
}
