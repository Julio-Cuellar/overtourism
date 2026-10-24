package mx.negocio360.restaurantes.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Una linea de una cuenta (CT-02, CT-03): un platillo, su cantidad y lo que el cliente dijo. Guarda el nombre y el precio
 * del momento en que se agrego; el precio vigente se toma al pedir la cuenta (CT-05, CT-13). Es inmutable.
 *
 * @param id             identificador
 * @param platilloId     el platillo, que es el {@code producto_id} que conoce Ventas (D-162)
 * @param nombre         nombre del platillo al agregarla
 * @param precio         precio unitario
 * @param cantidad       entera, de 1 a 99
 * @param nota           hasta 80 caracteres; se borra a los 30 dias (CT-12)
 * @param marcaAlergia   resalta la linea en el tablero; no guarda detalle medico (CD-06)
 * @param estado         pendiente, enviada o anulada
 * @param estacion       cocina o barra, la de la categoria del platillo (CT-04)
 * @param comandaId      la comanda a la que se envio, o nulo
 * @param envioId        el envio al que pertenece, o nulo
 * @param dispositivoHash hash con clave del dispositivo que la pidio por QR, o nulo (QR-12); se borra a los 30 dias
 * @param motivo         por que se anulo, o nulo
 * @param textoDelMotivo texto libre de hasta 80 caracteres, solo con {@link MotivoDeAnulacion#OTRO}
 * @param creadaEn       cuando se agrego
 * @param anuladaEn      cuando se anulo, o nulo
 */
public record LineaDeCuenta(UUID id, UUID platilloId, String nombre, BigDecimal precio, int cantidad, String nota,
                            boolean marcaAlergia, EstadoDeLinea estado, Estacion estacion, UUID comandaId, UUID envioId,
                            String dispositivoHash, MotivoDeAnulacion motivo, String textoDelMotivo, Instant creadaEn,
                            Instant anuladaEn) {

    /** Cantidad maxima por linea (CT-02). */
    public static final int MAX_CANTIDAD = 99;
    /** Largo maximo de la nota y del texto de un motivo (CT-02, QR-10). */
    public static final int MAX_TEXTO = 80;

    public LineaDeCuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(platilloId, "platilloId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(precio, "precio");
        if (cantidad < 1 || cantidad > MAX_CANTIDAD) {
            throw new RestaurantesException(CodigoError.CANTIDAD_INVALIDA);
        }
        nota = nota == null || nota.isBlank() ? null : nota.strip();
        if (nota != null && nota.length() > MAX_TEXTO) {
            throw new RestaurantesException(CodigoError.NOTA_INVALIDA);
        }
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    /** Una linea nueva del personal: pendiente de enviar a cocina. */
    public static LineaDeCuenta pendiente(UUID id, UUID platilloId, String nombre, BigDecimal precio, int cantidad, String nota,
                                          boolean marcaAlergia, Estacion estacion, Instant ahora) {
        return new LineaDeCuenta(id, platilloId, nombre, precio, cantidad, nota, marcaAlergia, EstadoDeLinea.PENDIENTE,
                estacion, null, null, null, null, null, ahora, null);
    }

    /** Una linea de un pedido por QR: nace enviada, directo a cocina (CT-03, QR-06). */
    public static LineaDeCuenta porQr(UUID id, UUID platilloId, String nombre, BigDecimal precio, int cantidad, String nota,
                                      boolean marcaAlergia, Estacion estacion, UUID comandaId, UUID envioId,
                                      String dispositivoHash, Instant ahora) {
        return new LineaDeCuenta(id, platilloId, nombre, precio, cantidad, nota, marcaAlergia, EstadoDeLinea.ENVIADA,
                estacion, comandaId, envioId, dispositivoHash, null, null, ahora, null);
    }

    /** Lo que cuesta la linea: cero si esta anulada. */
    public BigDecimal subtotal() {
        return estado == EstadoDeLinea.ANULADA ? BigDecimal.ZERO : precio.multiply(BigDecimal.valueOf(cantidad));
    }

    /** Hace un cambio y devuelve la linea nueva. */
    public LineaDeCuenta cambiar(Consumer<Cambios> cambio) {
        var c = new Cambios(this);
        cambio.accept(c);
        return c.construir();
    }

    /** Los campos que pueden cambiar de una linea, para armar la nueva sin repetir el constructor. */
    public static final class Cambios {
        public BigDecimal precio;
        public String nota;
        public EstadoDeLinea estado;
        public UUID comandaId;
        public UUID envioId;
        public String dispositivoHash;
        public MotivoDeAnulacion motivo;
        public String textoDelMotivo;
        public Instant anuladaEn;
        private final LineaDeCuenta base;

        private Cambios(LineaDeCuenta base) {
            this.base = base;
            this.precio = base.precio;
            this.nota = base.nota;
            this.estado = base.estado;
            this.comandaId = base.comandaId;
            this.envioId = base.envioId;
            this.dispositivoHash = base.dispositivoHash;
            this.motivo = base.motivo;
            this.textoDelMotivo = base.textoDelMotivo;
            this.anuladaEn = base.anuladaEn;
        }

        private LineaDeCuenta construir() {
            return new LineaDeCuenta(base.id, base.platilloId, base.nombre, precio, base.cantidad, nota, base.marcaAlergia,
                    estado, base.estacion, comandaId, envioId, dispositivoHash, motivo, textoDelMotivo, base.creadaEn,
                    anuladaEn);
        }
    }
}
