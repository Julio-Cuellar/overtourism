package mx.negocio360.caja.seguridad;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.springframework.stereotype.Component;

/**
 * Permisos que declara Caja (AZ-02, D-70, D-127). Ninguno es sensible por ahora (ver {@code PermisosDeVentas}): retirar
 * efectivo y autorizar una diferencia (CJ-08) son permisos aparte que solo traen el gerente, el administrador y el
 * dueno. Quien tiene cada permiso lo deciden los roles de Identidad, que son datos.
 */
@Component
public final class PermisosDeCaja implements CatalogoDePermisos {

    /** Crear cajas (CJ-01). */
    public static final String CAJA_GESTIONAR = "caja.caja.gestionar";
    /** Ver cajas, turnos y su saldo. */
    public static final String TURNO_VER = "caja.turno.ver";
    /** Abrir turno con fondo (CJ-02). */
    public static final String TURNO_ABRIR = "caja.turno.abrir";
    /** Meter efectivo (CJ-05). */
    public static final String MOVIMIENTO_REGISTRAR = "caja.movimiento.registrar";
    /** Sacar efectivo (CJ-10). */
    public static final String RETIRO_REGISTRAR = "caja.retiro.registrar";
    /** Preparar, descartar y confirmar el corte (CJ-06). */
    public static final String CORTE_CERRAR = "caja.corte.cerrar";
    /** Autorizar una diferencia sobre el umbral (CJ-08). */
    public static final String CORTE_AUTORIZAR = "caja.corte.autorizar";
    /** Auditar cortes pasados. */
    public static final String CORTE_VER = "caja.corte.ver";

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado(CAJA_GESTIONAR, "Crear cajas", false),
            new PermisoDeclarado(TURNO_VER, "Ver las cajas, los turnos y su saldo", false),
            new PermisoDeclarado(TURNO_ABRIR, "Abrir un turno con su fondo", false),
            new PermisoDeclarado(MOVIMIENTO_REGISTRAR, "Registrar una entrada de efectivo", false),
            new PermisoDeclarado(RETIRO_REGISTRAR, "Registrar un retiro de efectivo", false),
            new PermisoDeclarado(CORTE_CERRAR, "Preparar, descartar y confirmar el corte de caja", false),
            new PermisoDeclarado(CORTE_AUTORIZAR, "Autorizar una diferencia de caja sobre el umbral", false),
            new PermisoDeclarado(CORTE_VER, "Ver los cortes pasados", false));

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
