package cl.duoc.ms_intercambio.dto;

import java.time.LocalDateTime;

import cl.duoc.ms_intercambio.model.EstadoOferta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfertaRespuestaDto {

    private Integer id;
    private Integer emisorId;
    private String nombreEmisor;    // nombre del emisor (viene del token)
    private Integer receptorId;
    private String nombreReceptor;  // nombre del receptor (solo si esta disponible)
    private String ofrecido;
    private String solicitado;
    private String mensaje;
    private EstadoOferta  estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRespuesta;  // null mientras este PENDIENTE


}
