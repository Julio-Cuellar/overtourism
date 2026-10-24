package mx.negocio360.restaurantes.aplicacion;

import java.util.List;

/** Las lineas pedidas desde un dispositivo y el estado de la cuenta, sin totales ni lineas de otros dispositivos (QR-11). */
public record EstadoDePedidoQrVista(EstadoPublicoDeCuenta cuenta, List<LineaDePedidoQrVista> lineas) {

    public EstadoDePedidoQrVista {
        lineas = List.copyOf(lineas);
    }
}
