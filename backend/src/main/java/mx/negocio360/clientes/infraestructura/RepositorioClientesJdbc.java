package mx.negocio360.clientes.infraestructura;

import static mx.negocio360.clientes.infraestructura.MomentosJdbc.instante;
import static mx.negocio360.clientes.infraestructura.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.CriterioBusqueda;
import mx.negocio360.clientes.dominio.DatosCliente;
import mx.negocio360.clientes.dominio.EstadoCliente;
import mx.negocio360.clientes.eventos.OrigenCliente;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Clientes en {@code clientes.cliente}. Toda operacion fija el negocio en la transaccion ({@link EjecutorEnNegocio}) y
 * ademas lo pone en el {@code WHERE}: la seguridad por fila es la segunda barrera, no la unica. Lleva el esquema en
 * cada nombre porque el rol de la aplicacion no tiene {@code clientes} en su {@code search_path}.
 */
public final class RepositorioClientesJdbc implements RepositorioClientes {

    private static final String COLUMNAS = "id, negocio_id, nombre, apellidos, telefono, correo, fecha_nacimiento, notas, "
            + "origen, estado, aviso_version, aviso_entregado_en, anonimizado_en, version, creado_en, actualizado_en, "
            + "creado_por";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    public RepositorioClientesJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public Optional<Cliente> porId(UUID negocioId, UUID id) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS
                        + " FROM clientes.cliente WHERE negocio_id = :negocio AND id = :id")
                .param("negocio", negocioId).param("id", id)
                .query(RepositorioClientesJdbc::leer).optional());
    }

    @Override
    public Optional<Cliente> activoPorTelefono(UUID negocioId, String telefonoE164) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM clientes.cliente "
                        + "WHERE negocio_id = :negocio AND estado = 'ACTIVO' AND telefono = :telefono")
                .param("negocio", negocioId).param("telefono", telefonoE164)
                .query(RepositorioClientesJdbc::leer).optional());
    }

    @Override
    public void insertar(Cliente cliente) {
        negocio.en(cliente.negocioId(), () -> {
            // El unico parcial de telefono (I-03) se resuelve sin error: un error de Postgres abortaria la transaccion y
            // ya no se podria consultar a quien tiene el numero.
            var insertadas = jdbc.sql("""
                    INSERT INTO clientes.cliente (%s)
                    VALUES (:id, :negocio, :nombre, :apellidos, :telefono, :correo, :nacimiento, :notas, :origen, :estado,
                            :avisoVersion, :avisoEntregadoEn, :anonimizadoEn, :version, :creadoEn, :actualizadoEn,
                            :creadoPor)
                    ON CONFLICT (negocio_id, telefono) WHERE telefono IS NOT NULL AND estado = 'ACTIVO' DO NOTHING
                    """.formatted(COLUMNAS))
                    .param("id", cliente.id())
                    .param("negocio", cliente.negocioId())
                    .param("nombre", cliente.datos().nombre())
                    .param("apellidos", cliente.datos().apellidos(), Types.VARCHAR)
                    .param("telefono", cliente.datos().telefono(), Types.VARCHAR)
                    .param("correo", cliente.datos().correo(), Types.VARCHAR)
                    .param("nacimiento", cliente.datos().fechaNacimiento(), Types.DATE)
                    .param("notas", cliente.datos().notas(), Types.VARCHAR)
                    .param("origen", cliente.origen().name())
                    .param("estado", cliente.estado().name())
                    .param("avisoVersion", cliente.avisoVersion())
                    .param("avisoEntregadoEn", momento(cliente.avisoEntregadoEn()))
                    .param("anonimizadoEn", momento(cliente.anonimizadoEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("version", cliente.version())
                    .param("creadoEn", momento(cliente.creadoEn()))
                    .param("actualizadoEn", momento(cliente.actualizadoEn()))
                    .param("creadoPor", cliente.creadoPor())
                    .update();
            if (insertadas == 0) {
                var existente = activoPorTelefono(cliente.negocioId(), cliente.datos().telefono());
                throw existente.map(c -> new ClientesException(CodigoError.TELEFONO_DUPLICADO, c.id()))
                        .orElseGet(() -> new ClientesException(CodigoError.TELEFONO_DUPLICADO));
            }
            return null;
        });
    }

    @Override
    public void actualizar(Cliente nuevo, long versionEsperada) {
        negocio.en(nuevo.negocioId(), () -> {
            verificarTelefonoLibre(nuevo);
            int filas;
            try {
                filas = jdbc.sql("""
                        UPDATE clientes.cliente SET nombre = :nombre, apellidos = :apellidos, telefono = :telefono,
                            correo = :correo, fecha_nacimiento = :nacimiento, notas = :notas, estado = :estado,
                            anonimizado_en = :anonimizadoEn, version = :version, actualizado_en = :actualizadoEn
                        WHERE negocio_id = :negocio AND id = :id AND version = :esperada
                        """)
                        .param("nombre", nuevo.datos().nombre())
                        .param("apellidos", nuevo.datos().apellidos(), Types.VARCHAR)
                        .param("telefono", nuevo.datos().telefono(), Types.VARCHAR)
                        .param("correo", nuevo.datos().correo(), Types.VARCHAR)
                        .param("nacimiento", nuevo.datos().fechaNacimiento(), Types.DATE)
                        .param("notas", nuevo.datos().notas(), Types.VARCHAR)
                        .param("estado", nuevo.estado().name())
                        .param("anonimizadoEn", momento(nuevo.anonimizadoEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                        .param("version", nuevo.version())
                        .param("actualizadoEn", momento(nuevo.actualizadoEn()))
                        .param("negocio", nuevo.negocioId())
                        .param("id", nuevo.id())
                        .param("esperada", versionEsperada)
                        .update();
            } catch (DuplicateKeyException carrera) {
                // Otro alta tomo el telefono entre la comprobacion y la escritura.
                throw new ClientesException(CodigoError.TELEFONO_DUPLICADO);
            }
            if (filas == 0) {
                throw new ClientesException(CodigoError.CONCURRENCIA);
            }
            return null;
        });
    }

    /** I-03: el telefono de un cliente activo no puede ser el de otro activo. Cita al que ya lo tiene. */
    private void verificarTelefonoLibre(Cliente candidato) {
        var telefono = candidato.datos().telefono();
        if (telefono == null || candidato.estado() != EstadoCliente.ACTIVO) {
            return;
        }
        activoPorTelefono(candidato.negocioId(), telefono).filter(c -> !c.id().equals(candidato.id()))
                .ifPresent(c -> {
                    throw new ClientesException(CodigoError.TELEFONO_DUPLICADO, c.id());
                });
    }

    @Override
    public List<Cliente> buscar(UUID negocioId, CriterioBusqueda criterio, UUID despuesDe, int limite) {
        if (criterio instanceof CriterioBusqueda.Ninguno) {
            return List.of();
        }
        var sql = new StringBuilder("SELECT " + COLUMNAS
                + " FROM clientes.cliente WHERE negocio_id = :negocio AND estado = 'ACTIVO'");
        switch (criterio) {
            // strpos y no LIKE: el texto de la persona nunca se interpreta como patron.
            case CriterioBusqueda.PorNombre p -> sql.append(" AND strpos(nombre_busqueda, :texto) > 0");
            case CriterioBusqueda.PorTelefono t -> sql.append(" AND telefono = :telefono");
            default -> { }
        }
        if (despuesDe != null) {
            sql.append(" AND id > :despues");
        }
        sql.append(" ORDER BY id LIMIT :limite");
        return negocio.en(negocioId, () -> {
            var consulta = jdbc.sql(sql.toString()).param("negocio", negocioId).param("limite", limite);
            if (criterio instanceof CriterioBusqueda.PorNombre p) {
                consulta = consulta.param("texto", p.texto());
            } else if (criterio instanceof CriterioBusqueda.PorTelefono t) {
                consulta = consulta.param("telefono", t.telefono());
            }
            if (despuesDe != null) {
                consulta = consulta.param("despues", despuesDe);
            }
            return consulta.query(RepositorioClientesJdbc::leer).list();
        });
    }

    private static Cliente leer(ResultSet fila, int numero) throws SQLException {
        var datos = new DatosCliente(fila.getString("nombre"), fila.getString("apellidos"), fila.getString("telefono"),
                fila.getString("correo"), fila.getObject("fecha_nacimiento", LocalDate.class), fila.getString("notas"));
        return new Cliente(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class), datos,
                OrigenCliente.valueOf(fila.getString("origen")), EstadoCliente.valueOf(fila.getString("estado")),
                fila.getInt("aviso_version"), instante(fila, "aviso_entregado_en"), instante(fila, "anonimizado_en"),
                fila.getLong("version"), instante(fila, "creado_en"), instante(fila, "actualizado_en"),
                fila.getString("creado_por"));
    }
}
