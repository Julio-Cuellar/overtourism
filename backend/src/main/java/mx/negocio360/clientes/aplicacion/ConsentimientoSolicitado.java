package mx.negocio360.clientes.aplicacion;

import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;

/** Consentimiento expreso que la persona da al registrarse, por una finalidad y por un medio. */
public record ConsentimientoSolicitado(Finalidad finalidad, MedioConsentimiento medio) {
}
