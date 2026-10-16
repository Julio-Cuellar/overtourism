package mx.negocio360.negocios.aplicacion;

import java.util.Objects;
import java.util.UUID;
import mx.negocio360.negocios.AdministracionApi;
import mx.negocio360.negocios.ResenaVista;

/** La moderacion de la plataforma (R6): delega en las reglas de reseñas, que son las mismas que usa el panel. */
public final class AdministracionServicio implements AdministracionApi {

    private final ResenasServicio resenas;

    public AdministracionServicio(ResenasServicio resenas) {
        this.resenas = Objects.requireNonNull(resenas, "resenas");
    }

    @Override
    public ResenaVista ocultarResena(UUID negocioId, UUID resenaId, String motivo, String operador) {
        return resenas.ocultarResena(negocioId, resenaId, motivo, operador);
    }

    @Override
    public ResenaVista restituirResena(UUID negocioId, UUID resenaId, String operador) {
        return resenas.restituirResena(negocioId, resenaId, operador);
    }
}
