package mx.negocio360.compartido.comandos;

/**
 * Comando de escritura que inicia una persona y que pasa por preparar -&gt; confirmar (D-23, D-55).
 *
 * <p>Cada comando se serializa a JSON para guardarse en la preparacion, asi que debe ser un {@code record} de datos
 * simples (textos, numeros, {@code BigDecimal}, {@code UUID}, enums y listas de ellos).
 */
public interface Comando {
}
