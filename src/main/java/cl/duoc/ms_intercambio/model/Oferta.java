package cl.duoc.ms_intercambio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "ofertas")
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Id del jugador que hace la oferta (viene de ms-login)
    @Column(nullable = false)
    private Integer emisorId;

    // Id del jugador que recibe la oferta (viene de ms-login)
    @Column(nullable = false)
    private Integer receptorId;

    // Descripcion de lo que ofrece el emisor (texto libre)
    // Ejemplo: "Charizard ex full art + Pikachu V promo"
    @Column(nullable = false, length = 500)
    private String ofrecido;

    // Descripcion de lo que pide el emisor a cambio (texto libre)
    // Ejemplo: "Blastoise ex holo en buen estado"
    @Column(nullable = false, length = 500)
    private String solicitado;

    // Mensaje opcional del emisor para el receptor
    // Ejemplo: "Hola! Vi tu coleccion en el torneo, te propongo este cambio"
    @Column(length = 300)
    private String mensaje;

    // Estado actual de la oferta en su ciclo de vida
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOferta estado = EstadoOferta.PENDIENTE;

    // Cuando fue creada la oferta
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    // Cuando fue respondida (aceptada, rechazada, cancelada o completada)
    // Null mientras este en estado PENDIENTE
    private LocalDateTime fechaRespuesta;

}
