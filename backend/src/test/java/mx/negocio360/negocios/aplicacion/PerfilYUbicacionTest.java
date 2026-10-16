package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.negocios.ActualizarNegocio;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.infraestructura.EjecutorEnContexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** N2: perfil y contacto (NG-01, NG-05, NG-09 a NG-11, NG-13), ubicacion (SU-03, SU-04) y sus eventos, con Postgres. */
@SpringBootTest
@RecordApplicationEvents
class PerfilYUbicacionTest {

    @Autowired
    private NegociosApi negocios;
    @Autowired
    private AltasApi altas;
    @Autowired
    private EjecutorEnContexto ejecutor;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private ApplicationEvents eventos;

    private static CodigoError codigo(Throwable error) {
        assertThat(error).isInstanceOf(NegociosException.class);
        return ((NegociosException) error).codigo();
    }

    private SobreEvento evento(String tipo, java.util.UUID negocio) {
        return eventos.stream(SobreEvento.class).filter(e -> e.tipo().equals(tipo) && e.negocioId().equals(negocio))
                .reduce((primero, ultimo) -> ultimo).orElseThrow();
    }

    // ---- miNegocio y completitud ----

    @Test
    @DisplayName("NG-11: miNegocio de un negocio recien dado de alta vale 40 % y trae los cuatro faltantes")
    void miNegocio_recienDadoDeAlta() {
        var e = Escenario.nuevo(altas);

        var vista = negocios.miNegocio(e.contexto, e.dueno);

        assertThat(vista.id()).isEqualTo(e.negocioId);
        assertThat(vista.nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(vista.giro()).isEqualTo("RESTAURANTE");
        assertThat(vista.giroNombre()).isEqualTo("Restaurante");
        assertThat(vista.estado()).isEqualTo("ACTIVO");
        assertThat(vista.publicado()).isFalse();
        assertThat(vista.completitud().porcentaje()).isEqualTo(40);
        assertThat(vista.completitud().faltantes()).containsExactly("UBICACION", "HORARIO", "CONTACTO", "LOGO");
        assertThat(vista.principal().esPrincipal()).isTrue();
        assertThat(vista.subcategorias()).isEmpty();
        assertThat(vista.esSimulado()).isFalse();
    }

    @Test
    @DisplayName("SG-04: sin negocios.perfil.ver no se ve el perfil: PERMISO_DENEGADO")
    void miNegocio_sinPermiso() {
        var e = Escenario.nuevo(altas);

        assertThatThrownBy(() -> negocios.miNegocio(e.contexto, e.nadie)).isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("SN-02: con el contexto de otro negocio, miNegocio responde que no esta disponible")
    void miNegocio_deOtroNegocio_noExiste() {
        var a = Escenario.nuevo(altas);
        var b = Escenario.nuevo(altas);
        var contextoDeB = new mx.negocio360.compartido.contexto.ContextoNegocio(b.negocioId, "otro");

        assertThat(negocios.miNegocio(contextoDeB, b.dueno).id()).isEqualTo(b.negocioId);
        assertThat(negocios.miNegocio(a.contexto, a.dueno).id()).isEqualTo(a.negocioId);
        var desconocido = new mx.negocio360.compartido.contexto.ContextoNegocio(Datos.id(), "nadie");
        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.miNegocio(desconocido, a.dueno));
        assertThat(codigo(error)).isEqualTo(CodigoError.NEGOCIO_NO_DISPONIBLE);
    }

    // ---- actualizarNegocio ----

    @Test
    @DisplayName("actualizarNegocio cambia el perfil, normaliza el contacto, sube la version y publica negocio.actualizado")
    void actualizarNegocio_cambiaElPerfil() {
        var e = Escenario.nuevo(altas);

        var vista = negocios.actualizarNegocio(e.contexto, e.dueno, new ActualizarNegocio(0, "  Fonda  Chela  ",
                "Comida poblana de la casa", "222 555 0101", "  Chela@Fonda.EXAMPLE ", true));

        assertThat(vista.nombre()).isEqualTo("Fonda Chela");
        assertThat(vista.descripcion()).isEqualTo("Comida poblana de la casa");
        assertThat(vista.telefonoContacto()).isEqualTo("+522225550101");
        assertThat(vista.correoContacto()).isEqualTo("chela@fonda.example");
        assertThat(vista.contactoPublico()).isTrue();
        assertThat(vista.version()).isEqualTo(1);
        assertThat(vista.completitud().porcentaje()).isEqualTo(50);
        var sobre = evento("negocio.actualizado", e.negocioId);
        assertThat(sobre.origen()).isEqualTo("negocios");
        assertThat(sobre.datos()).containsEntry("nombre", "Fonda Chela").containsEntry("giro", "RESTAURANTE")
                .containsEntry("publicado", false);
        assertThat((List<Object>) sobre.datos().get("campos")).containsExactlyInAnyOrder("nombre", "descripcion",
                "telefono_contacto", "correo_contacto", "contacto_publico");
        assertThat(sobre.datos().toString()).doesNotContain("5550101").doesNotContain("chela@fonda");
    }

    @Test
    @DisplayName("lo que no se manda no cambia; un texto vacio borra el campo opcional")
    void actualizarNegocio_parcial_yBorrado() {
        var e = Escenario.nuevo(altas);
        negocios.actualizarNegocio(e.contexto, e.dueno,
                new ActualizarNegocio(0, null, "Una descripcion", "2225550101", null, null));

        var vista = negocios.actualizarNegocio(e.contexto, e.dueno,
                new ActualizarNegocio(1, null, "", "", null, null));

        assertThat(vista.nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(vista.descripcion()).isNull();
        assertThat(vista.telefonoContacto()).isNull();
        assertThat(vista.completitud().faltantes()).contains("CONTACTO");
    }

    @Test
    @DisplayName("NG-10: una edicion con una version vieja falla con CONCURRENCIA y no pisa nada")
    void actualizarNegocio_versionVieja() {
        var e = Escenario.nuevo(altas);
        negocios.actualizarNegocio(e.contexto, e.dueno, new ActualizarNegocio(0, "Primero", null, null, null, null));

        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(e.contexto,
                e.dueno, new ActualizarNegocio(0, "Segundo", null, null, null, null)));

        assertThat(codigo(error)).isEqualTo(CodigoError.CONCURRENCIA);
        assertThat(negocios.miNegocio(e.contexto, e.dueno).nombre()).isEqualTo("Primero");
    }

    @Test
    @DisplayName("NG-01 y NG-09: nombre y contacto invalidos fallan y no cambian nada")
    void actualizarNegocio_datosInvalidos() {
        var e = Escenario.nuevo(altas);

        var nombre = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(e.contexto,
                e.dueno, new ActualizarNegocio(0, "A", null, null, null, null)));
        var telefono = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(e.contexto,
                e.dueno, new ActualizarNegocio(0, null, null, "123", null, null)));
        var correo = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(e.contexto,
                e.dueno, new ActualizarNegocio(0, null, null, null, "a@", null)));
        var larga = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(e.contexto,
                e.dueno, new ActualizarNegocio(0, null, "x".repeat(281), null, null, null)));

        assertThat(codigo(nombre)).isEqualTo(CodigoError.NOMBRE_INVALIDO);
        assertThat(codigo(telefono)).isEqualTo(CodigoError.CONTACTO_INVALIDO);
        assertThat(codigo(correo)).isEqualTo(CodigoError.CONTACTO_INVALIDO);
        assertThat(codigo(larga)).isEqualTo(CodigoError.DESCRIPCION_INVALIDA);
        assertThat(negocios.miNegocio(e.contexto, e.dueno).version()).isZero();
    }

    @Test
    @DisplayName("SG-04: editar el perfil pide negocios.perfil.editar")
    void actualizarNegocio_sinPermiso() {
        var e = Escenario.nuevo(altas);

        assertThatThrownBy(() -> negocios.actualizarNegocio(e.contexto, e.con("negocios.perfil.ver"),
                new ActualizarNegocio(0, "X1", null, null, null, null))).isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("NG-05: un negocio cerrado no se edita; NG-14: uno suspendido tampoco")
    void actualizarNegocio_cerradoOSuspendido() {
        var cerrado = Escenario.nuevo(altas);
        var suspendido = Escenario.nuevo(altas);
        ejecutor.enNegocio(cerrado.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'CERRADO', "
                + "cerrado_en = now() WHERE id = :id").param("id", cerrado.negocioId).update());
        ejecutor.enNegocio(suspendido.negocioId, () -> jdbc.sql("UPDATE negocios.negocio SET estado = 'SUSPENDIDO', "
                + "suspendido_por = 'DUENO' WHERE id = :id").param("id", suspendido.negocioId).update());

        var alCerrado = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(
                cerrado.contexto, cerrado.dueno, new ActualizarNegocio(0, "Nuevo", null, null, null, null)));
        var alSuspendido = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarNegocio(
                suspendido.contexto, suspendido.dueno, new ActualizarNegocio(0, "Nuevo", null, null, null, null)));

        assertThat(codigo(alCerrado)).isEqualTo(CodigoError.NEGOCIO_CERRADO);
        assertThat(codigo(alSuspendido)).isEqualTo(CodigoError.NEGOCIO_SUSPENDIDO);
        assertThat(negocios.miNegocio(suspendido.contexto, suspendido.dueno).estado()).isEqualTo("SUSPENDIDO");
    }

    @Test
    @DisplayName("SN-05: el historial guarda los nombres de los campos cambiados, nunca sus valores")
    void historial_guardaNombresSinValores() {
        var e = Escenario.nuevo(altas);
        negocios.actualizarNegocio(e.contexto, e.dueno,
                new ActualizarNegocio(0, null, null, "2225550101", null, null));

        var filas = ejecutor.enNegocio(e.negocioId, () -> jdbc.sql("SELECT accion, campos::text AS campos, actor "
                + "FROM negocios.historial_negocio WHERE negocio_id = :id").param("id", e.negocioId)
                .query((r, n) -> r.getString("accion") + "|" + r.getString("campos") + "|" + r.getString("actor")).list());

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0)).startsWith("ACTUALIZAR_NEGOCIO|{telefono_contacto}|dueno-").doesNotContain("5550101");
    }

    @Test
    @DisplayName("el historial solo admite inserciones: ni siquiera la aplicacion lo modifica")
    void historial_soloInserciones() {
        var e = Escenario.nuevo(altas);
        negocios.actualizarNegocio(e.contexto, e.dueno, new ActualizarNegocio(0, "Cambio X", null, null, null, null));

        var intento = org.assertj.core.api.Assertions.catchThrowable(() -> ejecutor.enNegocio(e.negocioId,
                () -> jdbc.sql("DELETE FROM negocios.historial_negocio WHERE negocio_id = :id")
                        .param("id", e.negocioId).update()));

        assertThat(intento).isNotNull();
    }

    // ---- sucursal ----

    @Test
    @DisplayName("sucursales devuelve la principal; actualizarSucursal guarda direccion y coordenadas y publica el evento")
    void actualizarSucursal_guardaLaUbicacion() {
        var e = Escenario.nuevo(altas);
        var principal = negocios.sucursales(e.contexto, e.dueno).get(0);

        var vista = negocios.actualizarSucursal(e.contexto, e.dueno, principal.id(), new ActualizarSucursal(0, null,
                "5 de Mayo", "312", "Centro", "Puebla", "Puebla", "72000", "Frente al mercado, local 12",
                new BigDecimal("19.043300"), new BigDecimal("-98.198200"), "222 555 0102"));

        assertThat(vista.calle()).isEqualTo("5 de Mayo");
        assertThat(vista.codigoPostal()).isEqualTo("72000");
        assertThat(vista.latitud()).isEqualByComparingTo("19.0433");
        assertThat(vista.longitud()).isEqualByComparingTo("-98.1982");
        assertThat(vista.telefono()).isEqualTo("+522225550102");
        assertThat(vista.version()).isEqualTo(1);
        assertThat(negocios.miNegocio(e.contexto, e.dueno).completitud().faltantes()).doesNotContain("UBICACION");
        var sobre = evento("negocio.sucursal_cambiada", e.negocioId);
        assertThat(sobre.datos()).containsEntry("sucursal_id", principal.id().toString())
                .containsEntry("es_principal", true).containsEntry("municipio", "Puebla")
                .containsEntry("latitud", "19.0433").containsEntry("longitud", "-98.1982");
        assertThat(sobre.datos().toString()).doesNotContain("5 de Mayo").doesNotContain("0102");
    }

    @Test
    @DisplayName("SU-03: las coordenadas van juntas y dentro de su rango")
    void actualizarSucursal_coordenadasInvalidas() {
        var e = Escenario.nuevo(altas);
        var id = negocios.sucursales(e.contexto, e.dueno).get(0).id();

        var soloLatitud = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarSucursal(e.contexto,
                e.dueno, id, new ActualizarSucursal(0, null, null, null, null, null, null, null, null,
                        new BigDecimal("19.04"), null, null)));
        var fueraDeRango = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarSucursal(e.contexto,
                e.dueno, id, new ActualizarSucursal(0, null, null, null, null, null, null, null, null,
                        new BigDecimal("91"), new BigDecimal("-98"), null)));
        var longitudFuera = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarSucursal(e.contexto,
                e.dueno, id, new ActualizarSucursal(0, null, null, null, null, null, null, null, null,
                        new BigDecimal("19"), new BigDecimal("181"), null)));

        assertThat(codigo(soloLatitud)).isEqualTo(CodigoError.COORDENADAS_INVALIDAS);
        assertThat(codigo(fueraDeRango)).isEqualTo(CodigoError.COORDENADAS_INVALIDAS);
        assertThat(codigo(longitudFuera)).isEqualTo(CodigoError.COORDENADAS_INVALIDAS);
    }

    @Test
    @DisplayName("SU-04: el codigo postal lleva cinco digitos")
    void actualizarSucursal_codigoPostalInvalido() {
        var e = Escenario.nuevo(altas);
        var id = negocios.sucursales(e.contexto, e.dueno).get(0).id();

        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarSucursal(e.contexto,
                e.dueno, id, new ActualizarSucursal(0, null, null, null, null, null, null, "7200", null, null, null,
                        null)));

        assertThat(codigo(error)).isEqualTo(CodigoError.CODIGO_POSTAL_INVALIDO);
    }

    @Test
    @DisplayName("una sucursal de otro negocio responde como si no existiera; la version vieja da CONCURRENCIA")
    void actualizarSucursal_ajenaOVieja() {
        var a = Escenario.nuevo(altas);
        var b = Escenario.nuevo(altas);
        var deB = negocios.sucursales(b.contexto, b.dueno).get(0).id();
        var deA = negocios.sucursales(a.contexto, a.dueno).get(0).id();

        var ajena = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarSucursal(a.contexto,
                a.dueno, deB, new ActualizarSucursal(0, "X", null, null, null, null, null, null, null, null, null,
                        null)));
        negocios.actualizarSucursal(a.contexto, a.dueno, deA, new ActualizarSucursal(0, "Matriz", null, null, null,
                null, null, null, null, null, null, null));
        var vieja = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.actualizarSucursal(a.contexto,
                a.dueno, deA, new ActualizarSucursal(0, "Otra", null, null, null, null, null, null, null, null, null,
                        null)));

        assertThat(codigo(ajena)).isEqualTo(CodigoError.SUCURSAL_NO_ENCONTRADA);
        assertThat(codigo(vieja)).isEqualTo(CodigoError.CONCURRENCIA);
    }

    @Test
    @DisplayName("SG-04: editar la ubicacion pide negocios.sucursal.gestionar")
    void actualizarSucursal_sinPermiso() {
        var e = Escenario.nuevo(altas);
        var id = negocios.sucursales(e.contexto, e.dueno).get(0).id();

        assertThatThrownBy(() -> negocios.actualizarSucursal(e.contexto, e.con("negocios.perfil.editar"), id,
                new ActualizarSucursal(0, "X1", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(PermisoDenegadoException.class);
    }
}
