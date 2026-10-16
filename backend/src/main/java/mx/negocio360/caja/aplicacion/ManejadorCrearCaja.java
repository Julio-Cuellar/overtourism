package mx.negocio360.caja.aplicacion;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;

/** CF-14 CrearCaja: nombre de 1 a 60 caracteres, unico por negocio sin importar mayusculas. */
public final class ManejadorCrearCaja implements ManejadorComando<CrearCajaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "crear_caja";
    static final int MAX_CARACTERES_NOMBRE = 60;

    private final RepositorioCaja cajas;
    private final Clock reloj;

    /**
     * Arma el manejador.
     *
     * @param cajas tabla caja
     * @param reloj reloj de la plataforma
     */
    public ManejadorCrearCaja(RepositorioCaja cajas, Clock reloj) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<CrearCajaComando> claseComando() {
        return CrearCajaComando.class;
    }

    /** @throws CajaException NOMBRE_CAJA_INVALIDO o CAJA_DUPLICADA */
    @Override
    public String preparar(ContextoNegocio contexto, CrearCajaComando comando) {
        return PlantillasCaja.resumenCrearCaja(nombreValido(contexto, comando));
    }

    /** @throws CajaException los mismos de preparar, revisados de nuevo */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, CrearCajaComando comando, String resumenPreparado) {
        var nombre = nombreValido(contexto, comando);
        var caja = new Caja(UuidV7.generar(reloj.instant()), contexto.negocioId(), nombre, true);
        cajas.guardarCaja(caja);
        return ResultadoComando.ok(PlantillasCaja.CAJA_CREADA, caja.id(), Map.of("caja_id", caja.id().toString()));
    }

    private String nombreValido(ContextoNegocio contexto, CrearCajaComando comando) {
        var nombre = Validaciones.textoObligatorio(comando.nombre(), MAX_CARACTERES_NOMBRE);
        if (nombre == null) {
            throw new CajaException(CodigoError.NOMBRE_CAJA_INVALIDO);
        }
        var clave = nombre.toLowerCase(Locale.ROOT);
        var repetida = cajas.cajas(contexto.negocioId()).stream()
                .anyMatch(c -> c.nombre().toLowerCase(Locale.ROOT).equals(clave));
        if (repetida) {
            throw new CajaException(CodigoError.CAJA_DUPLICADA);
        }
        return nombre;
    }
}
