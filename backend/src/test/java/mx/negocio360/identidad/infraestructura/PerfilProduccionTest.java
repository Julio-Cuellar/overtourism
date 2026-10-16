package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.aplicacion.PoliticaDeAcceso;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.web.DevCanalController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;

/** IDN-1.6 · AS-10, SI-13: en produccion no existe ningun atajo de desarrollo, y la politica se lee de la configuracion. */
class PerfilProduccionTest {

    private static final String HABILITAR_CANAL_SIMULADO = "identidad.canal-simulado.habilitado=true";

    /** Los roles base citan permisos de Clientes (AZ-03): este contexto aislado no tiene ese modulo, asi que los declara. */
    private static final CatalogoDePermisos CLIENTES_DE_PRUEBA = () -> java.util.stream.Stream.of("clientes.sugerir",
            "clientes.lista.ver", "clientes.panorama.ver", "clientes.ficha.ver", "clientes.cliente.registrar",
            "clientes.cliente.editar", "clientes.consentimiento.registrar", "clientes.aviso.publicar",
            "clientes.cliente.anonimizar", "clientes.arco.gestionar", "clientes.datos.exportar")
            .map(codigo -> new PermisoDeclarado(codigo, "Permiso de prueba", false)).toList();

    /** El repositorio de pre-registros vive en Postgres (D-86): este contexto aislado no tiene base, asi que le da un cliente vacio. */
    private static final JdbcClient SIN_BASE = mock(JdbcClient.class);

    private final ApplicationContextRunner conTodo = new ApplicationContextRunner()
            .withUserConfiguration(IdentidadConfiguracion.class, IdentidadDesarrolloConfiguracion.class)
            .withBean("clientesDePrueba", CatalogoDePermisos.class, () -> CLIENTES_DE_PRUEBA)
            .withBean(JdbcClient.class, () -> SIN_BASE)
            .withPropertyValues(HABILITAR_CANAL_SIMULADO);

    private final ApplicationContextRunner soloDesarrollo = new ApplicationContextRunner()
            .withUserConfiguration(IdentidadDesarrolloConfiguracion.class);

    @Test
    @DisplayName("con el canal simulado habilitado se carga el canal, el controlador de codigos y la API de Identidad")
    void desarrollo_cargaElCanalSimuladoYLaApi() {
        conTodo.run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(CanalSimulado.class)
                .hasSingleBean(DevCanalController.class).hasSingleBean(IdentidadApi.class)
                .hasSingleBean(EscuchaDeNegocios.class));
    }

    @Test
    @DisplayName("SI-13: sin perfil y sin habilitarlo expresamente no existe el canal simulado, el controlador ni los repositorios en memoria")
    void sinPerfilNiPropiedad_noCargaPiezasDeDesarrollo() {
        soloDesarrollo.run(contexto -> assertThat(contexto).hasNotFailed().doesNotHaveBean(CanalSimulado.class)
                .doesNotHaveBean(DevCanalController.class).doesNotHaveBean(RepositorioCuentas.class));
    }

    @Test
    @DisplayName("SI-13: el controlador de codigos, por si solo, no existe sin la propiedad aunque haya un canal simulado")
    void controlador_sinPropiedad_noExiste() {
        var soloElControlador = new ApplicationContextRunner()
                .withBean(CanalSimulado.class, CanalSimulado::new)
                .withUserConfiguration(DevCanalController.class);

        soloElControlador.run(contexto -> assertThat(contexto).hasNotFailed().doesNotHaveBean(DevCanalController.class));
        soloElControlador.withPropertyValues(HABILITAR_CANAL_SIMULADO)
                .run(contexto -> assertThat(contexto).hasNotFailed().hasSingleBean(DevCanalController.class));
    }

    @Test
    @DisplayName("AS-10: sin habilitar el canal simulado y sin canal real la aplicacion no arranca, por falta del canal")
    void sinCanalReal_noArranca() {
        new ApplicationContextRunner()
                .withUserConfiguration(IdentidadConfiguracion.class, IdentidadDesarrolloConfiguracion.class)
                .withBean("clientesDePrueba", CatalogoDePermisos.class, () -> CLIENTES_DE_PRUEBA)
                .withBean(JdbcClient.class, () -> SIN_BASE)
                .run(contexto -> assertThat(contexto).hasFailed().getFailure()
                        .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    @DisplayName("AS-10: con el perfil produccion, aun con la propiedad puesta, la aplicacion no arranca sin canal real")
    void produccion_sinCanalReal_noArranca() {
        conTodo.withInitializer(c -> c.getEnvironment().setActiveProfiles("produccion"))
                .run(contexto -> assertThat(contexto).hasFailed().getFailure()
                        .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    @DisplayName("SI-13: con el perfil produccion no se carga ni el canal simulado, ni el controlador, ni los repositorios en memoria, aun con la propiedad puesta")
    void produccion_noCargaPiezasDeDesarrollo() {
        soloDesarrollo.withPropertyValues(HABILITAR_CANAL_SIMULADO)
                .withInitializer(c -> c.getEnvironment().setActiveProfiles("produccion"))
                .run(contexto -> assertThat(contexto).hasNotFailed().doesNotHaveBean(CanalSimulado.class)
                        .doesNotHaveBean(DevCanalController.class).doesNotHaveBean(RepositorioCuentas.class));
    }

    @Test
    @DisplayName("la politica de acceso se lee de la configuracion")
    void politica_seLeeDeLaConfiguracion() {
        conTodo.withPropertyValues("identidad.politica.intentos-maximos=3", "identidad.politica.codigo-vigencia=10m",
                        "identidad.politica.solicitudes-por-correo=4", "identidad.politica.preregistro-vigencia=20m")
                .run(contexto -> {
                    var politica = contexto.getBean(PoliticaDeAcceso.class);
                    assertThat(politica.intentosMaximos()).isEqualTo(3);
                    assertThat(politica.codigoVigencia()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(politica.solicitudesPorCorreo()).isEqualTo(4);
                    assertThat(politica.preregistroVigencia()).isEqualTo(Duration.ofMinutes(20));
                });
    }

    @Test
    @DisplayName("sin configuracion la politica es la del diseño")
    void politica_sinConfiguracion_esLaPorDefecto() {
        conTodo.run(contexto -> assertThat(contexto.getBean(PoliticaDeAcceso.class))
                .isEqualTo(PoliticaDeAcceso.porDefecto()));
    }

    @Test
    @DisplayName("una politica invalida en la configuracion impide el arranque")
    void politica_invalida_noArranca() {
        conTodo.withPropertyValues("identidad.politica.intentos-maximos=0")
                .run(contexto -> assertThat(contexto).hasFailed());
    }
}
