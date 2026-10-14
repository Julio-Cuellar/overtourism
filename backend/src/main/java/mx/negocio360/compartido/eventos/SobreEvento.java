package mx.negocio360.compartido.eventos;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Sobre comun de todos los eventos (R3 y R5). Los datos llevan hechos con ids y valores, nunca datos personales.
 *
 * <p>Reglas de {@code datos}: solo cadenas, enteros, booleanos, listas y mapas, copiados de forma profunda e
 * inmutable. El dinero y las cantidades van como cadena decimal; un {@code Double}, {@code Float} o
 * {@code BigDecimal} se rechaza, tambien anidado. Un campo opcional que no aplica se omite: no se escribe en null.
 *
 * @param id             UUIDv7 (variante RFC 4122) unico por evento
 * @param tipo           {@code entidad.hecho_en_pasado}, por ejemplo {@code cliente.registrado}
 * @param version        version del contrato de ese tipo, desde 1
 * @param negocioId      negocio al que pertenece el hecho
 * @param origen         modulo que publica, por ejemplo {@code clientes}
 * @param ocurridoEn     momento del hecho, en UTC
 * @param correlacionId  hilo de negocio; en una venta, el venta_id
 * @param causaId        evento que lo provoco, o nulo
 * @param datos          carga del contrato
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SobreEvento(UUID id, String tipo, int version, UUID negocioId, String origen, Instant ocurridoEn,
                          UUID correlacionId, UUID causaId, Map<String, Object> datos) {

    private static final Pattern TIPO_VALIDO = Pattern.compile("^[a-z]+\\.[a-z_]+$");
    private static final int VERSION_UUID_DE_EVENTOS = 7;
    private static final int VARIANTE_RFC_4122 = 2;

    public SobreEvento {
        Objects.requireNonNull(id, "id");
        if (id.version() != VERSION_UUID_DE_EVENTOS || id.variant() != VARIANTE_RFC_4122) {
            throw new IllegalArgumentException("El id del evento debe ser UUIDv7: " + id);
        }
        Objects.requireNonNull(tipo, "tipo");
        if (!TIPO_VALIDO.matcher(tipo).matches()) {
            throw new IllegalArgumentException("El tipo debe tener la forma entidad.hecho_en_pasado: " + tipo);
        }
        if (version < 1) {
            throw new IllegalArgumentException("La version debe ser mayor o igual a 1: " + version);
        }
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(origen, "origen");
        if (origen.isBlank()) {
            throw new IllegalArgumentException("El origen no puede estar vacio");
        }
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(correlacionId, "correlacionId");
        datos = copiarMapa(Objects.requireNonNull(datos, "datos"), "datos");
    }

    private static Map<String, Object> copiarMapa(Map<?, ?> mapa, String ruta) {
        var copia = new LinkedHashMap<String, Object>();
        for (var entrada : mapa.entrySet()) {
            if (!(entrada.getKey() instanceof String clave)) {
                throw new IllegalArgumentException("Las claves de " + ruta + " deben ser cadenas");
            }
            copia.put(clave, copiarValor(entrada.getValue(), ruta + "." + clave));
        }
        return Collections.unmodifiableMap(copia);
    }

    private static Object copiarValor(Object valor, String ruta) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor nulo en " + ruta + ": omita el campo en lugar de escribir null");
        }
        if (valor instanceof Float || valor instanceof Double || valor instanceof BigDecimal) {
            throw new IllegalArgumentException("Numero decimal en " + ruta + ": el dinero y las cantidades van como "
                    + "cadena decimal");
        }
        if (valor instanceof String || valor instanceof Boolean || valor instanceof Integer || valor instanceof Long
                || valor instanceof Short || valor instanceof Byte || valor instanceof BigInteger) {
            return valor;
        }
        if (valor instanceof Map<?, ?> mapa) {
            return copiarMapa(mapa, ruta);
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(elemento -> copiarValor(elemento, ruta + "[]")).toList();
        }
        throw new IllegalArgumentException("Tipo no permitido en " + ruta + ": " + valor.getClass().getName());
    }
}
