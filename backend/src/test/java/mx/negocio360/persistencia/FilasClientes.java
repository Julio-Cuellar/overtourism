package mx.negocio360.persistencia;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Filas válidas de las tablas de Clientes para las pruebas de esquema. Todo es dato inventado y marcado con
 * {@link #ACTOR}; el correo usa el dominio reservado .invalid (SC-11).
 */
final class FilasClientes {

    static final String ACTOR = "actor-de-prueba";

    private FilasClientes() {
    }

    static String telefono() {
        return "+52222" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    /** 64 caracteres hexadecimales, el formato del hash de un token. */
    static String hashDeToken() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "").substring(0, 64);
    }

    static void insertarAviso(SesionSql sesion, UUID negocio, int version) throws Exception {
        sesion.ejecutar("insert into clientes.aviso_privacidad (id, negocio_id, version, responsable_nombre, "
                + "responsable_domicilio, finalidades, texto_simplificado, texto_integral, contacto_arco, vigente_desde, "
                + "publicado_por) values (?, ?, ?, 'Fonda de prueba', 'Calle de prueba 1', "
                + "'[{\"codigo\":\"CONTACTO\",\"requiere_consentimiento\":false}]'::jsonb, 'simplificado', 'integral', "
                + "'datos@ejemplo.invalid', now(), ?)", UUID.randomUUID(), negocio, version, ACTOR);
    }

    static UUID insertarCliente(SesionSql sesion, UUID negocio, String nombre, String tel, String estado) throws Exception {
        var id = UUID.randomUUID();
        var anonimizado = "ANONIMIZADO".equals(estado);
        // Un cliente anonimizado ya no conserva datos de contacto ni personales (B-01).
        sesion.ejecutar("insert into clientes.cliente (id, negocio_id, nombre, apellidos, telefono, origen, estado, "
                + "aviso_version, aviso_entregado_en, anonimizado_en, creado_por) "
                + "values (?, ?, ?, " + (anonimizado ? "null, null" : "'Pérez', ?") + ", 'PANEL', ?, 1, now(), "
                + (anonimizado ? "now()" : "null") + ", ?)",
                anonimizado ? new Object[] {id, negocio, nombre, estado, ACTOR}
                        : new Object[] {id, negocio, nombre, tel, estado, ACTOR});
        return id;
    }

    static void insertarConsentimiento(SesionSql sesion, UUID negocio, UUID cliente) throws Exception {
        sesion.ejecutar("insert into clientes.consentimiento (id, negocio_id, cliente_id, finalidad, estado, forma, medio, "
                + "aviso_version, registrado_por, registrado_en) values (?, ?, ?, 'PROMOCIONES', 'OTORGADO', 'EXPRESO', "
                + "'PANEL', 1, ?, now())", UUID.randomUUID(), negocio, cliente, ACTOR);
    }

    static void insertarSolicitudArco(SesionSql sesion, UUID negocio, UUID cliente) throws Exception {
        sesion.ejecutar("insert into clientes.solicitud_arco (id, negocio_id, cliente_id, derecho, recibida_en, "
                + "medio_recepcion, responder_antes_de, estado, creado_por) "
                + "values (?, ?, ?, 'ACCESO', current_date, 'PRESENCIAL', current_date + 20, 'RECIBIDA', ?)",
                UUID.randomUUID(), negocio, cliente, ACTOR);
    }

    static void insertarResumen(SesionSql sesion, UUID negocio, UUID cliente) throws Exception {
        sesion.ejecutar("insert into clientes.resumen_cliente (cliente_id, negocio_id) values (?, ?)", cliente, negocio);
    }

    static void insertarCompra(SesionSql sesion, UUID negocio, UUID cliente, String estado) throws Exception {
        sesion.ejecutar("insert into clientes.compra_aplicada (id, negocio_id, venta_id, cliente_id, monto, ocurrido_en, estado) "
                + "values (?, ?, ?, ?, 100, now(), ?)", UUID.randomUUID(), negocio, UUID.randomUUID(), cliente, estado);
    }

    static void insertarAcceso(SesionSql sesion, UUID negocio, UUID cliente) throws Exception {
        sesion.ejecutar("insert into clientes.acceso_cliente (id, negocio_id, cliente_id, actor, operacion, ocurrido_en) "
                + "values (?, ?, ?, ?, 'DETALLE', now())", UUID.randomUUID(), negocio, cliente, ACTOR);
    }

    static String insertarPreparacion(SesionSql sesion, UUID negocio) throws Exception {
        var token = hashDeToken();
        sesion.ejecutar("insert into clientes.preparacion (token_hash, negocio_id, actor, comando_tipo, comando_hash, "
                + "comando, resumen, expira_en) values (?, ?, ?, 'RegistrarCliente', 'hash', '{\"nombre\":\"Ana\"}'::jsonb, "
                + "'{}'::jsonb, now() + interval '5 minutes')", token, negocio, ACTOR);
        return token;
    }

    /** Una fila de cada tabla de Clientes con negocio_id, todas del mismo negocio. La sesión ya debe estar en ese negocio. */
    static UUID sembrarNegocio(SesionSql sesion, UUID negocio) throws Exception {
        insertarAviso(sesion, negocio, 1);
        var cliente = insertarCliente(sesion, negocio, "Cliente de prueba", telefono(), "ACTIVO");
        insertarConsentimiento(sesion, negocio, cliente);
        insertarSolicitudArco(sesion, negocio, cliente);
        insertarResumen(sesion, negocio, cliente);
        insertarCompra(sesion, negocio, cliente, "APLICADA");
        insertarAcceso(sesion, negocio, cliente);
        insertarPreparacion(sesion, negocio);
        return cliente;
    }
}
