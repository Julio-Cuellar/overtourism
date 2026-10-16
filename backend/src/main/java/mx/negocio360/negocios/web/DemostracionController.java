package mx.negocio360.negocios.web;

import mx.negocio360.negocios.DemostracionApi;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los comandos de la demo del stand (DM-02, DM-04): sembrar los negocios simulados y restablecer lo que hicieron los
 * visitantes. SOLO existe con el perfil {@code demo}, nunca con {@code produccion}, y ademas exige el modo demostracion
 * encendido. No tiene autenticacion porque no deja de ser una herramienta del equipo en una maquina de la presentacion.
 */
@RestController
@Profile("demo & !produccion")
public class DemostracionController {

    private final DemostracionApi demostracion;

    public DemostracionController(DemostracionApi demostracion) {
        this.demostracion = demostracion;
    }

    @PostMapping("/demo/sembrar")
    public DemostracionApi.ResultadoSiembra sembrar() {
        return demostracion.sembrar();
    }

    @PostMapping("/demo/restablecer")
    public DemostracionApi.ResultadoRestablecimiento restablecer() {
        return demostracion.restablecer();
    }
}
