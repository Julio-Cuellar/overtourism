/**
 * Canal del panel y de la app (F4): la puerta GraphQL de la plataforma (D-55) para Ventas y Caja. Toma el negocio y
 * el actor de la sesion de Identidad (nunca de la peticion), enruta {@code prepararComando} y
 * {@code confirmarComando} al modulo dueno del comando, expone las consultas y traduce los errores de dominio a su
 * codigo y su texto fijo.
 *
 * <p>{@code canal.demo} simula Catalogo, Pasaporte y los permisos mientras esos modulos no existan; todo lo que sale
 * de ahi va rotulado como simulado. El canal entero solo se enciende con {@code negocio360.demo.habilitada=true} y
 * nunca con el perfil {@code produccion}.
 */
@ApplicationModule(displayName = "Canal", allowedDependencies = {"compartido", "identidad",
        "ventas", "ventas :: puertos", "ventas :: dominio", "ventas :: comandos", "ventas :: textos",
        "caja", "caja :: puertos", "caja :: dominio", "caja :: comandos", "caja :: textos"})
package mx.negocio360.canal;

import org.springframework.modulith.ApplicationModule;
