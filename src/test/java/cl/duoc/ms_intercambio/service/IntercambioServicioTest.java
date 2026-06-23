package cl.duoc.ms_intercambio.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.ms_intercambio.client.UsuarioFeignClient;
import cl.duoc.ms_intercambio.dto.EnviarOfertaDto;
import cl.duoc.ms_intercambio.dto.OfertaRespuestaDto;
import cl.duoc.ms_intercambio.model.EstadoOferta;
import cl.duoc.ms_intercambio.model.Oferta;
import cl.duoc.ms_intercambio.repository.OfertaRepositorio;

// NOTA: completarIntercambio depende internamente de UsuarioFeignClient
// para notificar a ms-usuarios, por lo que queda fuera del alcance de
// estas pruebas unitarias, por decision del equipo.
@ExtendWith(MockitoExtension.class)
public class IntercambioServicioTest {

    @Mock
    private OfertaRepositorio ofertaRepositorio;

    @Mock
    private UsuarioFeignClient usuarioFeignClient;

    @InjectMocks
    private IntercambioServicio intercambioServicio;

    private Oferta ofertaEjemplo;

    @BeforeEach
    void setUp(){
        ofertaEjemplo = new Oferta();
        ofertaEjemplo.setId(1);
        ofertaEjemplo.setEmisorId(3);
        ofertaEjemplo.setReceptorId(7);
        ofertaEjemplo.setOfrecido("Charizard ex");
        ofertaEjemplo.setSolicitado("Blastoise ex");
        ofertaEjemplo.setEstado(EstadoOferta.PENDIENTE);
        ofertaEjemplo.setFechaCreacion(LocalDateTime.now());
    }

    // =====================================================================
    // enviarOferta
    // =====================================================================

    @Test
    void enviarOferta_exitoso(){
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(7);
        dto.setOfrecido("Charizard ex");
        dto.setSolicitado("Blastoise ex");

        when(ofertaRepositorio.save(any(Oferta.class))).thenReturn(ofertaEjemplo);

        OfertaRespuestaDto resultado = intercambioServicio.enviarOferta(dto, 3, "Pedro");

        assertEquals(EstadoOferta.PENDIENTE, resultado.getEstado());
        assertEquals("Pedro", resultado.getNombreEmisor());
    }

    @Test
    void enviarOferta_aSiMismo_lanzaExcepcion(){
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(3);
        dto.setOfrecido("Charizard ex");
        dto.setSolicitado("Blastoise ex");

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.enviarOferta(dto, 3, "Pedro"));

        assertEquals("No puedes enviarte una oferta de intercambio a ti mismo.", error.getMessage());
    }

    @Test
    void enviarOferta_sinOfrecido_lanzaExcepcion(){
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(7);
        dto.setOfrecido("");
        dto.setSolicitado("Blastoise ex");

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.enviarOferta(dto, 3, "Pedro"));

        assertEquals("Debes describir que cartas estas ofreciendo.", error.getMessage());
    }

    @Test
    void enviarOferta_sinSolicitado_lanzaExcepcion(){
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(7);
        dto.setOfrecido("Charizard ex");
        dto.setSolicitado("");

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.enviarOferta(dto, 3, "Pedro"));

        assertEquals("Debes describir que cartas estas solicitando a cambio.", error.getMessage());
    }

    // =====================================================================
    // misOfertasRecibidas
    // =====================================================================

    @Test
    void misOfertasRecibidas_retornaLista(){
        when(ofertaRepositorio.findByReceptorIdAndEstado(7, EstadoOferta.PENDIENTE))
                .thenReturn(Arrays.asList(ofertaEjemplo));

        List<OfertaRespuestaDto> resultado = intercambioServicio.misOfertasRecibidas(7, "Ana");

        assertEquals(1, resultado.size());
        assertEquals("Ana", resultado.get(0).getNombreReceptor());
    }

    // =====================================================================
    // misOfertasEnviadas
    // =====================================================================

    @Test
    void misOfertasEnviadas_retornaLista(){
        when(ofertaRepositorio.findByEmisorId(3)).thenReturn(Arrays.asList(ofertaEjemplo));

        List<OfertaRespuestaDto> resultado = intercambioServicio.misOfertasEnviadas(3, "Pedro");

        assertEquals(1, resultado.size());
        assertEquals("Pedro", resultado.get(0).getNombreEmisor());
    }

    // =====================================================================
    // responderOferta
    // =====================================================================

    @Test
    void responderOferta_aceptar_exitoso(){
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));
        when(ofertaRepositorio.save(any(Oferta.class))).thenReturn(ofertaEjemplo);

        OfertaRespuestaDto resultado = intercambioServicio.responderOferta(1, true, 7, "Ana");

        assertEquals(EstadoOferta.ACEPTADA, ofertaEjemplo.getEstado());
        assertEquals("Ana", resultado.getNombreReceptor());
    }

    @Test
    void responderOferta_rechazar_exitoso(){
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));
        when(ofertaRepositorio.save(any(Oferta.class))).thenReturn(ofertaEjemplo);

        intercambioServicio.responderOferta(1, false, 7, "Ana");

        assertEquals(EstadoOferta.RECHAZADA, ofertaEjemplo.getEstado());
    }

    @Test
    void responderOferta_noEncontrada(){
        when(ofertaRepositorio.findByIdConBloqueo(99)).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.responderOferta(99, true, 7, "Ana"));

        assertEquals("Oferta no encontrada con id: 99", error.getMessage());
    }

    @Test
    void responderOferta_noEsElReceptor(){
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.responderOferta(1, true, 999, "Otro"));

        assertEquals("No puedes responder una oferta que no te enviaron a ti.", error.getMessage());
    }

    @Test
    void responderOferta_yaRespondida(){
        ofertaEjemplo.setEstado(EstadoOferta.ACEPTADA);
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.responderOferta(1, true, 7, "Ana"));

        assertEquals("Esta oferta ya fue respondida. Estado actual: ACEPTADA", error.getMessage());
    }

    // =====================================================================
    // cancelarOferta
    // =====================================================================

    @Test
    void cancelarOferta_exitoso(){
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));
        when(ofertaRepositorio.save(any(Oferta.class))).thenReturn(ofertaEjemplo);

        intercambioServicio.cancelarOferta(1, 3, "Pedro");

        assertEquals(EstadoOferta.CANCELADA, ofertaEjemplo.getEstado());
    }

    @Test
    void cancelarOferta_noEncontrada(){
        when(ofertaRepositorio.findByIdConBloqueo(99)).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.cancelarOferta(99, 3, "Pedro"));

        assertEquals("Oferta no encontrada con id: 99", error.getMessage());
    }

    @Test
    void cancelarOferta_noEsElEmisor(){
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.cancelarOferta(1, 999, "Otro"));

        assertEquals("No puedes cancelar una oferta que no enviaste tu.", error.getMessage());
    }

    @Test
    void cancelarOferta_noEstaPendiente(){
        ofertaEjemplo.setEstado(EstadoOferta.ACEPTADA);
        when(ofertaRepositorio.findByIdConBloqueo(1)).thenReturn(Optional.of(ofertaEjemplo));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                intercambioServicio.cancelarOferta(1, 3, "Pedro"));

        assertEquals("Solo puedes cancelar ofertas que esten PENDIENTES. Estado actual: ACEPTADA", error.getMessage());
    }
}
