package mx.negocio360.clientes.aplicacion;

import java.time.Clock;
import java.util.Objects;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * PublicarAvisoPrivacidad (L-01, L-02). Un aviso nuevo es la version siguiente y cierra la vigente; el contenido
 * publicado no se edita. La version se calcula al confirmar, no al preparar. No publica eventos.
 */
public final class ManejadorPublicarAviso implements ManejadorComando<PublicarAvisoComando> {

    public static final String TIPO = "publicar_aviso_privacidad";

    private final RepositorioAvisos avisos;
    private final Clock reloj;

    public ManejadorPublicarAviso(RepositorioAvisos avisos, Clock reloj) {
        this.avisos = Objects.requireNonNull(avisos, "avisos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<PublicarAvisoComando> claseComando() {
        return PublicarAvisoComando.class;
    }

    @Override
    public String preparar(ContextoNegocio contexto, PublicarAvisoComando comando) {
        var aviso = construir(contexto, comando);
        return "Se publicará la versión " + aviso.version() + " del aviso de privacidad. Reemplaza a la vigente y "
                + "su contenido ya no se podrá editar.";
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, PublicarAvisoComando comando) {
        var aviso = construir(contexto, comando);
        avisos.publicar(aviso);
        return ResultadoComando.ok(PlantillasClientes.AVISO_PUBLICADO, aviso.id());
    }

    private AvisoPrivacidad construir(ContextoNegocio contexto, PublicarAvisoComando comando) {
        var ahora = reloj.instant();
        return AvisoPrivacidad.nuevo(UuidV7.generar(ahora), contexto.negocioId(),
                avisos.ultimaVersion(contexto.negocioId()) + 1, comando.responsableNombre(),
                comando.responsableDomicilio(), comando.finalidades(), comando.textoSimplificado(),
                comando.textoIntegral(), comando.contactoArco(), contexto.actor(), ahora);
    }
}
