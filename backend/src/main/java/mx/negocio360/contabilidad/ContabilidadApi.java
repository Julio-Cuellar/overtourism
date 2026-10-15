package mx.negocio360.contabilidad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.Comando;
import mx.negocio360.contabilidad.aplicacion.PreparacionCreada;
import mx.negocio360.contabilidad.aplicacion.ResultadoComando;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ReglaContable;

/**
 * Funciones publicas del modulo (seccion 7 de {@code docs/diseno/contabilidad.md}). Los resolvedores GraphQL, los
 * controladores REST y los listeners llaman a esta interfaz y nunca a los repositorios. El negocio y el actor salen
 * siempre del contexto, nunca de un argumento (SG-02).
 */
public interface ContabilidadApi {

    /**
     * Valida un comando sin escribir y devuelve el token de un solo uso con el resumen para confirmar.
     *
     * @param contexto negocio y actor
     * @param comando  comando a preparar
     * @return token, resumen y vencimiento
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException con el codigo de la regla incumplida
     */
    PreparacionCreada preparar(ContextoNegocio contexto, Comando comando);

    /**
     * Ejecuta el comando preparado.
     *
     * @param contexto negocio y actor; los mismos que al preparar
     * @param token    token devuelto al preparar
     * @return el resultado
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException {@code TOKEN_*}, {@code COMANDO_CAMBIO} o el
     *                                                                  codigo de la regla incumplida
     */
    ResultadoComando confirmar(ContextoNegocio contexto, String token);

    /**
     * Catalogo de cuentas del negocio.
     *
     * @param contexto negocio
     * @return las cuentas ordenadas por codigo; vacio si la contabilidad no esta activa
     */
    List<Cuenta> catalogo(ContextoNegocio contexto);

    /**
     * Reglas de contabilizacion del negocio.
     *
     * @param contexto negocio
     * @return las reglas; vacio si la contabilidad no esta activa
     */
    List<ReglaContable> reglas(ContextoNegocio contexto);

    /**
     * Una poliza del negocio con sus renglones.
     *
     * @param contexto negocio
     * @param polizaId poliza
     * @return la poliza, o vacio si no existe o es de otro negocio (responden igual)
     */
    Optional<Poliza> poliza(ContextoNegocio contexto, UUID polizaId);
}
