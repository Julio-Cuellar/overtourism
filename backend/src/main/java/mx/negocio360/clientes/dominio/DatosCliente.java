package mx.negocio360.clientes.dominio;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Datos personales del cliente ya validados y normalizados. Son los unicos que se guardan (P-01): nombre,
 * apellidos, telefono, correo, fecha de nacimiento y notas. El constructor normaliza, de modo que no existe una
 * instancia sin normalizar.
 *
 * <p>Nombre y apellidos rechazan controles y caracteres invisibles (SG-17); las notas los descartan, salvo saltos
 * de linea y tabulaciones. El {@code toString} no imprime los campos: un registro o un mensaje de error no debe
 * poder copiarlos (P-06).
 */
public record DatosCliente(String nombre, String apellidos, String telefono, String correo,
                           LocalDate fechaNacimiento, String notas) {

    public static final int MAX_NOMBRE = 80;
    public static final int MAX_NOTAS = 500;

    public DatosCliente {
        nombre = nombreValido(nombre);
        if (nombre == null) {
            throw new ClientesException(CodigoError.NOMBRE_INVALIDO);
        }
        apellidos = nombreValido(apellidos);
        telefono = normalizarOpcional(telefono, CodigoError.TELEFONO_INVALIDO, Telefono::normalizar);
        correo = normalizarOpcional(correo, CodigoError.CORREO_INVALIDO, Correo::normalizar);
        notas = notasValidas(notas);
    }

    /** Valida tambien que la fecha de nacimiento no sea futura (I-08). {@code hoy} es la fecha local de Mexico. */
    public static DatosCliente de(String nombre, String apellidos, String telefono, String correo,
                                  LocalDate fechaNacimiento, String notas, LocalDate hoy) {
        var datos = new DatosCliente(nombre, apellidos, telefono, correo, fechaNacimiento, notas);
        if (fechaNacimiento != null && fechaNacimiento.isAfter(hoy)) {
            throw new ClientesException(CodigoError.FECHA_NACIMIENTO_INVALIDA);
        }
        return datos;
    }

    public String nombreCompleto() {
        return apellidos == null ? nombre : nombre + " " + apellidos;
    }

    /** Nombre y apellidos sin acentos ni mayusculas, para buscar (I-04). */
    public String nombreBusqueda() {
        return TextoBusqueda.normalizar(nombreCompleto());
    }

    @Override
    public String toString() {
        return "DatosCliente[***]";
    }

    /** Recorta, pasa los espacios Unicode a espacios normales y rechaza caracteres prohibidos. Vacio queda nulo. */
    private static String nombreValido(String valor) {
        var recortado = CaracteresSeguros.recortar(valor);
        if (recortado == null) {
            return null;
        }
        if (recortado.length() > MAX_NOMBRE || CaracteresSeguros.contieneProhibidos(recortado, false)) {
            throw new ClientesException(CodigoError.NOMBRE_INVALIDO);
        }
        return CaracteresSeguros.conEspaciosNormales(recortado);
    }

    private static String notasValidas(String valor) {
        var recortado = CaracteresSeguros.recortar(valor);
        if (recortado == null) {
            return null;
        }
        var limpias = CaracteresSeguros.recortar(CaracteresSeguros.sinProhibidos(recortado, true));
        if (limpias != null && limpias.length() > MAX_NOTAS) {
            throw new ClientesException(CodigoError.NOTAS_MUY_LARGAS);
        }
        return limpias;
    }

    private static String normalizarOpcional(String valor, CodigoError errorSiInvalido,
                                             java.util.function.Function<String, Optional<String>> normalizador) {
        var recortado = CaracteresSeguros.recortar(valor);
        if (recortado == null) {
            return null;
        }
        return normalizador.apply(recortado).orElseThrow(() -> new ClientesException(errorSiInvalido));
    }
}
