package mx.negocio360.restaurantes.infraestructura;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lo mismo que {@code @ApplicationModuleListener} (asincrono, en su propia transaccion y despues de que confirme la del
 * publicador, para que el outbox de Modulith reintente), <b>pero tambien recibe el evento si se publico sin transaccion</b>
 * ({@code fallbackExecution}). Con Ventas en memoria (perfil {@code desarrollo}) el evento se publica sin transaccion y
 * {@code @ApplicationModuleListener} lo descartaba en silencio: la venta no descontaba insumos. Caja en memoria resuelve lo
 * mismo con {@code @EventListener}; aqui la misma escucha sirve para los dos casos.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
@TransactionalEventListener(fallbackExecution = true)
public @interface EscuchaDeEventos {
}
