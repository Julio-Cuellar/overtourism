package mx.negocio360.clientes.fakes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.PublicarAvisoComando;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.DatosCliente;
import mx.negocio360.clientes.dominio.EstadoCliente;
import mx.negocio360.clientes.dominio.FinalidadAviso;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.compartido.identidad.UuidV7;

/** Datos sinteticos y rotulados (SC-11): nombres inventados, telefonos de ejemplo y el dominio .test. */
public final class Fixtures {

    public static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");
    public static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    private Fixtures() {
    }

    public static PublicarAvisoComando avisoValido() {
        return new PublicarAvisoComando("Taller Mixcoatl", "Calle 5 de Mayo 10, Puebla",
                List.of(new FinalidadAviso("VENTA", false), new FinalidadAviso("PROMOCIONES", true)),
                "Usamos tu nombre y telefono para atenderte. Consulta el aviso integral en mostrador.",
                "Aviso de privacidad integral del negocio de ejemplo.", "privacidad@taller.test");
    }

    public static RegistrarClienteComando registrar(String nombre) {
        return registrar(nombre, null, null);
    }

    public static RegistrarClienteComando registrar(String nombre, String apellidos, String telefono) {
        return new RegistrarClienteComando(nombre, apellidos, telefono, null, null, null, 1, List.of());
    }

    public static Cliente cliente(UUID negocioId, String nombre, String apellidos, String telefono) {
        var datos = DatosCliente.de(nombre, apellidos, telefono, null, null, null, HOY);
        return Cliente.nuevo(UuidV7.generar(AHORA), negocioId, datos, OrigenCliente.PANEL, 1, "fixture", AHORA);
    }

    public static Cliente clienteConCorreo(UUID negocioId, String nombre, String correo) {
        var datos = DatosCliente.de(nombre, null, null, correo, null, null, HOY);
        return Cliente.nuevo(UuidV7.generar(AHORA), negocioId, datos, OrigenCliente.PANEL, 1, "fixture", AHORA);
    }

    public static Cliente anonimizado(UUID negocioId) {
        var base = cliente(negocioId, "Cliente anonimizado", null, null);
        return new Cliente(base.id(), negocioId, base.datos(), OrigenCliente.PANEL, EstadoCliente.ANONIMIZADO, 1, AHORA,
                AHORA, 1L, AHORA, AHORA, "fixture");
    }
}
