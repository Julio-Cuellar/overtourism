package mx.negocio360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del monolito modular. Cada subpaquete directo es un modulo de Spring Modulith. */
@SpringBootApplication
public class Negocio360Application {

    public static void main(String[] args) {
        SpringApplication.run(Negocio360Application.class, args);
    }
}
