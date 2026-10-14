package mx.negocio360.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.clientes.eventos.ClienteAnonimizado;
import mx.negocio360.clientes.eventos.ClienteConsentimientoCambiado;
import mx.negocio360.clientes.eventos.ClienteRegistrado;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * P-06: ningun evento de Clientes lleva datos personales. La regla se prueba por tipo: cada componente de un evento
 * debe ser un UUID o un enum del propio paquete de eventos, asi que un campo de texto nuevo hace fallar la prueba.
 */
class EventosClientesContratoTest {

    private static final String PAQUETE = "mx.negocio360.clientes.eventos";
    private static final UUID CLIENTE = UUID.fromString("0192f3a0-1c00-7000-8000-00000000d004");
    private static final Set<String> CLAVES_PERMITIDAS =
            Set.of("cliente_id", "origen", "finalidad", "estado", "medio");

    /** Descubre por escaneo todos los records del paquete: un evento nuevo entra solo a esta revision. */
    static List<Class<?>> eventosDeClientes() {
        try {
            var recursos = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + PAQUETE.replace('.', '/') + "/*.class");
            var clases = new java.util.ArrayList<Class<?>>();
            for (var recurso : recursos) {
                var nombre = recurso.getFilename().replace(".class", "");
                if (!nombre.equals("package-info")) {
                    var clase = Class.forName(PAQUETE + "." + nombre);
                    if (clase.isRecord()) {
                        clases.add(clase);
                    }
                }
            }
            return clases;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("No se pudieron descubrir los eventos de Clientes", e);
        }
    }

    private static Object valorDeEjemplo(Class<?> tipo, String evento) {
        if (tipo == UUID.class) {
            return CLIENTE;
        }
        if (tipo.isEnum() && tipo.getPackageName().equals(PAQUETE)) {
            return tipo.getEnumConstants()[0];
        }
        throw new AssertionError("El evento " + evento + " tiene un componente de tipo " + tipo.getName()
                + ". Un evento solo puede llevar UUID o enums de " + PAQUETE + " (P-06).");
    }

    private static Map<String, Object> datosDe(Class<?> evento) throws Exception {
        var componentes = evento.getRecordComponents();
        var valores = new Object[componentes.length];
        for (var i = 0; i < componentes.length; i++) {
            valores[i] = valorDeEjemplo(componentes[i].getType(), evento.getSimpleName());
        }
        var constructor = evento.getDeclaredConstructor(
                java.util.Arrays.stream(componentes).map(c -> c.getType()).toArray(Class<?>[]::new));
        var instancia = constructor.newInstance(valores);
        @SuppressWarnings("unchecked")
        var datos = (Map<String, Object>) evento.getMethod("datos").invoke(instancia);
        return datos;
    }

    @Test
    @DisplayName("el escaneo encuentra exactamente los tres eventos acordados")
    void eventosDescubiertos_sonLosTresAcordados() {
        assertThat(eventosDeClientes()).extracting(Class::getSimpleName).containsExactlyInAnyOrder(
                "ClienteRegistrado", "ClienteConsentimientoCambiado", "ClienteAnonimizado");
    }

    @ParameterizedTest
    @MethodSource("eventosDeClientes")
    @DisplayName("cada evento solo lleva ids y catalogos, y sus claves estan permitidas")
    void evento_datos_soloLlevanIdsYCatalogos(Class<?> evento) throws Exception {
        var datos = datosDe(evento);

        assertThat(datos.keySet()).isSubsetOf(CLAVES_PERMITIDAS).contains("cliente_id");
        assertThat(datos.values()).allMatch(valor -> valor instanceof String);
    }

    @ParameterizedTest
    @MethodSource("eventosDeClientes")
    @DisplayName("cada evento declara su tipo y su version")
    void evento_tipoYVersion_siguenElContrato(Class<?> evento) throws Exception {
        var tipo = (String) evento.getField("TIPO").get(null);
        var version = (int) evento.getField("VERSION").get(null);

        assertThat(tipo).matches("^cliente\\.[a-z_]+$");
        assertThat(version).isEqualTo(1);
    }

    @Test
    @DisplayName("cliente.registrado lleva el id y el origen")
    void registrado_datos_llevanIdYOrigen() {
        assertThat(new ClienteRegistrado(CLIENTE, OrigenCliente.PANEL).datos())
                .containsExactlyInAnyOrderEntriesOf(Map.of("cliente_id", CLIENTE.toString(), "origen", "PANEL"));
    }

    @Test
    @DisplayName("cliente.consentimiento_cambiado lleva finalidad, estado y medio")
    void consentimientoCambiado_datos_llevanFinalidadEstadoYMedio() {
        var evento = new ClienteConsentimientoCambiado(CLIENTE, Finalidad.PROMOCIONES,
                EstadoConsentimiento.OTORGADO, MedioConsentimiento.PANEL);

        assertThat(evento.datos()).containsExactlyInAnyOrderEntriesOf(Map.of("cliente_id", CLIENTE.toString(),
                "finalidad", "PROMOCIONES", "estado", "OTORGADO", "medio", "PANEL"));
    }

    @Test
    @DisplayName("cliente.anonimizado solo lleva el id")
    void anonimizado_datos_soloLlevanElId() {
        assertThat(new ClienteAnonimizado(CLIENTE).datos())
                .containsExactlyInAnyOrderEntriesOf(Map.of("cliente_id", CLIENTE.toString()));
    }

    @Test
    @DisplayName("los tipos del contrato son los acordados")
    void tipos_delContrato_sonLosAcordados() {
        assertThat(ClienteRegistrado.TIPO).isEqualTo("cliente.registrado");
        assertThat(ClienteConsentimientoCambiado.TIPO).isEqualTo("cliente.consentimiento_cambiado");
        assertThat(ClienteAnonimizado.TIPO).isEqualTo("cliente.anonimizado");
    }

    @Test
    @DisplayName("los datos y el tipo caben en un sobre comun valido")
    void sobre_conDatosDeUnEventoDeClientes_esValido() {
        var evento = new ClienteConsentimientoCambiado(CLIENTE, Finalidad.CONTACTO_MENSAJERIA,
                EstadoConsentimiento.REVOCADO, MedioConsentimiento.VERBAL);

        var sobre = new SobreEvento(UUID.fromString("0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10"),
                ClienteConsentimientoCambiado.TIPO, ClienteConsentimientoCambiado.VERSION,
                UUID.fromString("0192f39f-1111-7000-8000-00000000a001"), "clientes",
                Instant.parse("2026-10-13T16:00:00Z"), CLIENTE, null, evento.datos());

        assertThat(sobre.datos()).containsEntry("estado", "REVOCADO");
    }
}
