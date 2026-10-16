package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Platillo;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * MN-10 DarDeBajaPlatillo (D-165): la baja es logica, exige {@code restaurantes.menu.editar} en preparar y en confirmar y
 * falla si el platillo esta en una cuenta abierta. Confirmar revalida y compara el resumen con el que se mostro: si el
 * platillo cambio entre los dos pasos, {@code COMANDO_CAMBIO}. Un platillo dado de baja deja de venderse y de pedirse, su
 * nombre queda libre y sus ventas anteriores no cambian.
 */
public final class ManejadorDarDeBajaPlatillo implements ManejadorComando<DarDeBajaPlatilloComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "dar_de_baja_platillo";
    /** Mensaje de exito. */
    public static final String PLANTILLA_BAJA = "Platillo dado de baja.";

    private final RepositorioMenu menu;
    private final UsoDeLosPlatillos uso;
    private final ConsultaDeRecetas recetas;
    private final AutorizacionDeRestaurantes autorizacion;
    private final Clock reloj;

    /**
     * Arma el manejador.
     *
     * @param menu         almacen del menu
     * @param uso          cuentas abiertas que lo usan (MN-10)
     * @param recetas      para avisar que la receta se conserva
     * @param autorizacion permisos de la sesion
     * @param reloj        reloj de la plataforma
     */
    public ManejadorDarDeBajaPlatillo(RepositorioMenu menu, UsoDeLosPlatillos uso, ConsultaDeRecetas recetas,
                                      AutorizacionDeRestaurantes autorizacion, Clock reloj) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.uso = Objects.requireNonNull(uso, "uso");
        this.recetas = Objects.requireNonNull(recetas, "recetas");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<DarDeBajaPlatilloComando> claseComando() {
        return DarDeBajaPlatilloComando.class;
    }

    /**
     * Valida y muestra lo que se daria de baja, sin escribir.
     *
     * @throws RestaurantesException PLATILLO_NO_ENCONTRADO o PLATILLO_EN_CUENTA_ABIERTA
     */
    @Override
    public String preparar(ContextoNegocio contexto, DarDeBajaPlatilloComando comando) {
        return resumen(contexto, platilloDeBaja(contexto, comando));
    }

    /**
     * Da de baja el platillo.
     *
     * @throws RestaurantesException COMANDO_CAMBIO si el platillo cambio, CONCURRENCIA, o los de preparar
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, DarDeBajaPlatilloComando comando, String resumenPreparado) {
        var platillo = platilloDeBaja(contexto, comando);
        if (!resumen(contexto, platillo).equals(resumenPreparado)) {
            throw new RestaurantesException(CodigoError.COMANDO_CAMBIO);
        }
        if (!menu.reemplazar(platillo.conBaja(reloj.instant()), platillo.version())) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        return ResultadoComando.ok(PLANTILLA_BAJA, platillo.id(), Map.of("platillo_id", platillo.id().toString()));
    }

    private Platillo platilloDeBaja(ContextoNegocio contexto, DarDeBajaPlatilloComando comando) {
        autorizacion.exigir(contexto, PermisosDeRestaurantes.MENU_EDITAR);
        if (comando.platilloId() == null) {
            throw new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO);
        }
        var platillo = menu.platillo(contexto.negocioId(), comando.platilloId()).filter(Platillo::activo)
                .orElseThrow(() -> new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO));
        if (uso.enCuentaAbierta(contexto.negocioId(), platillo.id())) {
            throw new RestaurantesException(CodigoError.PLATILLO_EN_CUENTA_ABIERTA);
        }
        return platillo;
    }

    private String resumen(ContextoNegocio contexto, Platillo platillo) {
        var texto = "Dar de baja «" + platillo.nombre() + "» (precio " + platillo.precio().toPlainString()
                + "). Deja de venderse y de pedirse; sus ventas anteriores no cambian.";
        if (recetas.tieneRecetaActiva(contexto.negocioId(), platillo.id())) {
            texto += " Su receta se conserva.";
        }
        return texto;
    }
}
