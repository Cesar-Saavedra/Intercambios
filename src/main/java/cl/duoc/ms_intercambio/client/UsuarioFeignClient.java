package cl.duoc.ms_intercambio.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/*
 * Cliente Feign para comunicarse con ms-usuarios.
 *
 * name = "ms-usuarios" → Feign resuelve el host via Eureka (lb://ms-usuarios),
 * sin URLs hardcodeadas en el yaml.
 *
 * ms-intercambios llama a ms-usuarios para notificar que un jugador
 * completo un intercambio (ms-usuarios actualiza el contador del perfil).
 */
@FeignClient(name = "ms-usuarios")
public interface UsuarioFeignClient {

    /*
     * PUT /api/perfil/sumar-intercambio/{usuarioId}
     * Incrementa el contador de intercambios del perfil del jugador.
     * Se llama cuando un intercambio queda en estado COMPLETADO.
     *
     * Nota: este endpoint debe existir en ms-usuarios (PerfilController).
     */
    @PutMapping("/api/perfil/sumar-intercambio/{usuarioId}")
    void sumarIntercambio(
            @PathVariable("usuarioId") Integer usuarioId,
            @RequestHeader("Authorization") String authHeader
    );
}
