package mx.negocio360.caja.fakes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.ConteoDeVentas;
import mx.negocio360.caja.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.caja.aplicacion.puertos.ReglasCaja;
import mx.negocio360.caja.aplicacion.puertos.ResumenVentas;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;

/** Dobles pequeños de los puertos de Caja que no son tablas. */
public final class Dobles {

    private Dobles() {
    }

    /** Eventos publicados por Caja, en orden. */
    public static final class Eventos implements PublicadorEventos {
        private final List<SobreEvento> publicados = new ArrayList<>();

        @Override
        public void publicar(SobreEvento evento) {
            publicados.add(evento);
        }

        public List<SobreEvento> deTipo(String tipo) {
            return publicados.stream().filter(e -> e.tipo().equals(tipo)).toList();
        }

        public List<SobreEvento> todos() {
            return List.copyOf(publicados);
        }
    }

    /** Lo que Ventas reporta de cada turno; la prueba lo fija. Si no se fijo, Ventas no tiene nada de ese turno. */
    public static final class Ventas implements ResumenVentas {
        private final Map<UUID, ConteoDeVentas> porTurno = new HashMap<>();

        public void fijar(UUID turnoId, int registradas, int anuladas, long preparacionesVigentes) {
            porTurno.put(turnoId, new ConteoDeVentas(registradas, anuladas, preparacionesVigentes));
        }

        @Override
        public ConteoDeVentas deTurno(ContextoNegocio contexto, UUID turnoId) {
            return porTurno.getOrDefault(turnoId, new ConteoDeVentas(0, 0, 0));
        }
    }

    /** Umbral de diferencia por negocio; 50.00 por omision (valor inicial propuesto en CJ-08). */
    public static final class Reglas implements ReglasCaja {
        public BigDecimal umbral = new BigDecimal("50.00");

        @Override
        public BigDecimal umbralDiferencia(UUID negocioId) {
            return umbral;
        }
    }
}
