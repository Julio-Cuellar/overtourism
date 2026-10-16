package mx.negocio360.negocios.dominio;

/** Estado de un negocio (NG-04). {@code SUSPENDIDO} lleva siempre quien lo suspendio ({@link OrigenSuspension}). */
public enum EstadoNegocio { ACTIVO, SUSPENDIDO, CERRADO }
