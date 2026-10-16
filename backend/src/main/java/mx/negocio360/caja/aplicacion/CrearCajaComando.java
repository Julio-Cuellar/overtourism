package mx.negocio360.caja.aplicacion;

import mx.negocio360.compartido.comandos.Comando;
import org.springframework.modulith.NamedInterface;

/**
 * CF-14 CrearCaja.
 *
 * @param nombre de 1 a 60 caracteres, unico por negocio sin importar mayusculas
 */
@NamedInterface("comandos")
public record CrearCajaComando(String nombre) implements Comando {
}
