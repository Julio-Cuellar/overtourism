package mx.negocio360.negocios.infraestructura;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.MembresiaAgregadaV1;
import org.springframework.modulith.ApplicationModuleListener;

/**
 * El aviso en vivo del alta (W1, {@code altaDeNegocio}). Escucha el bus y avisa a quien pidio el alta: {@code RECHAZADO}
 * cuando Negocios publica {@code negocio.registro_rechazado}, y {@code REGISTRADO} cuando Identidad le da la membresia
 * de dueno inicial, o sea DESPUES de que el acceso ya existe, asi que cuando la app consulta {@code /auth/yo} ya ve su
 * negocio. Es un atajo y no la verdad: el aviso no se guarda y la consulta {@code misAltasDeNegocio} dice lo mismo.
 *
 * <p>Una suscripcion por cuenta (la segunda cierra la primera) y se cierra sola al emitir un estado final. El resolvedor
 * de la suscripcion llega con el filtro de entrada (IDN-4); aqui solo vive el centro de avisos, sin dependencias de red.
 */
public class AvisosDeAlta {

    /** Quien recibe los avisos: normalmente la suscripcion de GraphQL de una conexion. */
    public interface Suscriptor {
        void aviso(Aviso aviso);

        void completo();
    }

    /** Lo que se avisa: el id del negocio, el estado final y, si se rechazo, el codigo del motivo. */
    public record Aviso(UUID negocioId, EstadoAlta estado, String motivo) {
    }

    /** Se cancela al cerrarse la conexion. */
    public interface Suscripcion extends AutoCloseable {
        @Override
        void close();
    }

    private final AltasApi altas;
    private final Map<UUID, Suscriptor> suscriptores = new ConcurrentHashMap<>();

    public AvisosDeAlta(AltasApi altas) {
        this.altas = Objects.requireNonNull(altas, "altas");
    }

    public Suscripcion suscribir(UUID cuentaId, Suscriptor suscriptor) {
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(suscriptor, "suscriptor");
        var anterior = suscriptores.put(cuentaId, suscriptor);
        if (anterior != null) {
            completarSinFallar(anterior);
        }
        return () -> suscriptores.remove(cuentaId, suscriptor);
    }

    public int suscritas() {
        return suscriptores.size();
    }

    @ApplicationModuleListener
    public void alRecibir(SobreEvento evento) {
        MembresiaAgregadaV1.de(evento).filter(MembresiaAgregadaV1::esDuenoInicial).ifPresent(this::alDuenoInicial);
        rechazo(evento);
    }

    private void alDuenoInicial(MembresiaAgregadaV1 membresia) {
        if (!suscriptores.containsKey(membresia.cuentaId())) {
            return;
        }
        // La verdad es la base: solo se avisa REGISTRADO si el alta de esa cuenta de verdad quedo registrada.
        var registrada = altas.misAltas(membresia.cuentaId()).stream().anyMatch(
                a -> a.negocioId().equals(membresia.negocioId()) && a.estado() == EstadoAlta.REGISTRADO);
        if (registrada) {
            avisar(membresia.cuentaId(), new Aviso(membresia.negocioId(), EstadoAlta.REGISTRADO, null));
        }
    }

    private void rechazo(SobreEvento evento) {
        if (!"negocio.registro_rechazado".equals(evento.tipo()) || evento.version() != 1
                || !"negocios".equals(evento.origen())) {
            return;
        }
        if (!(evento.datos().get("solicitado_por") instanceof String cuenta)
                || !(evento.datos().get("motivo") instanceof String motivo)) {
            return;
        }
        try {
            avisar(UUID.fromString(cuenta), new Aviso(evento.negocioId(), EstadoAlta.RECHAZADO, motivo));
        } catch (IllegalArgumentException e) {
            // Cuenta mal formada: no se puede avisar a nadie y reintentarlo no lo arregla (EV-03).
        }
    }

    private void avisar(UUID cuentaId, Aviso aviso) {
        var suscriptor = suscriptores.remove(cuentaId);
        if (suscriptor == null) {
            return;
        }
        try {
            suscriptor.aviso(aviso);
            suscriptor.completo();
        } catch (RuntimeException e) {
            // Un socket roto no debe tumbar al listener: el aviso es un atajo y la consulta de respaldo dice lo mismo.
        }
    }

    private static void completarSinFallar(Suscriptor suscriptor) {
        try {
            suscriptor.completo();
        } catch (RuntimeException e) {
            // Igual que arriba.
        }
    }
}
