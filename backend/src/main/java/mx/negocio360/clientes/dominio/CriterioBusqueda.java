package mx.negocio360.clientes.dominio;

import java.util.regex.Pattern;

/**
 * Lo que se busca (I-04): todos, parte del nombre o apellidos, un telefono completo, o nada. Un texto hecho solo de
 * digitos y signos de telefono que no sea un numero completo no busca nada: nunca se busca por telefono parcial.
 * El correo no es criterio de busqueda.
 *
 * <p>{@link #coincide(Cliente)} es la regla; el repositorio real debe producir el mismo resultado con SQL (trigram
 * sobre {@code nombre_busqueda} y igualdad sobre {@code telefono}). Solo los clientes activos aparecen.
 */
public sealed interface CriterioBusqueda {

    Pattern SOLO_SIGNOS_DE_TELEFONO = Pattern.compile("[0-9+()\\s\\p{Z}.\\-]+");

    record Todos() implements CriterioBusqueda { }

    /** {@code texto} ya viene normalizado (minusculas, sin acentos). */
    record PorNombre(String texto) implements CriterioBusqueda { }

    /** {@code telefono} ya viene en E.164. */
    record PorTelefono(String telefono) implements CriterioBusqueda { }

    record Ninguno() implements CriterioBusqueda { }

    static CriterioBusqueda de(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Todos();
        }
        var recortado = texto.strip();
        if (SOLO_SIGNOS_DE_TELEFONO.matcher(recortado).matches()) {
            return Telefono.normalizar(recortado).<CriterioBusqueda>map(PorTelefono::new).orElseGet(Ninguno::new);
        }
        var normalizado = TextoBusqueda.normalizar(recortado);
        // Un texto hecho solo de marcas combinantes queda vacio; "contiene vacio" coincidiria con todos.
        return normalizado.isEmpty() ? new Ninguno() : new PorNombre(normalizado);
    }

    default boolean coincide(Cliente cliente) {
        if (cliente.estado() != EstadoCliente.ACTIVO) {
            return false;
        }
        return switch (this) {
            case Todos todos -> true;
            case PorNombre porNombre -> cliente.datos().nombreBusqueda().contains(porNombre.texto());
            case PorTelefono porTelefono -> porTelefono.telefono().equals(cliente.datos().telefono());
            case Ninguno ninguno -> false;
        };
    }
}
