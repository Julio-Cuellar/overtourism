package mx.negocio360.identidad.dominio;

/** Estados de una membresia (AM-01). REVOCADA es final: para volver hay que invitar de nuevo. */
public enum EstadoMembresia {
    ACTIVA, SUSPENDIDA, REVOCADA
}
