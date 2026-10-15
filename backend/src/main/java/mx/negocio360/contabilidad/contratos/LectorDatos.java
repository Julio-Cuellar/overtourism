package mx.negocio360.contabilidad.contratos;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * Lee los campos de {@code datos} de un evento como lector tolerante: ignora lo que no usa (R5) y falla con
 * {@link DatosInvalidosException} solo si un campo que necesita falta o no tiene forma valida.
 */
final class LectorDatos {

    private final Map<String, Object> datos;

    LectorDatos(Map<String, Object> datos) {
        this.datos = datos == null ? Map.of() : datos;
    }

    UUID uuid(String campo) {
        var uuid = uuidOpcional(campo);
        if (uuid == null) {
            throw new DatosInvalidosException(campo);
        }
        return uuid;
    }

    UUID uuidOpcional(String campo) {
        var valor = datos.get(campo);
        if (valor == null) {
            return null;
        }
        try {
            return UUID.fromString(valor.toString());
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException(campo);
        }
    }

    BigDecimal dinero(String campo) {
        var monto = dineroOpcional(campo);
        if (monto == null) {
            throw new DatosInvalidosException(campo);
        }
        return monto;
    }

    BigDecimal dineroOpcional(String campo) {
        var valor = datos.get(campo);
        if (valor == null) {
            return null;
        }
        if (!(valor instanceof String texto)) {
            throw new DatosInvalidosException(campo);
        }
        return DecimalPlano.leer(texto).orElseThrow(() -> new DatosInvalidosException(campo));
    }

    String texto(String campo) {
        var valor = datos.get(campo);
        return valor == null ? null : valor.toString();
    }

    LectorDatos objeto(String campo) {
        var valor = datos.get(campo);
        if (valor == null) {
            return new LectorDatos(Map.of());
        }
        if (!(valor instanceof Map<?, ?> mapa)) {
            throw new DatosInvalidosException(campo);
        }
        @SuppressWarnings("unchecked")
        var anidado = (Map<String, Object>) mapa;
        return new LectorDatos(anidado);
    }
}
