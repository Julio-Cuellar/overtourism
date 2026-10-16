package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import mx.negocio360.identidad.SesionIniciada;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoMembresia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * IDN-2.5 y IDN-2.6 · AM-02, AM-05, AM-06, AM-07, AM-08, AP-02: administrar al equipo de un negocio. Estas operaciones
 * son de nivel de servicio: no tienen REST hasta que exista la confirmacion reforzada (IDN-3.2, D-73).
 */
class GestionDeEquipoTest {

    private static final String[] TELEFONOS = {"222 111 0001", "222 111 0002", "222 111 0003", "222 111 0004",
        "222 111 0005"};

    private final Escenario e = new Escenario();
    private UUID restaurante;
    private SesionIniciada ana;
    private SesionIniciada beto;
    private SesionIniciada carla;
    private SesionIniciada dani;
    private String tokenDeAna;

    @BeforeEach
    void preparar() {
        ana = e.registrar(TELEFONOS[0], "Ana");
        beto = e.registrar(TELEFONOS[1], "Beto");
        carla = e.registrar(TELEFONOS[2], "Carla");
        dani = e.registrar(TELEFONOS[3], "Dani");
        restaurante = e.crearNegocio(ana.token(), "Fonda Doña Chela", "RESTAURANTE");
        e.agregarMiembro(restaurante, beto.cuenta().id(), "ADMINISTRADOR");
        e.agregarMiembro(restaurante, carla.cuenta().id(), "CAJERO");
        e.agregarMiembro(restaurante, dani.cuenta().id(), "GERENTE");
        tokenDeAna = e.api.elegirNegocio(ana.token(), restaurante).token();
    }

    /** Una sesion nueva de esa persona con el negocio elegido (la sesion con la que se registro no se gasta). */
    private String tokenEn(UUID negocio, SesionIniciada persona) {
        e.reloj.avanzar(Duration.ofMinutes(16));
        var sesion = e.iniciar(persona.cuenta().telefono());
        return e.api.elegirNegocio(sesion.token(), negocio).token();
    }

    private String rolDe(SesionIniciada persona) {
        return e.membresias.porCuentaYNegocio(persona.cuenta().id(), restaurante).orElseThrow().rolCodigo();
    }

    private EstadoMembresia estadoDe(SesionIniciada persona) {
        return e.membresias.porCuentaYNegocio(persona.cuenta().id(), restaurante).orElseThrow().estado();
    }

    private long eventos(String tipo) {
        return e.eventos.eventos().stream().filter(ev -> ev.tipo().equals(tipo)).count();
    }

    // ---- ver al equipo ----

    @Test
    @DisplayName("el equipo se lista con el celular enmascarado (AP-03), el rol y el estado")
    void equipo_listaConCelularEnmascarado() {
        var equipo = e.api.equipo(tokenDeAna);

        assertThat(equipo).hasSize(4);
        assertThat(equipo).extracting(m -> m.rol()).containsExactlyInAnyOrder("DUENO", "ADMINISTRADOR", "CAJERO", "GERENTE");
        assertThat(equipo).allSatisfy(m -> {
            assertThat(m.telefonoEnmascarado()).startsWith("••••").doesNotContain("2221110");
            assertThat(m.estado()).isEqualTo("ACTIVA");
        });
        assertThat(equipo).filteredOn(m -> m.cuentaId().equals(carla.cuenta().id())).singleElement()
                .satisfies(m -> assertThat(m.nombre()).isEqualTo("Carla"));
    }

    @Test
    @DisplayName("ver al equipo exige identidad.equipo.ver: el gerente puede, el cajero no")
    void equipo_exigeElPermiso() {
        var gerente = tokenEn(restaurante, dani);
        var cajera = tokenEn(restaurante, carla);

        assertThat(e.api.equipo(gerente)).hasSize(4);
        fallaCon(() -> e.api.equipo(cajera), CodigoError.PERMISO_DENEGADO);
    }

    @Test
    @DisplayName("sin negocio activo o sin sesion no se ve ni se administra a ningun equipo")
    void equipo_sinNegocioOSinSesion_falla() {
        var sinNegocio = e.registrar(TELEFONOS[4], "Ernesto").token();

        fallaCon(() -> e.api.equipo(sinNegocio), CodigoError.NEGOCIO_NO_DISPONIBLE);
        fallaCon(() -> e.api.equipo("token-inventado"), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.cambiarRol(sinNegocio, carla.cuenta().id(), "GERENTE"), CodigoError.NEGOCIO_NO_DISPONIBLE);
        fallaCon(() -> e.api.salir(sinNegocio), CodigoError.NEGOCIO_NO_DISPONIBLE);
        fallaCon(() -> e.api.revocar(null, carla.cuenta().id()), CodigoError.NO_AUTENTICADO);
    }

    // ---- cambiar el rol ----

    @Test
    @DisplayName("el dueño cambia el rol de un miembro: se guarda, sube la version y se publica membresia_cambiada")
    void cambiarRol_porElDueno() {
        var versionAntes = e.membresias.porCuentaYNegocio(carla.cuenta().id(), restaurante).orElseThrow().version();

        var miembro = e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), "GERENTE");

        assertThat(miembro.rol()).isEqualTo("GERENTE");
        assertThat(rolDe(carla)).isEqualTo("GERENTE");
        assertThat(e.membresias.porCuentaYNegocio(carla.cuenta().id(), restaurante).orElseThrow().version())
                .isEqualTo(versionAntes + 1);
        assertThat(e.eventos.eventos()).filteredOn(ev -> ev.tipo().equals("identidad.membresia_cambiada")).singleElement()
                .satisfies(ev -> {
                    assertThat(ev.negocioId()).isEqualTo(restaurante);
                    assertThat(ev.datos()).containsEntry("cuenta_id", carla.cuenta().id().toString())
                            .containsEntry("rol_anterior", "CAJERO").containsEntry("rol_nuevo", "GERENTE")
                            .containsEntry("estado", "ACTIVA");
                    assertThat(ev.toString()).as("AP-02").doesNotContain("Carla").doesNotContain("2221110003");
                });
    }

    @Test
    @DisplayName("AM-08: la persona con sesion abierta recibe los permisos de su rol nuevo en la siguiente peticion")
    void cambiarRol_laSesionAbiertaSeActualiza() {
        var tokenCarla = tokenEn(restaurante, carla);
        assertThat(e.api.yo(tokenCarla).permisos()).doesNotContain("clientes.lista.ver");

        e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), "GERENTE");

        assertThat(e.api.yo(tokenCarla).permisos()).contains("clientes.lista.ver");
        assertThat(e.api.yo(tokenCarla).rol()).isEqualTo("GERENTE");
    }

    @Test
    @DisplayName("el mismo rol no hace nada: ni version nueva ni evento")
    void cambiarRol_mismoRol_noHaceNada() {
        e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), "CAJERO");

        assertThat(eventos("identidad.membresia_cambiada")).isZero();
        assertThat(e.membresias.porCuentaYNegocio(carla.cuenta().id(), restaurante).orElseThrow().version()).isZero();
    }

    @Test
    @DisplayName("AM-05: el administrador puede dar roles menores, pero no el de dueño porque no tiene sus permisos")
    void cambiarRol_elAdministradorNoOtorgaMasDeLoQueTiene() {
        var tokenBeto = tokenEn(restaurante, beto);

        e.api.cambiarRol(tokenBeto, carla.cuenta().id(), "GERENTE");
        fallaCon(() -> e.api.cambiarRol(tokenBeto, carla.cuenta().id(), "DUENO"), CodigoError.ROL_NO_PERMITIDO);

        assertThat(rolDe(carla)).isEqualTo("GERENTE");
    }

    @Test
    @DisplayName("AM-05: el administrador no cambia ni revoca ni suspende a un dueño")
    void elAdministradorNoToca_alDueno() {
        var tokenBeto = tokenEn(restaurante, beto);

        fallaCon(() -> e.api.cambiarRol(tokenBeto, ana.cuenta().id(), "CAJERO"), CodigoError.ROL_NO_PERMITIDO);
        fallaCon(() -> e.api.suspender(tokenBeto, ana.cuenta().id()), CodigoError.ROL_NO_PERMITIDO);
        fallaCon(() -> e.api.revocar(tokenBeto, ana.cuenta().id()), CodigoError.ROL_NO_PERMITIDO);

        assertThat(rolDe(ana)).isEqualTo("DUENO");
        assertThat(estadoDe(ana)).isEqualTo(EstadoMembresia.ACTIVA);
    }

    @Test
    @DisplayName("el gerente y el cajero no administran al equipo: PERMISO_DENEGADO")
    void sinPermiso_noAdministran() {
        var gerente = tokenEn(restaurante, dani);
        var cajera = tokenEn(restaurante, carla);

        fallaCon(() -> e.api.cambiarRol(gerente, carla.cuenta().id(), "MESERO"), CodigoError.PERMISO_DENEGADO);
        fallaCon(() -> e.api.suspender(gerente, carla.cuenta().id()), CodigoError.PERMISO_DENEGADO);
        fallaCon(() -> e.api.revocar(cajera, dani.cuenta().id()), CodigoError.PERMISO_DENEGADO);
        fallaCon(() -> e.api.reactivar(cajera, dani.cuenta().id()), CodigoError.PERMISO_DENEGADO);
    }

    @Test
    @DisplayName("solo se ofrecen los roles de la plantilla del giro: no hay taquillero en un restaurante ni roles inventados")
    void cambiarRol_fueraDeLaPlantilla_noPermitido() {
        fallaCon(() -> e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), "TAQUILLERO"), CodigoError.ROL_NO_PERMITIDO);
        fallaCon(() -> e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), "SUPERUSUARIO"), CodigoError.ROL_NO_PERMITIDO);
        fallaCon(() -> e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), null), CodigoError.ROL_NO_PERMITIDO);

        assertThat(rolDe(carla)).isEqualTo("CAJERO");
    }

    @Test
    @DisplayName("AM-07: nadie cambia su propio rol ni se suspende ni se revoca a si mismo desde la administracion")
    void operacionesSobreUnoMismo_noPermitidas() {
        fallaCon(() -> e.api.cambiarRol(tokenDeAna, ana.cuenta().id(), "GERENTE"), CodigoError.OPERACION_SOBRE_SI_MISMO);
        fallaCon(() -> e.api.suspender(tokenDeAna, ana.cuenta().id()), CodigoError.OPERACION_SOBRE_SI_MISMO);
        fallaCon(() -> e.api.revocar(tokenDeAna, ana.cuenta().id()), CodigoError.OPERACION_SOBRE_SI_MISMO);
        fallaCon(() -> e.api.reactivar(tokenDeAna, ana.cuenta().id()), CodigoError.OPERACION_SOBRE_SI_MISMO);

        assertThat(rolDe(ana)).isEqualTo("DUENO");
    }

    @Test
    @DisplayName("quien no es miembro de este negocio recibe NO_ES_MIEMBRO, tambien si lo es de otro (aislamiento)")
    void noEsMiembro_tambienDeOtroNegocio() {
        var ernesto = e.registrar(TELEFONOS[4], "Ernesto");
        var tienda = e.crearNegocio(ernesto.token(), "Tienda de Ernesto", "MISCELANEA");
        var tokenBeto = tokenEn(restaurante, beto);

        fallaCon(() -> e.api.cambiarRol(tokenBeto, ernesto.cuenta().id(), "CAJERO"), CodigoError.NO_ES_MIEMBRO);
        fallaCon(() -> e.api.revocar(tokenBeto, UUID.randomUUID()), CodigoError.NO_ES_MIEMBRO);
        assertThat(e.membresias.porCuentaYNegocio(ernesto.cuenta().id(), tienda).orElseThrow().rolCodigo())
                .isEqualTo("DUENO");
    }

    @Test
    @DisplayName("el dueño puede ascender a otro a dueño, y entonces ese puede degradar al primero (siempre queda uno)")
    void dobleDueno_puedenCambiarseEntreSi() {
        e.api.cambiarRol(tokenDeAna, beto.cuenta().id(), "DUENO");
        var tokenBeto = tokenEn(restaurante, beto);

        e.api.cambiarRol(tokenBeto, ana.cuenta().id(), "GERENTE");

        assertThat(rolDe(beto)).isEqualTo("DUENO");
        assertThat(rolDe(ana)).isEqualTo("GERENTE");
        assertThat(e.api.autenticar(tokenDeAna).orElseThrow().rol()).as("el rol se renueva en la sesion de Ana")
                .isEqualTo("GERENTE");
    }

    // ---- suspender, reactivar, revocar ----

    @Test
    @DisplayName("AM-06: suspender revoca las sesiones de esa persona en ese negocio, no las de otros negocios")
    void suspender_revocaLasSesionesDelNegocio() {
        var ernesto = e.registrar(TELEFONOS[4], "Ernesto");
        var tienda = e.crearNegocio(ernesto.token(), "Tienda", "MISCELANEA");
        e.agregarMiembro(tienda, carla.cuenta().id(), "CAJERO");
        var enRestaurante = tokenEn(restaurante, carla);
        var enTienda = tokenEn(tienda, carla);

        var miembro = e.api.suspender(tokenDeAna, carla.cuenta().id());

        assertThat(miembro.estado()).isEqualTo("SUSPENDIDA");
        assertThat(estadoDe(carla)).isEqualTo(EstadoMembresia.SUSPENDIDA);
        assertThat(e.api.autenticar(enRestaurante)).isEmpty();
        assertThat(e.api.autenticar(enTienda)).isPresent();
        fallaCon(() -> e.api.elegirNegocio(enTienda, restaurante), CodigoError.NEGOCIO_NO_DISPONIBLE);
        assertThat(e.eventos.eventos()).filteredOn(ev -> ev.tipo().equals("identidad.membresia_cambiada")).singleElement()
                .satisfies(ev -> assertThat(ev.datos()).containsEntry("estado", "SUSPENDIDA"));
    }

    @Test
    @DisplayName("reactivar devuelve la membresia a ACTIVA y la persona vuelve a poder elegir el negocio")
    void reactivar_devuelveAActiva() {
        e.api.suspender(tokenDeAna, carla.cuenta().id());
        e.eventos.limpiar();

        var miembro = e.api.reactivar(tokenDeAna, carla.cuenta().id());

        assertThat(miembro.estado()).isEqualTo("ACTIVA");
        assertThat(tokenEn(restaurante, carla)).isNotBlank();
        assertThat(e.api.yo(tokenEn(restaurante, carla)).negocioActivo()).isEqualTo(restaurante);
        assertThat(e.eventos.eventos()).singleElement().satisfies(ev -> {
            assertThat(ev.tipo()).isEqualTo("identidad.membresia_cambiada");
            assertThat(ev.datos()).containsEntry("estado", "ACTIVA");
        });
    }

    @Test
    @DisplayName("suspender a quien ya esta suspendido no repite el evento, y reactivar a quien esta activo tampoco")
    void suspenderYReactivar_sonIdempotentes() {
        e.api.suspender(tokenDeAna, carla.cuenta().id());
        e.api.suspender(tokenDeAna, carla.cuenta().id());
        e.api.reactivar(tokenDeAna, carla.cuenta().id());
        e.api.reactivar(tokenDeAna, carla.cuenta().id());

        assertThat(eventos("identidad.membresia_cambiada")).isEqualTo(2);
    }

    @Test
    @DisplayName("revocar deja la membresia REVOCADA, revoca sus sesiones del negocio y publica membresia_revocada")
    void revocar_quitaALaPersona() {
        var tokenCarla = tokenEn(restaurante, carla);

        e.api.revocar(tokenDeAna, carla.cuenta().id());

        assertThat(estadoDe(carla)).isEqualTo(EstadoMembresia.REVOCADA);
        assertThat(e.api.autenticar(tokenCarla)).isEmpty();
        assertThat(e.api.equipo(tokenDeAna)).extracting(m -> m.cuentaId()).doesNotContain(carla.cuenta().id());
        assertThat(e.eventos.eventos()).filteredOn(ev -> ev.tipo().equals("identidad.membresia_revocada")).singleElement()
                .satisfies(ev -> assertThat(ev.datos()).containsEntry("cuenta_id", carla.cuenta().id().toString())
                        .containsEntry("motivo", "REVOCADA"));
    }

    @Test
    @DisplayName("una membresia revocada no vuelve: no se reactiva, no se suspende y no se le cambia el rol")
    void revocada_noVuelve() {
        e.api.revocar(tokenDeAna, carla.cuenta().id());

        fallaCon(() -> e.api.reactivar(tokenDeAna, carla.cuenta().id()), CodigoError.NO_ES_MIEMBRO);
        fallaCon(() -> e.api.suspender(tokenDeAna, carla.cuenta().id()), CodigoError.NO_ES_MIEMBRO);
        fallaCon(() -> e.api.cambiarRol(tokenDeAna, carla.cuenta().id(), "GERENTE"), CodigoError.NO_ES_MIEMBRO);
        fallaCon(() -> e.api.revocar(tokenDeAna, carla.cuenta().id()), CodigoError.NO_ES_MIEMBRO);
    }

    // ---- salir ----

    @Test
    @DisplayName("salir del negocio revoca la membresia propia y sus sesiones, con motivo SALIO")
    void salir_revocaLaMembresiaPropia() {
        var tokenCarla = tokenEn(restaurante, carla);

        e.api.salir(tokenCarla);

        assertThat(estadoDe(carla)).isEqualTo(EstadoMembresia.REVOCADA);
        assertThat(e.api.autenticar(tokenCarla)).isEmpty();
        assertThat(e.eventos.eventos()).filteredOn(ev -> ev.tipo().equals("identidad.membresia_revocada")).singleElement()
                .satisfies(ev -> assertThat(ev.datos()).containsEntry("motivo", "SALIO"));
    }

    @Test
    @DisplayName("AM-02: el unico dueño no puede salir; con otro dueño si puede")
    void salir_elUltimoDueno_noPuede() {
        fallaCon(() -> e.api.salir(tokenDeAna), CodigoError.ULTIMO_DUENO);
        assertThat(estadoDe(ana)).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(e.api.autenticar(tokenDeAna)).isPresent();

        e.api.cambiarRol(tokenDeAna, beto.cuenta().id(), "DUENO");
        e.api.salir(tokenDeAna);

        assertThat(estadoDe(ana)).isEqualTo(EstadoMembresia.REVOCADA);
        assertThat(rolDe(beto)).isEqualTo("DUENO");
    }

    @Test
    @DisplayName("AM-02: un dueño suspendido no cuenta como dueño que queda: el activo no puede salir")
    void salir_conOtroDuenoSuspendido_sigueSiendoElUltimo() {
        e.api.cambiarRol(tokenDeAna, beto.cuenta().id(), "DUENO");
        e.api.suspender(tokenDeAna, beto.cuenta().id());

        fallaCon(() -> e.api.salir(tokenDeAna), CodigoError.ULTIMO_DUENO);
    }

    @Test
    @DisplayName("suspender no toca la sesion de la persona que no tiene negocio activo")
    void suspender_noTocaSesionesSinNegocio() {
        e.api.suspender(tokenDeAna, carla.cuenta().id());

        assertThat(e.api.autenticar(carla.token())).isPresent();
    }
}
