package mx.negocio360.clientes.dominio;

/**
 * Finalidad declarada en el aviso de privacidad (art. 15 III). Las que requieren consentimiento se distinguen de las
 * que no (art. 8 y 11). La validez del codigo la comprueba {@link AvisoPrivacidad}.
 */
public record FinalidadAviso(String codigo, boolean requiereConsentimiento) {

    public FinalidadAviso {
        codigo = codigo == null ? null : codigo.strip();
    }
}
