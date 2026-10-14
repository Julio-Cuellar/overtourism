package mx.negocio360.clientes.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;

/** Doble en memoria: version unica por negocio y, al publicar, cierra la version vigente anterior. */
public final class RepositorioAvisosEnMemoria implements RepositorioAvisos {

    private final List<AvisoPrivacidad> avisos = new ArrayList<>();

    @Override
    public Optional<AvisoPrivacidad> vigente(UUID negocioId) {
        return avisos.stream().filter(a -> a.negocioId().equals(negocioId)).filter(AvisoPrivacidad::vigente)
                .findFirst();
    }

    @Override
    public int ultimaVersion(UUID negocioId) {
        return avisos.stream().filter(a -> a.negocioId().equals(negocioId)).mapToInt(AvisoPrivacidad::version)
                .max().orElse(0);
    }

    @Override
    public void publicar(AvisoPrivacidad nuevo) {
        var repetida = avisos.stream()
                .anyMatch(a -> a.negocioId().equals(nuevo.negocioId()) && a.version() == nuevo.version());
        if (repetida) {
            throw new ClientesException(CodigoError.CONCURRENCIA);
        }
        avisos.replaceAll(a -> a.negocioId().equals(nuevo.negocioId()) && a.vigente()
                ? a.cerradoEn(nuevo.vigenteDesde()) : a);
        avisos.add(nuevo);
    }

    /** Todas las versiones del negocio, de la mas antigua a la mas nueva. */
    public List<AvisoPrivacidad> historial(UUID negocioId) {
        return avisos.stream().filter(a -> a.negocioId().equals(negocioId))
                .sorted((a, b) -> Integer.compare(a.version(), b.version())).toList();
    }
}
