package mx.negocio360.restaurantes.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Una cuenta de mesa o de mostrador (CT-01 a CT-15). Vive en Restaurantes: Ventas registra una venta de un solo golpe y
 * una cuenta que dura una hora no cabe en ella (H-R2). No guarda datos personales del cliente (CT-12). Es inmutable; cada
 * cambio sube la version.
 *
 * @param id                  identificador
 * @param negocioId           negocio dueño
 * @param numero              consecutivo por negocio
 * @param mesaId              la mesa, o nulo en mostrador (MS-08)
 * @param estado              abierta, por cobrar, pagada o cancelada
 * @param origen              abierta por el personal o por el primer pedido por QR
 * @param comensales          de 1 a 50, o nulo si la abrio un pedido por QR (CT-01)
 * @param abiertaPor          quien la abrio, o nulo si fue por QR
 * @param abiertaEn           cuando se abrio
 * @param solicitadaEn        cuando se pidio la cuenta, o nulo
 * @param solicitadaPor       quien la pidio ({@code QR} o el actor), o nulo
 * @param cerradaEn           cuando se pago o se cancelo, o nulo
 * @param ventaId             la venta que la pago, o nulo (CT-08)
 * @param discrepancia        verdadero si las lineas vendidas difieren de las de la cuenta (CT-08, CT-09)
 * @param ventaAnulada        verdadero si la venta que la pago se anulo (CT-10)
 * @param presenciaConfirmada verdadero si un mesero ya esta con la mesa (QR-08)
 * @param totalCalculado      total al pedir la cuenta con los precios vigentes (CT-05), o nulo antes
 * @param motivoDeCierre      por que se cancelo, o nulo
 * @param lineas              las lineas, en el orden en que se agregaron
 * @param version             bloqueo optimista, desde 1
 */
public record Cuenta(UUID id, UUID negocioId, int numero, UUID mesaId, EstadoDeCuenta estado, OrigenDeCuenta origen,
                     Integer comensales, String abiertaPor, Instant abiertaEn, Instant solicitadaEn, String solicitadaPor,
                     Instant cerradaEn, UUID ventaId, boolean discrepancia, boolean ventaAnulada,
                     boolean presenciaConfirmada, BigDecimal totalCalculado, MotivoDeAnulacion motivoDeCierre,
                     List<LineaDeCuenta> lineas, long version) {

    /** Tope de comensales de una cuenta del personal (CT-01). */
    public static final int MAX_COMENSALES = 50;
    /** Prefijo de la referencia que viaja a Ventas (CT-07). */
    public static final String PREFIJO_DE_REFERENCIA = "restaurantes:cuenta:";

    public Cuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(origen, "origen");
        if (comensales != null && (comensales < 1 || comensales > MAX_COMENSALES)) {
            throw new RestaurantesException(CodigoError.COMENSALES_INVALIDOS);
        }
        Objects.requireNonNull(abiertaEn, "abiertaEn");
        lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
    }

    /** Una cuenta nueva, abierta y sin lineas. */
    public static Cuenta nueva(UUID id, UUID negocioId, int numero, UUID mesaId, OrigenDeCuenta origen, Integer comensales,
                               String abiertaPor, Instant ahora) {
        return new Cuenta(id, negocioId, numero, mesaId, EstadoDeCuenta.ABIERTA, origen, comensales, abiertaPor, ahora, null,
                null, null, null, false, false, false, null, null, List.of(), 1);
    }

    /** La referencia opaca con la que se cobra esta cuenta en Ventas (CT-07). */
    public String referencia() {
        return PREFIJO_DE_REFERENCIA + id;
    }

    /** Suma de las lineas no anuladas con el precio que tienen. */
    public BigDecimal total() {
        return lineas.stream().map(LineaDeCuenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Hace un cambio y devuelve la cuenta nueva con la version siguiente. */
    public Cuenta cambiar(Consumer<Cambios> cambio) {
        var c = new Cambios(this);
        cambio.accept(c);
        return c.construir();
    }

    /** Los campos que pueden cambiar de una cuenta, para armar la nueva sin repetir el constructor. */
    public static final class Cambios {
        public EstadoDeCuenta estado;
        public Instant solicitadaEn;
        public String solicitadaPor;
        public Instant cerradaEn;
        public UUID ventaId;
        public boolean discrepancia;
        public boolean ventaAnulada;
        public boolean presenciaConfirmada;
        public BigDecimal totalCalculado;
        public MotivoDeAnulacion motivoDeCierre;
        public List<LineaDeCuenta> lineas;
        private final Cuenta base;

        private Cambios(Cuenta base) {
            this.base = base;
            this.estado = base.estado;
            this.solicitadaEn = base.solicitadaEn;
            this.solicitadaPor = base.solicitadaPor;
            this.cerradaEn = base.cerradaEn;
            this.ventaId = base.ventaId;
            this.discrepancia = base.discrepancia;
            this.ventaAnulada = base.ventaAnulada;
            this.presenciaConfirmada = base.presenciaConfirmada;
            this.totalCalculado = base.totalCalculado;
            this.motivoDeCierre = base.motivoDeCierre;
            this.lineas = base.lineas;
        }

        private Cuenta construir() {
            return new Cuenta(base.id, base.negocioId, base.numero, base.mesaId, estado, base.origen, base.comensales,
                    base.abiertaPor, base.abiertaEn, solicitadaEn, solicitadaPor, cerradaEn, ventaId, discrepancia,
                    ventaAnulada, presenciaConfirmada, totalCalculado, motivoDeCierre, lineas, base.version + 1);
        }
    }
}
