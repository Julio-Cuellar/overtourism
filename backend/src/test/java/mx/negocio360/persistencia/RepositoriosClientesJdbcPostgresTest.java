package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.Preparacion;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioConsentimientos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioPreparaciones;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.Consentimiento;
import mx.negocio360.clientes.dominio.CriterioBusqueda;
import mx.negocio360.clientes.dominio.DatosCliente;
import mx.negocio360.clientes.dominio.EstadoCliente;
import mx.negocio360.clientes.dominio.FinalidadAviso;
import mx.negocio360.clientes.dominio.FormaConsentimiento;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.clientes.infraestructura.EjecutorEnNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Los repositorios JDBC de Clientes contra PostgreSQL de verdad, con el rol de la aplicacion (sin superusuario y sujeto
 * a la seguridad por fila). Cada prueba usa su propio negocio.
 */
@SpringBootTest
class RepositoriosClientesJdbcPostgresTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 15);

    @Autowired
    private RepositorioClientes clientes;
    @Autowired
    private RepositorioAvisos avisos;
    @Autowired
    private RepositorioConsentimientos consentimientos;
    @Autowired
    private RepositorioPreparaciones preparaciones;
    @Autowired
    private EjecutorEnNegocio ejecutor;
    @Autowired
    private TransactionTemplate transaccion;

    private static AvisoPrivacidad aviso(UUID negocio, int version, Instant desde) {
        return AvisoPrivacidad.nuevo(UuidV7.generar(desde), negocio, version, "Taller Mixcoatl",
                "Calle 5 de Mayo 10, Puebla", List.of(new FinalidadAviso("VENTA", false),
                        new FinalidadAviso("PROMOCIONES", true)),
                "Usamos tu nombre y telefono para atenderte.", "Aviso integral de ejemplo.", "privacidad@taller.test",
                "prueba", desde);
    }

    /** Un negocio con su primer aviso publicado, que los clientes necesitan (llave foranea). */
    private UUID negocioConAviso() {
        var negocio = UUID.randomUUID();
        avisos.publicar(aviso(negocio, 1, AHORA));
        return negocio;
    }

    private static Cliente cliente(UUID negocio, String nombre, String apellidos, String telefono) {
        var datos = DatosCliente.de(nombre, apellidos, telefono, "ana@ejemplo.test", LocalDate.of(1990, 5, 17),
                "Prefiere tortillas azules", HOY);
        return Cliente.nuevo(UuidV7.generar(AHORA), negocio, datos, OrigenCliente.PANEL, 1, "locatario-a", AHORA);
    }

    private static String telefonoNuevo() {
        return FilasClientes.telefono();
    }

    // ---- clientes ----

    @Test
    @DisplayName("un cliente insertado se lee igual: datos con acentos, fecha de nacimiento y version")
    void cliente_insertadoSeLeeIgual() {
        var negocio = negocioConAviso();
        var original = cliente(negocio, "José Ángel", "Pérez Núñez", telefonoNuevo());

        clientes.insertar(original);

        var leido = clientes.porId(negocio, original.id()).orElseThrow();
        assertThat(leido.datos()).isEqualTo(original.datos());
        assertThat(leido).extracting(Cliente::id, Cliente::estado, Cliente::version, Cliente::origen, Cliente::avisoVersion,
                Cliente::creadoPor).containsExactly(original.id(), EstadoCliente.ACTIVO, 1L, OrigenCliente.PANEL, 1,
                "locatario-a");
        assertThat(leido.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("un negocio no ve los clientes de otro")
    void cliente_otroNegocioNoLoVe() {
        var a = negocioConAviso();
        var b = negocioConAviso();
        var deA = cliente(a, "Ana", null, telefonoNuevo());
        clientes.insertar(deA);

        assertThat(clientes.porId(b, deA.id())).isEmpty();
        assertThat(clientes.buscar(b, new CriterioBusqueda.Todos(), null, 10)).isEmpty();
        assertThat(clientes.activoPorTelefono(b, deA.datos().telefono())).isEmpty();
    }

    @Test
    @DisplayName("insertar con el telefono de un cliente activo falla con TELEFONO_DUPLICADO y cita al existente")
    void cliente_telefonoDuplicado_fallaCitandoAlExistente() {
        var negocio = negocioConAviso();
        var telefono = telefonoNuevo();
        var primero = cliente(negocio, "Ana", null, telefono);
        clientes.insertar(primero);

        assertThatThrownBy(() -> clientes.insertar(cliente(negocio, "Beto", null, telefono)))
                .isInstanceOfSatisfying(ClientesException.class, e -> {
                    assertThat(e.codigo()).isEqualTo(CodigoError.TELEFONO_DUPLICADO);
                    assertThat(e.recursoId()).contains(primero.id());
                });
    }

    @Test
    @DisplayName("el mismo telefono en dos negocios distintos es valido")
    void cliente_mismoTelefonoEnOtroNegocio_esValido() {
        var telefono = telefonoNuevo();
        var a = negocioConAviso();
        var b = negocioConAviso();

        clientes.insertar(cliente(a, "Ana", null, telefono));
        clientes.insertar(cliente(b, "Ana", null, telefono));

        assertThat(clientes.activoPorTelefono(a, telefono)).isPresent();
        assertThat(clientes.activoPorTelefono(b, telefono)).isPresent();
    }

    @Test
    @DisplayName("actualizar con la version esperada guarda y sube la version; con una vieja falla con CONCURRENCIA")
    void cliente_actualizar_bloqueoOptimista() {
        var negocio = negocioConAviso();
        var original = cliente(negocio, "Ana", null, telefonoNuevo());
        clientes.insertar(original);
        var editado = original.conDatos(DatosCliente.de("Ana Maria", null, original.datos().telefono(), null, null,
                null, HOY), AHORA.plusSeconds(60));

        clientes.actualizar(editado, 1L);

        var leido = clientes.porId(negocio, original.id()).orElseThrow();
        assertThat(leido.version()).isEqualTo(2L);
        assertThat(leido.datos().nombre()).isEqualTo("Ana Maria");
        assertThat(leido.datos().correo()).isNull();
        assertThatThrownBy(() -> clientes.actualizar(editado.conDatos(editado.datos(), AHORA.plusSeconds(120)), 1L))
                .isInstanceOfSatisfying(ClientesException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.CONCURRENCIA));
    }

    @Test
    @DisplayName("actualizar a un telefono que ya tiene otro cliente activo falla con TELEFONO_DUPLICADO")
    void cliente_actualizarATelefonoOcupado_falla() {
        var negocio = negocioConAviso();
        var ocupado = telefonoNuevo();
        clientes.insertar(cliente(negocio, "Ana", null, ocupado));
        var beto = cliente(negocio, "Beto", null, telefonoNuevo());
        clientes.insertar(beto);
        var conTelefonoAjeno = beto.conDatos(DatosCliente.de("Beto", null, ocupado, null, null, null, HOY), AHORA);

        assertThatThrownBy(() -> clientes.actualizar(conTelefonoAjeno, 1L))
                .isInstanceOfSatisfying(ClientesException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.TELEFONO_DUPLICADO));
    }

    @Test
    @DisplayName("buscar por nombre ignora acentos y mayusculas, por telefono exige el numero completo, y solo trae activos")
    void cliente_buscar_porNombreYTelefono() {
        var negocio = negocioConAviso();
        var jose = cliente(negocio, "José", "Hernández", telefonoNuevo());
        var maria = cliente(negocio, "María", "López", telefonoNuevo());
        clientes.insertar(jose);
        clientes.insertar(maria);

        assertThat(clientes.buscar(negocio, CriterioBusqueda.de("JOSE hernandez"), null, 10))
                .extracting(Cliente::id).containsExactly(jose.id());
        assertThat(clientes.buscar(negocio, CriterioBusqueda.de("lopez"), null, 10))
                .extracting(Cliente::id).containsExactly(maria.id());
        assertThat(clientes.buscar(negocio, CriterioBusqueda.de(maria.datos().telefono()), null, 10))
                .extracting(Cliente::id).containsExactly(maria.id());
        assertThat(clientes.buscar(negocio, new CriterioBusqueda.Todos(), null, 10)).hasSize(2);
    }

    @Test
    @DisplayName("buscar pagina por id: el cursor despuesDe y el limite devuelven la pagina siguiente en orden")
    void cliente_buscar_paginaPorId() {
        var negocio = negocioConAviso();
        for (var i = 0; i < 5; i++) {
            clientes.insertar(cliente(negocio, "Cliente " + i, null, telefonoNuevo()));
        }

        var primera = clientes.buscar(negocio, new CriterioBusqueda.Todos(), null, 2);
        var segunda = clientes.buscar(negocio, new CriterioBusqueda.Todos(), primera.get(1).id(), 2);

        assertThat(primera).hasSize(2);
        assertThat(segunda).hasSize(2);
        assertThat(segunda.get(0).id().toString()).isGreaterThan(primera.get(1).id().toString());
        assertThat(primera.get(0).id().toString()).isLessThan(primera.get(1).id().toString());
    }

    // ---- avisos ----

    @Test
    @DisplayName("publicar un aviso nuevo cierra el vigente con la fecha de inicio del nuevo")
    void aviso_publicar_cierraElAnterior() {
        var negocio = UUID.randomUUID();
        assertThat(avisos.vigente(negocio)).isEmpty();
        assertThat(avisos.ultimaVersion(negocio)).isZero();

        avisos.publicar(aviso(negocio, 1, AHORA));
        avisos.publicar(aviso(negocio, 2, AHORA.plusSeconds(3600)));

        var vigente = avisos.vigente(negocio).orElseThrow();
        assertThat(vigente.version()).isEqualTo(2);
        assertThat(vigente.finalidades()).containsExactly(new FinalidadAviso("VENTA", false),
                new FinalidadAviso("PROMOCIONES", true));
        assertThat(avisos.ultimaVersion(negocio)).isEqualTo(2);
    }

    @Test
    @DisplayName("publicar una version que ya existe falla con CONCURRENCIA y no deja dos avisos vigentes")
    void aviso_versionRepetida_fallaConConcurrencia() {
        var negocio = UUID.randomUUID();
        avisos.publicar(aviso(negocio, 1, AHORA));

        assertThatThrownBy(() -> avisos.publicar(aviso(negocio, 1, AHORA.plusSeconds(10))))
                .isInstanceOfSatisfying(ClientesException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.CONCURRENCIA));
        assertThat(avisos.vigente(negocio).orElseThrow().version()).isEqualTo(1);
    }

    @Test
    @DisplayName("un aviso es de un negocio: otro negocio no lo ve como vigente")
    void aviso_otroNegocioNoLoVe() {
        var a = UUID.randomUUID();
        avisos.publicar(aviso(a, 1, AHORA));

        assertThat(avisos.vigente(UUID.randomUUID())).isEmpty();
    }

    // ---- consentimientos ----

    @Test
    @DisplayName("el historial de consentimientos se lee de la fila mas antigua a la mas reciente")
    void consentimiento_historialEnOrden() {
        var negocio = negocioConAviso();
        var ana = cliente(negocio, "Ana", null, telefonoNuevo());
        clientes.insertar(ana);
        var primero = new Consentimiento(UuidV7.generar(AHORA), negocio, ana.id(), Finalidad.PROMOCIONES,
                EstadoConsentimiento.OTORGADO, FormaConsentimiento.EXPRESO, MedioConsentimiento.VERBAL, 1, "locatario-a",
                AHORA);
        var revocacion = new Consentimiento(UuidV7.generar(AHORA.plusSeconds(60)), negocio, ana.id(),
                Finalidad.PROMOCIONES, EstadoConsentimiento.REVOCADO, FormaConsentimiento.EXPRESO,
                MedioConsentimiento.PANEL, 1, "locatario-a", AHORA.plusSeconds(60));

        consentimientos.insertar(revocacion);
        consentimientos.insertar(primero);

        assertThat(consentimientos.porCliente(negocio, ana.id())).containsExactly(primero, revocacion);
        assertThat(consentimientos.porCliente(UUID.randomUUID(), ana.id())).isEmpty();
    }

    // ---- preparaciones ----

    private static String hashDeToken() {
        return FilasClientes.hashDeToken();
    }

    private static Preparacion preparacion(UUID negocio, String hash) {
        return new Preparacion(hash, negocio, "locatario-a", "registrar_cliente", "hash-del-comando",
                "{\"nombre\":\"Ana\",\"apellidos\":null}", "Registrar al cliente Ana.", AHORA.plusSeconds(300), null);
    }

    @Test
    @DisplayName("una preparacion guardada se lee con el comando y el resumen tal cual")
    void preparacion_guardarYLeer() {
        var negocio = UUID.randomUUID();
        var hash = hashDeToken();

        preparaciones.guardar(preparacion(negocio, hash));

        var leida = preparaciones.porHash(negocio, hash).orElseThrow();
        assertThat(leida.actor()).isEqualTo("locatario-a");
        assertThat(leida.comandoTipo()).isEqualTo("registrar_cliente");
        assertThat(leida.resumen()).isEqualTo("Registrar al cliente Ana.");
        assertThat(leida.comando()).contains("\"nombre\"").contains("Ana");
        assertThat(leida.usadoEn()).isNull();
        assertThat(leida.expiraEn()).isEqualTo(AHORA.plusSeconds(300));
    }

    @Test
    @DisplayName("la preparacion de un negocio no la ve otro negocio")
    void preparacion_otroNegocioNoLaVe() {
        var negocio = UUID.randomUUID();
        var hash = hashDeToken();
        preparaciones.guardar(preparacion(negocio, hash));

        assertThat(preparaciones.porHash(UUID.randomUUID(), hash)).isEmpty();
        assertThat(preparaciones.marcarUsada(UUID.randomUUID(), hash, AHORA)).isFalse();
    }

    @Test
    @DisplayName("marcarUsada es atomica: la primera gana, la segunda recibe falso, y la fila ya no conserva el comando")
    void preparacion_marcarUsada_esAtomica() {
        var negocio = UUID.randomUUID();
        var hash = hashDeToken();
        preparaciones.guardar(preparacion(negocio, hash));

        var primera = preparaciones.marcarUsada(negocio, hash, AHORA);
        var segunda = preparaciones.marcarUsada(negocio, hash, AHORA.plusSeconds(1));

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        var leida = preparaciones.porHash(negocio, hash).orElseThrow();
        assertThat(leida.usadoEn()).isEqualTo(AHORA);
        assertThat(leida.comando()).isNull();
        assertThat(leida.resumen()).isNull();
    }

    @Test
    @DisplayName("vaciar quita el comando y el resumen, y deja la fila inutilizable (la base exige comando o uso)")
    void preparacion_vaciar_quemaElToken() {
        var negocio = UUID.randomUUID();
        var hash = hashDeToken();
        preparaciones.guardar(preparacion(negocio, hash));

        preparaciones.vaciar(negocio, hash);

        var leida = preparaciones.porHash(negocio, hash).orElseThrow();
        assertThat(leida.comando()).isNull();
        assertThat(leida.resumen()).isNull();
        assertThat(leida.usadoEn()).isNotNull();
        assertThat(preparaciones.marcarUsada(negocio, hash, AHORA)).isFalse();
    }

    @Test
    @DisplayName("marcarUsada se confirma aunque la transaccion de fuera se deshaga: es su propia transaccion")
    void preparacion_marcarUsada_sobreviveAlRollbackDeFuera() {
        var negocio = UUID.randomUUID();
        var hash = hashDeToken();
        preparaciones.guardar(preparacion(negocio, hash));

        transaccion.executeWithoutResult(estado -> {
            assertThat(preparaciones.marcarUsada(negocio, hash, AHORA)).isTrue();
            estado.setRollbackOnly();
        });

        assertThat(preparaciones.porHash(negocio, hash).orElseThrow().usadoEn()).isEqualTo(AHORA);
    }

    // ---- aislamiento por negocio ----

    @Test
    @DisplayName("una transaccion no puede cambiar de negocio a la mitad: el guardian falla cerrado")
    void ejecutor_noCambiaDeNegocioEnLaMismaTransaccion() {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();

        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            ejecutor.en(a, () -> 1);
            ejecutor.en(b, () -> 2);
        })).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("la misma transaccion si puede repetir su propio negocio")
    void ejecutor_repetirElMismoNegocio_esValido() {
        var a = UUID.randomUUID();

        var suma = transaccion.execute(estado -> ejecutor.en(a, () -> 1) + ejecutor.en(a, () -> 2));

        assertThat(suma).isEqualTo(3);
    }
}
