package cl.duoc.ms_intercambio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * DTO de PETICION: datos para enviar una oferta de intercambio.
 *
 * El emisorId NO viene aqui, viene del token JWT.
 * Nunca confiamos en que el cliente nos diga quien es el emisor.
 *
 * Ejemplo de body JSON:
 * {
 *   "receptorId": 7,
 *   "ofrecido": "Charizard ex full art (NM) + Pikachu V promo",
 *   "solicitado": "Blastoise ex holo en buen estado",
 *   "mensaje": "Hola! Vi tu coleccion en el torneo del sabado, te propongo este cambio :)"
 * }
 */

@Data
public class EnviarOfertaDto {

    @NotNull(message = "El receptor de la oferta es obligatorio")
    private Integer receptorId;

    @NotBlank(message = "Debes indicar que estas solicitando")
    private String solicitado;

    @NotBlank(message = "Debes indicar que estas ofreciendo")
    private String ofrecido;

    private String mensaje;
}
