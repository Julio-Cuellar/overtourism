package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.aplicacion.AltasApi.ResultadoAlta;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.dominio.EstadoNegocio;
import mx.negocio360.negocios.infraestructura.EjecutorEnContexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** AL-01, AL-02, AL-04, AL-05, NG-01 a NG-03, NG-08 y SN-02 contra PostgreSQL de verdad, con el rol de la aplicacion. */
@SpringBootTest
@RecordApplicationEvents
class AltaDeNegocioTest {

    @Autowired
    private AltasApi altas;
    @Autowired
    private RepositorioNegocios negocios;
    @Autowired
    private EjecutorEnContexto contexto;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private ApplicationEvents eventos;

    private long publicados(String tipo, UUID negocio) {
        return eventos.stream(SobreEvento.class).filter(e -> e.tipo().equals(tipo) && e.negocioId().equals(negocio))
                .count();
    }

    @Test
    @DisplayName("AL-01: un pedido valido deja un negocio ACTIVO, sin publicar, y publica negocio.registrado")
    void pedidoValido_creaElNegocio_yPublicaRegistrado() {
        var pedido = Datos.pedidoValido();

        var resultado = altas.registrarAlta(pedido);

        assertThat(resultado).isEqualTo(ResultadoAlta.REGISTRADO);
        var negocio = negocios.negocio(pedido.negocioId()).orElseThrow();
        assertThat(negocio.estado()).isEqualTo(EstadoNegocio.ACTIVO);
        assertThat(negocio.publicado()).isFalse();
        assertThat(negocio.nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(negocio.giro()).isEqualTo("RESTAURANTE");
        assertThat(negocio.solicitadoPor()).isEqualTo(pedido.cuentaId());
        assertThat(negocio.contactoPublico()).isFalse();
        assertThat(negocio.esSimulado()).isFalse();
        assertThat(publicados("negocio.registrado", pedido.negocioId())).isEqualTo(1);
    }

    @Test
    @DisplayName("el evento negocio.registrado cumple el contrato que lee Identidad")
    void registrado_cumpleElContrato() {
        var pedido = Datos.pedidoValido();

        altas.registrarAlta(pedido);

        var sobre = eventos.stream(SobreEvento.class).filter(e -> e.tipo().equals("negocio.registrado")
                && e.negocioId().equals(pedido.negocioId())).findFirst().orElseThrow();
        assertThat(sobre.origen()).isEqualTo("negocios");
        assertThat(sobre.version()).isEqualTo(1);
        assertThat(sobre.correlacionId()).isEqualTo(pedido.correlacionId());
        assertThat(sobre.causaId()).isEqualTo(pedido.eventoId());
        assertThat(sobre.datos()).containsEntry("solicitado_por", pedido.cuentaId().toString())
                .containsEntry("nombre", "Fonda Doña Chela").containsEntry("giro", "RESTAURANTE");
    }

    @Test
    @DisplayName("NG-08: todo negocio nace con una sucursal principal llamada Principal, sin direccion")
    void alta_creaLaSucursalPrincipal() {
        var pedido = Datos.pedidoValido();

        altas.registrarAlta(pedido);

        var principal = negocios.principal(pedido.negocioId()).orElseThrow();
        assertThat(principal.nombre()).isEqualTo("Principal");
        assertThat(principal.esPrincipal()).isTrue();
        assertThat(principal.activa()).isTrue();
        assertThat(principal.calle()).isNull();
        assertThat(principal.tieneUbicacion()).isFalse();
    }

    @Test
    @DisplayName("NG-03: procesar dos veces el mismo evento deja un negocio y publica una sola vez")
    void mismoEvento_dosVeces_esIdempotente() {
        var pedido = Datos.pedidoValido();

        var primero = altas.registrarAlta(pedido);
        var segundo = altas.registrarAlta(pedido);

        assertThat(primero).isEqualTo(ResultadoAlta.REGISTRADO);
        assertThat(segundo).isEqualTo(ResultadoAlta.YA_PROCESADO);
        assertThat(publicados("negocio.registrado", pedido.negocioId())).isEqualTo(1);
        assertThat(altas.misAltas(pedido.cuentaId())).hasSize(1);
    }

    @Test
    @DisplayName("NG-03: otro evento con el mismo negocio no lo crea de nuevo")
    void mismoNegocio_conOtroEvento_noSeDuplica() {
        var pedido = Datos.pedidoValido();
        var repetido = Datos.pedido(pedido.negocioId(), pedido.cuentaId(), "Otro nombre", "RESTAURANTE");

        altas.registrarAlta(pedido);
        var resultado = altas.registrarAlta(repetido);

        assertThat(resultado).isEqualTo(ResultadoAlta.YA_PROCESADO);
        assertThat(negocios.negocio(pedido.negocioId()).orElseThrow().nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(publicados("negocio.registrado", pedido.negocioId())).isEqualTo(1);
    }

    @Test
    @DisplayName("AL-02 y NG-02: un giro que no existe se rechaza, sin negocio, y publica el rechazo")
    void giroInexistente_seRechaza() {
        var pedido = Datos.pedido(Datos.id(), Datos.id(), "Farmacia del Centro", "FARMACIA");

        var resultado = altas.registrarAlta(pedido);

        assertThat(resultado).isEqualTo(ResultadoAlta.RECHAZADO);
        assertThat(negocios.negocio(pedido.negocioId())).isEmpty();
        var alta = altas.misAltas(pedido.cuentaId()).get(0);
        assertThat(alta.estado()).isEqualTo(EstadoAlta.RECHAZADO);
        assertThat(alta.motivo()).isEqualTo("GIRO_INVALIDO");
        assertThat(publicados("negocio.registro_rechazado", pedido.negocioId())).isEqualTo(1);
        assertThat(publicados("negocio.registrado", pedido.negocioId())).isZero();
    }

    @Test
    @DisplayName("AL-02 y NG-01: un nombre invalido se rechaza con NOMBRE_INVALIDO y no lleva caracteres de control")
    void nombreInvalido_seRechaza() {
        var pedido = Datos.pedido(Datos.id(), Datos.id(), "A\u0000", "RESTAURANTE");

        var resultado = altas.registrarAlta(pedido);

        assertThat(resultado).isEqualTo(ResultadoAlta.RECHAZADO);
        assertThat(altas.misAltas(pedido.cuentaId()).get(0).motivo()).isEqualTo("NOMBRE_INVALIDO");
        var rechazo = eventos.stream(SobreEvento.class).filter(e -> e.tipo().equals("negocio.registro_rechazado")
                && e.negocioId().equals(pedido.negocioId())).findFirst().orElseThrow();
        assertThat(rechazo.datos().get("nombre").toString()).doesNotContain("\u0000");
        assertThat(rechazo.datos()).containsEntry("motivo", "NOMBRE_INVALIDO");
    }

    @Test
    @DisplayName("NG-01: los espacios sobrantes del nombre se colapsan y el giro no distingue mayusculas")
    void nombre_seNormaliza() {
        var pedido = Datos.pedido(Datos.id(), Datos.id(), "  Fonda   Doña \t Chela ".replace('\t', ' '), "restaurante");

        altas.registrarAlta(pedido);

        var negocio = negocios.negocio(pedido.negocioId()).orElseThrow();
        assertThat(negocio.nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(negocio.giro()).isEqualTo("RESTAURANTE");
    }

    @Test
    @DisplayName("AL-04: la persona solo ve sus propias altas; la ajena responde como si no existiera")
    void altas_seAislanPorCuenta() {
        var a = Datos.pedidoValido();
        var b = Datos.pedidoValido();
        altas.registrarAlta(a);
        altas.registrarAlta(b);

        assertThat(altas.misAltas(a.cuentaId())).extracting("negocioId").containsExactly(a.negocioId());
        assertThat(altas.misAltas(b.cuentaId())).extracting("negocioId").containsExactly(b.negocioId());
        assertThat(altas.misAltas(Datos.id())).isEmpty();
    }

    @Test
    @DisplayName("SN-02: con el contexto de otro negocio la base no muestra el negocio ni su sucursal")
    void seguridadPorFila_elOtroNegocioNoVeNada() {
        var pedido = Datos.pedidoValido();
        altas.registrarAlta(pedido);
        var otro = Datos.id();

        var negocios = contexto.enNegocio(otro, () -> jdbc.sql("SELECT count(*) FROM negocios.negocio WHERE id = :id")
                .param("id", pedido.negocioId()).query(Integer.class).single());
        var sucursales = contexto.enNegocio(otro, () -> jdbc
                .sql("SELECT count(*) FROM negocios.sucursal WHERE negocio_id = :id")
                .param("id", pedido.negocioId()).query(Integer.class).single());

        assertThat(negocios).isZero();
        assertThat(sucursales).isZero();
    }

    @Test
    @DisplayName("SN-02: sin ningun contexto la base falla cerrado y no muestra negocios")
    void seguridadPorFila_sinContexto_noVeNada() {
        altas.registrarAlta(Datos.pedidoValido());

        var visibles = jdbc.sql("SELECT count(*) FROM negocios.negocio").query(Integer.class).single();

        assertThat(visibles).isZero();
    }

    @Test
    @DisplayName("SN-02: el contexto de un negocio no deja escribir filas de otro")
    void seguridadPorFila_noDejaEscribirEnOtroNegocio() {
        var otro = Datos.id();
        var ajeno = Datos.id();

        var intento = org.assertj.core.api.Assertions.catchThrowable(() -> contexto.enNegocio(otro, () -> jdbc.sql(
                "INSERT INTO negocios.negocio (id, nombre, giro, solicitado_por, creado_en, actualizado_en) "
                        + "VALUES (:id, 'Colado', 'RESTAURANTE', :cuenta, now(), now())")
                .param("id", ajeno).param("cuenta", Datos.id()).update()));

        assertThat(intento).rootCause().hasMessageContaining("row-level security");
    }

    @Test
    @DisplayName("la transaccion no puede cambiar de negocio a mitad de camino")
    void contexto_noCambiaDeNegocio() {
        var uno = Datos.id();
        var dos = Datos.id();

        var intento = org.assertj.core.api.Assertions.catchThrowable(() -> contexto.enNegocio(uno,
                () -> contexto.enNegocio(dos, () -> "no debe llegar")));

        assertThat(intento).isInstanceOf(IllegalStateException.class);
    }
}
