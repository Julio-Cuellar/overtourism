package mx.negocio360.compartido.transaccion;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Hace que cada llamada a una API sea una sola transaccion, sin anotaciones de Spring en la capa de aplicacion: sus
 * clases son {@code final} y no admiten el proxy de {@code @Transactional}. Es un proxy sobre la interfaz.
 *
 * <p>La transaccion se deshace ante cualquier error, salvo los de negocio que se declaren en {@code noDeshacerPor}.
 * Esos se confirman y se vuelven a lanzar tal cual: un intento fallido de codigo que se cuenta, por ejemplo, tiene que
 * sobrevivir al error que provoca. {@code equals}, {@code hashCode} y {@code toString} no abren transaccion.
 */
public final class FronteraTransaccional {

    private FronteraTransaccional() {
    }

    @SafeVarargs
    public static <T> T envolver(Class<T> api, Object objetivo, TransactionOperations transacciones,
                                 Class<? extends RuntimeException>... noDeshacerPor) {
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(objetivo, "objetivo");
        Objects.requireNonNull(transacciones, "transacciones");
        if (!api.isInterface()) {
            throw new IllegalArgumentException(api.getName() + " no es una interfaz");
        }
        var heredadas = api.getInterfaces();
        var cumple = api.isInstance(objetivo)
                || (heredadas.length > 0 && Arrays.stream(heredadas).allMatch(i -> i.isInstance(objetivo)));
        if (!cumple) {
            throw new IllegalArgumentException(objetivo.getClass().getName() + " no implementa " + api.getName());
        }
        var controlador = new Controlador(api, objetivo, transacciones, List.of(noDeshacerPor));
        return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, controlador));
    }

    /** Lleva una excepcion verificada fuera de la transaccion, que la deshace, para relanzarla intacta. */
    private static final class Verificada extends RuntimeException {
        Verificada(Throwable causa) {
            super(causa);
        }
    }

    /** Resultado de una llamada: el valor, o el error de negocio que se confirma y se relanza despues. */
    private record Resultado(Object valor, RuntimeException errorDeNegocio) {
    }

    private static final class Controlador implements InvocationHandler {

        private final Class<?> api;
        private final Object objetivo;
        private final TransactionOperations transacciones;
        private final List<Class<? extends RuntimeException>> noDeshacerPor;

        Controlador(Class<?> api, Object objetivo, TransactionOperations transacciones,
                    List<Class<? extends RuntimeException>> noDeshacerPor) {
            this.api = api;
            this.objetivo = objetivo;
            this.transacciones = transacciones;
            this.noDeshacerPor = noDeshacerPor;
        }

        @Override
        public Object invoke(Object proxy, Method metodo, Object[] argumentos) throws Throwable {
            if (metodo.getDeclaringClass() == Object.class) {
                return deObject(proxy, metodo, argumentos);
            }
            Resultado resultado;
            try {
                resultado = transacciones.execute(estado -> ejecutar(metodo, argumentos));
            } catch (Verificada verificada) {
                throw verificada.getCause();
            }
            if (resultado.errorDeNegocio() != null) {
                throw resultado.errorDeNegocio();
            }
            return resultado.valor();
        }

        private Resultado ejecutar(Method metodo, Object[] argumentos) {
            try {
                return new Resultado(metodo.invoke(objetivo, argumentos), null);
            } catch (InvocationTargetException e) {
                var causa = e.getCause();
                if (causa instanceof RuntimeException error && noDeshacerPor.stream().anyMatch(t -> t.isInstance(error))) {
                    return new Resultado(null, error);
                }
                if (causa instanceof RuntimeException error) {
                    throw error;
                }
                if (causa instanceof Error error) {
                    throw error;
                }
                throw new Verificada(causa);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("No se pudo llamar a " + metodo, e);
            }
        }

        private Object deObject(Object proxy, Method metodo, Object[] argumentos) {
            return switch (metodo.getName()) {
                case "equals" -> proxy == argumentos[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> "FronteraTransaccional de " + api.getSimpleName();
            };
        }
    }
}
