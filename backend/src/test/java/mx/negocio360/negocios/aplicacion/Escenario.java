package mx.negocio360.negocios.aplicacion;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;

/** Un negocio dado de alta de verdad (por el servicio de altas) y quien lo opera, para las pruebas con Postgres. */
final class Escenario {

    static final Set<String> TODOS_LOS_PERMISOS = Set.of("negocios.perfil.ver", "negocios.perfil.editar",
            "negocios.sucursal.gestionar", "negocios.horario.editar", "negocios.negocio.publicar",
            "negocios.disponibilidad.editar", "negocios.negocio.pausar", "negocios.negocio.cerrar", "negocios.resena.ver",
            "negocios.resena.responder");

    final UUID negocioId;
    final UUID cuentaId;
    final ContextoNegocio contexto;
    final Autorizacion dueno = Autorizacion.de(TODOS_LOS_PERMISOS);
    final Autorizacion nadie = Autorizacion.de(Set.of());

    private Escenario(NegocioRegistroSolicitadoV1 pedido) {
        this.negocioId = pedido.negocioId();
        this.cuentaId = pedido.cuentaId();
        this.contexto = new ContextoNegocio(negocioId, "dueno-" + cuentaId);
    }

    /** Da de alta un negocio nuevo y devuelve su escenario. */
    static Escenario nuevo(AltasApi altas, String nombre, String giro) {
        var pedido = Datos.pedido(Datos.id(), Datos.id(), nombre, giro);
        altas.registrarAlta(pedido);
        return new Escenario(pedido);
    }

    static Escenario nuevo(AltasApi altas) {
        return nuevo(altas, "Fonda Doña Chela", "RESTAURANTE");
    }

    Autorizacion con(String... permisos) {
        return Autorizacion.de(Set.of(permisos));
    }
}
