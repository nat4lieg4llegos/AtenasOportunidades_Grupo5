package cl.duoc.atenas.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del microservicio de oportunidades de Agencia Atenas. */
@SpringBootApplication
public class AtenasApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtenasApiApplication.class, args);
    }
}
