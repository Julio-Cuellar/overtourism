package mx.negocio360.restaurantes.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * El servicio de un restaurante: si esta abierto (MS-05), si acepta pedidos por QR (QR-17), como atiende (MS-08) y cual fue
 * la ultima disponibilidad que se publico, para publicar solo lo que cambia (MS-06). Es inmutable.
 *
 * @param negocioId  negocio dueño
 * @param abierto    servicio abierto; sin esto no se abren cuentas
 * @param qrHabilitado falso si el local apago los pedidos por QR
 * @param modo       con mesas, mostrador o ambos
 * @param publicada  la ultima disponibilidad publicada, o nulo si nunca se publico
 */
public record Servicio(UUID negocioId, boolean abierto, boolean qrHabilitado, ModoServicio modo, Disponibilidad publicada) {

    public Servicio {
        Objects.requireNonNull(negocioId, "negocioId");
        modo = modo == null ? ModoServicio.AMBOS : modo;
    }

    /** Un servicio recien creado: cerrado, con QR encendido, en modo ambos y sin nada publicado. */
    public static Servicio inicial(UUID negocioId) {
        return new Servicio(negocioId, false, true, ModoServicio.AMBOS, null);
    }

    /** Abre o cierra el servicio. */
    public Servicio conAbierto(boolean nuevo) {
        return new Servicio(negocioId, nuevo, qrHabilitado, modo, publicada);
    }

    /** Enciende o apaga los pedidos por QR de todo el local. */
    public Servicio conQrHabilitado(boolean nuevo) {
        return new Servicio(negocioId, abierto, nuevo, modo, publicada);
    }

    /** Cambia el modo de servicio. */
    public Servicio conModo(ModoServicio nuevo) {
        return new Servicio(negocioId, abierto, qrHabilitado, nuevo, publicada);
    }

    /** Recuerda la ultima disponibilidad publicada. */
    public Servicio conPublicada(Disponibilidad nueva) {
        return new Servicio(negocioId, abierto, qrHabilitado, modo, nueva);
    }
}
