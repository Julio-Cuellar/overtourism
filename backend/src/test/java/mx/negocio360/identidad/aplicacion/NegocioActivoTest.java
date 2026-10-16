package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.dominio.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * IDN-2.4 · AS-06, AM-03, AM-06, AM-08, SI-07: el negocio activo de la sesion, el selector de negocio, y el rol y los
 * permisos de la persona en ese negocio. El negocio_id solo se lee de la sesion (AZ-05).
 */
class NegocioActivoTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("quien tiene un solo negocio entra con ese negocio activo y su rol")
    void iniciar_conUnNegocio_loActiva() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocio = e.crearNegocio(ana.token(), "Fonda Doña Chela", "RESTAURANTE");
        e.reloj.avanzar(Duration.ofMinutes(20));

        var sesion = e.iniciar(TEL_A);

        assertThat(sesion.negocioActivo()).isEqualTo(negocio);
        assertThat(sesion.negocios()).singleElement().satisfies(n -> {
            assertThat(n.id()).isEqualTo(negocio);
            assertThat(n.nombre()).isEqualTo("Fonda Doña Chela");
            assertThat(n.rol()).isEqualTo("DUENO");
        });
        assertThat(e.api.autenticar(sesion.token()).orElseThrow().contextoNegocio()).hasValueSatisfying(contexto -> {
            assertThat(contexto.negocioId()).isEqualTo(negocio);
            assertThat(contexto.actor()).isEqualTo(ana.cuenta().id().toString());
        });
    }

    @Test
    @DisplayName("AM-03: con dos negocios entra sin negocio activo y los ve todos con su rol en cada uno")
    void iniciar_conDosNegocios_noActivaNinguno() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var fonda = e.crearNegocio(ana.token(), "Fonda Uno", "RESTAURANTE");
        var tienda = e.crearNegocio(beto.token(), "Tienda Dos", "MISCELANEA");
        e.agregarMiembro(tienda, ana.cuenta().id(), "CAJERO");
        e.reloj.avanzar(Duration.ofMinutes(20));

        var sesion = e.iniciar(TEL_A);

        assertThat(sesion.negocioActivo()).isNull();
        assertThat(sesion.negocios()).extracting(n -> n.id(), n -> n.rol())
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(fonda, "DUENO"),
                        org.assertj.core.groups.Tuple.tuple(tienda, "CAJERO"));
    }

    @Test
    @DisplayName("AC-02: una cuenta sin negocios entra y su lista esta vacia")
    void iniciar_sinNegocios_listaVacia() {
        var sesion = e.registrar(TEL_A, "Ana");

        assertThat(sesion.negocios()).isEmpty();
        assertThat(sesion.negocioActivo()).isNull();
        assertThat(e.api.yo(sesion.token()).rol()).isNull();
        assertThat(e.api.yo(sesion.token()).permisos()).isEmpty();
    }

    @Test
    @DisplayName("yo devuelve el rol y los permisos del negocio activo, en orden")
    void yo_conNegocioActivo_devuelveRolYPermisos() {
        var ana = e.registrar(TEL_A, "Ana");
        e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        e.reloj.avanzar(Duration.ofMinutes(20));
        var sesion = e.iniciar(TEL_A);

        var perfil = e.api.yo(sesion.token());

        assertThat(perfil.rol()).isEqualTo("DUENO");
        assertThat(perfil.permisos()).hasSize(16).isSorted().contains("identidad.miembro.invitar", "clientes.sugerir");
        assertThat(perfil.negocioActivo()).isEqualTo(sesion.negocioActivo());
    }

    @Test
    @DisplayName("D-80: la sesion abierta antes de crear el negocio lo activa sola cuando es el unico, sin cambiar de token")
    void yo_negocioCreadoDespues_seActivaSolo() {
        var ana = e.registrar(TEL_A, "Ana");

        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");

        var perfil = e.api.yo(ana.token());
        assertThat(perfil.negocios()).extracting(n -> n.id()).containsExactly(negocio);
        assertThat(perfil.negocioActivo()).isEqualTo(negocio);
        assertThat(perfil.rol()).isEqualTo("DUENO");
        assertThat(perfil.permisos()).hasSize(16);
        var sesion = e.api.autenticar(ana.token()).orElseThrow();
        assertThat(sesion.negocioActivo()).contains(negocio);
        assertThat(sesion.contextoNegocio()).hasValueSatisfying(c -> assertThat(c.negocioId()).isEqualTo(negocio));
        assertThat(sesion.autorizacion().puede("identidad.miembro.invitar")).isTrue();
    }

    @Test
    @DisplayName("D-80: con la activacion implicita ya se puede administrar el equipo sin elegir negocio")
    void equipo_conNegocioImplicito_funcionaSinElegir() {
        var ana = e.registrar(TEL_A, "Ana");
        e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");

        assertThat(e.api.equipo(ana.token())).singleElement().satisfies(m -> assertThat(m.rol()).isEqualTo("DUENO"));
        assertThat(e.api.roles(ana.token())).extracting(r -> r.codigo()).contains("DUENO", "MESERO");
    }

    @Test
    @DisplayName("D-80: con dos negocios no se activa ninguno solo: hay que elegir, y mientras tanto todo esta negado")
    void autenticar_conDosNegocios_noActivaNinguno() {
        var ana = e.registrar(TEL_A, "Ana");
        e.crearNegocio(ana.token(), "Fonda Uno", "RESTAURANTE");
        e.reloj.avanzar(Duration.ofSeconds(1));
        var tienda = e.crearNegocio(ana.token(), "Tienda Dos", "MISCELANEA");

        var perfil = e.api.yo(ana.token());

        assertThat(perfil.negocios()).hasSize(2);
        assertThat(perfil.negocioActivo()).isNull();
        assertThat(perfil.rol()).isNull();
        fallaCon(() -> e.api.equipo(ana.token()), CodigoError.NEGOCIO_NO_DISPONIBLE);
        assertThat(e.api.elegirNegocio(ana.token(), tienda).negocioActivo()).isEqualTo(tienda);
    }

    @Test
    @DisplayName("D-80: cuando llega un segundo negocio la sesion pierde el activo implicito (falla cerrado)")
    void autenticar_alLlegarUnSegundoNegocio_sePierdeElImplicito() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var fonda = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        assertThat(e.api.yo(ana.token()).negocioActivo()).isEqualTo(fonda);
        var tienda = e.crearNegocio(beto.token(), "Tienda", "MISCELANEA");

        e.agregarMiembro(tienda, ana.cuenta().id(), "CAJERO");

        assertThat(e.api.yo(ana.token()).negocioActivo()).isNull();
        assertThat(e.api.autenticar(ana.token()).orElseThrow().permisos()).isEmpty();
    }

    @Test
    @DisplayName("D-80: una membresia suspendida no cuenta como unica: no se activa nada")
    void autenticar_unicaMembresiaSuspendida_noActivaNada() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var negocio = e.crearNegocio(beto.token(), "Tienda", "MISCELANEA");
        var membresia = e.agregarMiembro(negocio, ana.cuenta().id(), "CAJERO");
        assertThat(e.api.yo(ana.token()).negocioActivo()).isEqualTo(negocio);

        e.membresias.guardar(membresia.suspendida(e.reloj.instant()));

        assertThat(e.api.yo(ana.token()).negocioActivo()).isNull();
        assertThat(e.api.yo(ana.token()).negocios()).isEmpty();
    }

    @Test
    @DisplayName("D-80: elegir un negocio explicito sigue funcionando aunque haya uno solo, y no alarga la sesion")
    void elegir_conUnSoloNegocio_sigueFuncionando() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");

        var nueva = e.api.elegirNegocio(ana.token(), negocio);

        assertThat(nueva.token()).isNotEqualTo(ana.token());
        assertThat(e.api.autenticar(ana.token())).isEmpty();
        assertThat(e.api.yo(nueva.token()).negocioActivo()).isEqualTo(negocio);
    }

    // ---- elegir negocio ----

    @Test
    @DisplayName("SI-07: elegir negocio emite otro token, revoca el anterior y conserva el tope absoluto")
    void elegir_emiteOtroTokenYRevocaElAnterior() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        var antes = e.sesiones.porHash(TokenOpaco.hash(ana.token())).orElseThrow();
        e.reloj.avanzar(Duration.ofHours(2));

        var nueva = e.api.elegirNegocio(ana.token(), negocio);

        assertThat(nueva.token()).isNotEqualTo(ana.token());
        assertThat(nueva.negocioActivo()).isEqualTo(negocio);
        assertThat(e.api.autenticar(ana.token())).as("el token anterior ya no sirve").isEmpty();
        var despues = e.sesiones.porHash(TokenOpaco.hash(nueva.token())).orElseThrow();
        assertThat(despues.expiraAbsolutaEn()).as("cambiar de negocio no alarga la sesion").isEqualTo(antes.expiraAbsolutaEn());
        assertThat(despues.dispositivo()).isEqualTo(antes.dispositivo());
        assertThat(despues.ipTruncada()).isEqualTo(antes.ipTruncada());
        assertThat(despues.negocioId()).isEqualTo(negocio);
        assertThat(e.api.yo(nueva.token()).rol()).isEqualTo("DUENO");
    }

    @Test
    @DisplayName("AS-06: pedir un negocio donde no se es miembro da el mismo error que uno que no existe")
    void elegir_noMiembroOInexistente_mismoError() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var deBeto = e.crearNegocio(beto.token(), "Tienda de Beto", "MISCELANEA");
        var inexistente = UuidV7.generar(e.reloj.instant());

        var ajeno = fallaCon(() -> e.api.elegirNegocio(ana.token(), deBeto), CodigoError.NEGOCIO_NO_DISPONIBLE);
        var noExiste = fallaCon(() -> e.api.elegirNegocio(ana.token(), inexistente), CodigoError.NEGOCIO_NO_DISPONIBLE);

        assertThat(ajeno.getMessage()).isEqualTo(noExiste.getMessage());
        assertThat(e.api.autenticar(ana.token())).as("fallar no revoca la sesion").isPresent();
    }

    @Test
    @DisplayName("sin id de negocio o sin sesion valida no se elige nada")
    void elegir_sinIdOSinSesion_falla() {
        var ana = e.registrar(TEL_A, "Ana");

        fallaCon(() -> e.api.elegirNegocio(ana.token(), null), CodigoError.NEGOCIO_NO_DISPONIBLE);
        fallaCon(() -> e.api.elegirNegocio("token-inventado", UUID.randomUUID()), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.elegirNegocio(null, UUID.randomUUID()), CodigoError.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("una membresia suspendida o revocada ya no deja elegir ese negocio ni aparece en la lista")
    void elegir_membresiaNoActiva_noDisponible() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        var membresia = e.agregarMiembro(negocio, beto.cuenta().id(), "CAJERO");
        e.membresias.guardar(membresia.suspendida(e.reloj.instant()));

        fallaCon(() -> e.api.elegirNegocio(beto.token(), negocio), CodigoError.NEGOCIO_NO_DISPONIBLE);
        assertThat(e.api.yo(beto.token()).negocios()).isEmpty();

        e.membresias.guardar(membresia.suspendida(e.reloj.instant()).revocada(e.reloj.instant()));
        fallaCon(() -> e.api.elegirNegocio(beto.token(), negocio), CodigoError.NEGOCIO_NO_DISPONIBLE);
        assertThat(e.api.yo(beto.token()).negocios()).isEmpty();
    }

    // ---- AM-06 y AM-08 ----

    @Test
    @DisplayName("AM-06: si la membresia del negocio activo deja de estar activa, el token ya no autentica")
    void autenticar_membresiaRevocada_tokenCae() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        var membresia = e.agregarMiembro(negocio, beto.cuenta().id(), "CAJERO");
        var enNegocio = e.api.elegirNegocio(beto.token(), negocio).token();
        assertThat(e.api.autenticar(enNegocio)).isPresent();

        e.membresias.guardar(membresia.revocada(e.reloj.instant()));

        assertThat(e.api.autenticar(enNegocio)).isEmpty();
        fallaCon(() -> e.api.yo(enNegocio), CodigoError.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("AM-08: autenticar entrega el rol y los permisos al dia, sin esperar a que venza la sesion")
    void autenticar_rolCambiado_permisosAlDia() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        var membresia = e.agregarMiembro(negocio, beto.cuenta().id(), "CAJERO");
        var token = e.api.elegirNegocio(beto.token(), negocio).token();
        assertThat(e.api.autenticar(token).orElseThrow().permisos()).doesNotContain("clientes.lista.ver");

        e.membresias.guardar(membresia.conRol("GERENTE", e.reloj.instant()));

        var sesion = e.api.autenticar(token).orElseThrow();
        assertThat(sesion.rol()).isEqualTo("GERENTE");
        assertThat(sesion.permisos()).contains("clientes.lista.ver", "identidad.equipo.ver");
        assertThat(sesion.autorizacion().puede("clientes.lista.ver")).isTrue();
        assertThat(sesion.autorizacion().puede("clientes.cliente.anonimizar")).isFalse();
    }

    @Test
    @DisplayName("una sesion sin negocio activo no tiene rol ni permisos: todo esta negado (AZ-04)")
    void autenticar_sinNegocio_todoNegado() {
        var ana = e.registrar(TEL_A, "Ana");

        var sesion = e.api.autenticar(ana.token()).orElseThrow();

        assertThat(sesion.rol()).isNull();
        assertThat(sesion.permisos()).isEmpty();
        assertThat(sesion.autorizacion().puede("clientes.sugerir")).isFalse();
        assertThat(sesion.negocioActivo()).isEmpty();
    }

    @Test
    @DisplayName("la sesion de otro negocio de la misma persona no se ve afectada por la baja en un negocio")
    void autenticar_otroNegocio_sigueFuncionando() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var fonda = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        var tienda = e.crearNegocio(beto.token(), "Tienda", "MISCELANEA");
        var enFonda = e.agregarMiembro(fonda, beto.cuenta().id(), "CAJERO");
        var tokenTienda = e.api.elegirNegocio(beto.token(), tienda).token();

        e.membresias.guardar(enFonda.revocada(e.reloj.instant()));

        assertThat(e.api.autenticar(tokenTienda)).isPresent();
        assertThat(List.copyOf(e.api.yo(tokenTienda).negocios())).extracting(n -> n.id()).containsExactly(tienda);
    }
}
