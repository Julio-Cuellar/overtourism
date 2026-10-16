package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.dominio.EstadoMembresia;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.Membresia;
import mx.negocio360.identidad.dominio.SolicitudDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AM-01 con «invitar de nuevo»: una persona puede tener varias membresias en un negocio, pero a lo mas una no revocada.
 * Los repositorios en memoria imitan al indice unico parcial de V022. Tambien, las solicitudes de negocio.
 */
class RepositoriosEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final UUID CUENTA = UuidV7.generar(AHORA);
    private static final UUID NEGOCIO = UuidV7.generar(AHORA);

    private static Membresia nueva(String rol) {
        return Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, rol, null, AHORA);
    }

    @Test
    @DisplayName("porCuentaYNegocio devuelve la membresia no revocada aunque exista una revocada anterior")
    void porCuentaYNegocio_prefiereLaNoRevocada() {
        var membresias = new RepositorioMembresiasEnMemoria();
        var vieja = membresias.guardar(nueva("CAJERO").revocada(AHORA));
        var nueva = membresias.guardar(nueva("GERENTE"));

        assertThat(membresias.porCuentaYNegocio(CUENTA, NEGOCIO)).contains(nueva);
        assertThat(membresias.deCuenta(CUENTA)).containsExactlyInAnyOrder(vieja, nueva);
    }

    @Test
    @DisplayName("si solo hay revocadas devuelve la mas reciente")
    void porCuentaYNegocio_sinActivas_devuelveLaMasReciente() {
        var membresias = new RepositorioMembresiasEnMemoria();
        membresias.guardar(nueva("CAJERO").revocada(AHORA));
        var reciente = membresias.guardar(Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, "GERENTE", null,
                AHORA.plusSeconds(60)).revocada(AHORA.plusSeconds(120)));

        assertThat(membresias.porCuentaYNegocio(CUENTA, NEGOCIO)).contains(reciente);
        assertThat(reciente.estado()).isEqualTo(EstadoMembresia.REVOCADA);
    }

    @Test
    @DisplayName("no se guarda una segunda membresia no revocada de la misma cuenta en el mismo negocio")
    void guardar_dosNoRevocadas_seRechaza() {
        var membresias = new RepositorioMembresiasEnMemoria();
        membresias.guardar(nueva("CAJERO"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> membresias.guardar(nueva("GERENTE")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("actualizar la misma membresia no cuenta como una segunda")
    void guardar_laMismaMembresia_loActualiza() {
        var membresias = new RepositorioMembresiasEnMemoria();
        var original = membresias.guardar(nueva("CAJERO"));

        membresias.guardar(original.conRol("GERENTE", AHORA.plusSeconds(1)));

        assertThat(membresias.deNegocio(NEGOCIO)).singleElement().satisfies(m -> assertThat(m.rolCodigo()).isEqualTo("GERENTE"));
    }

    @Test
    @DisplayName("las solicitudes de una cuenta se listan por orden de pedido y no se mezclan con las de otra")
    void solicitudes_deCuenta_enOrdenYSinMezclar() {
        var solicitudes = new SolicitudesDeNegocioEnMemoria();
        var otraCuenta = UuidV7.generar(AHORA);
        var segunda = new SolicitudDeNegocio(UuidV7.generar(AHORA), CUENTA, "Museo", Giro.MUSEO, AHORA.plusSeconds(5));
        var primera = new SolicitudDeNegocio(UuidV7.generar(AHORA), CUENTA, "Fonda", Giro.RESTAURANTE, AHORA);
        solicitudes.guardar(segunda);
        solicitudes.guardar(primera);
        solicitudes.guardar(new SolicitudDeNegocio(UuidV7.generar(AHORA), otraCuenta, "Tienda", Giro.MISCELANEA, AHORA));

        assertThat(solicitudes.deCuenta(CUENTA)).containsExactly(primera, segunda);
        assertThat(solicitudes.deCuenta(UuidV7.generar(AHORA))).isEmpty();
    }

    @Test
    @DisplayName("las solicitudes de negocio se guardan, se leen y se eliminan")
    void solicitudes_guardarLeerEliminar() {
        var solicitudes = new SolicitudesDeNegocioEnMemoria();
        var solicitud = new SolicitudDeNegocio(NEGOCIO, CUENTA, "Fonda", Giro.RESTAURANTE, AHORA);

        solicitudes.guardar(solicitud);

        assertThat(solicitudes.porNegocio(NEGOCIO)).contains(solicitud);
        solicitudes.eliminar(NEGOCIO);
        assertThat(solicitudes.porNegocio(NEGOCIO)).isEmpty();
        solicitudes.eliminar(NEGOCIO);
    }
}
