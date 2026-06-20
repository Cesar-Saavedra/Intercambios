package cl.duoc.ms_intercambio.config;

import org.springframework.context.annotation.Configuration;

/*
 * Configuracion general de ms-intercambios.
 * El bean RestTemplate fue eliminado: la comunicacion con ms-usuarios
 * ahora se hace via Feign (UsuarioFeignClient), que usa Eureka para
 * resolver el host sin URLs hardcodeadas.
 */
@Configuration
public class AppConfig {
    // Vacio: Feign no necesita un bean RestTemplate manual.
}
