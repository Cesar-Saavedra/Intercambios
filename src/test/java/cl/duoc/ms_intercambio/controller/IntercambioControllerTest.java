package cl.duoc.ms_intercambio.controller;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.ms_intercambio.dto.EnviarOfertaDto;
import cl.duoc.ms_intercambio.dto.OfertaRespuestaDto;
import cl.duoc.ms_intercambio.model.EstadoOferta;
import cl.duoc.ms_intercambio.security.JwtUtil;
import cl.duoc.ms_intercambio.service.IntercambioServicio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@WebMvcTest(IntercambioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class IntercambioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntercambioServicio intercambioServicio;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OfertaRespuestaDto ofertaEjemplo;

    @BeforeEach
    void setUp(){
        ofertaEjemplo = new OfertaRespuestaDto(1, 3, "Pedro", 7, "Jugador #7",
                "Charizard ex", "Blastoise ex", "Hola", EstadoOferta.PENDIENTE,
                LocalDateTime.now(), null);
    }

    // =====================================================================
    // POST /api/intercambios
    // =====================================================================

    @Test
    void enviarOferta_sinToken_retorna401() throws Exception {
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(7);
        dto.setOfrecido("Charizard ex");
        dto.setSolicitado("Blastoise ex");

        mockMvc.perform(post("/api/intercambios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enviarOferta_exitoso_retorna201() throws Exception {
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(7);
        dto.setOfrecido("Charizard ex");
        dto.setSolicitado("Blastoise ex");

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(3);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Pedro");
        when(intercambioServicio.enviarOferta(any(EnviarOfertaDto.class), eq(3), eq("Pedro")))
                .thenReturn(ofertaEjemplo);

        mockMvc.perform(post("/api/intercambios")
                        .header("Authorization", "Bearer token-bueno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void enviarOferta_aSiMismo_retorna400() throws Exception {
        EnviarOfertaDto dto = new EnviarOfertaDto();
        dto.setReceptorId(3);
        dto.setOfrecido("Charizard ex");
        dto.setSolicitado("Blastoise ex");

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(3);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Pedro");
        when(intercambioServicio.enviarOferta(any(EnviarOfertaDto.class), eq(3), eq("Pedro")))
                .thenThrow(new RuntimeException("No puedes enviarte una oferta de intercambio a ti mismo."));

        mockMvc.perform(post("/api/intercambios")
                        .header("Authorization", "Bearer token-bueno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // GET /api/intercambios/recibidas
    // =====================================================================

    @Test
    void misOfertasRecibidas_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/intercambios/recibidas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void misOfertasRecibidas_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(7);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Ana");
        when(intercambioServicio.misOfertasRecibidas(7, "Ana")).thenReturn(Arrays.asList(ofertaEjemplo));

        mockMvc.perform(get("/api/intercambios/recibidas").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // =====================================================================
    // GET /api/intercambios/enviadas
    // =====================================================================

    @Test
    void misOfertasEnviadas_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/intercambios/enviadas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void misOfertasEnviadas_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(3);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Pedro");
        when(intercambioServicio.misOfertasEnviadas(3, "Pedro")).thenReturn(Arrays.asList(ofertaEjemplo));

        mockMvc.perform(get("/api/intercambios/enviadas").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // =====================================================================
    // PUT /api/intercambios/{id}/responder
    // =====================================================================

    @Test
    void responderOferta_sinToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/intercambios/1/responder").param("acepta", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responderOferta_exitoso_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(7);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Ana");
        when(intercambioServicio.responderOferta(1, true, 7, "Ana")).thenReturn(ofertaEjemplo);

        mockMvc.perform(put("/api/intercambios/1/responder")
                        .param("acepta", "true")
                        .header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk());
    }

    @Test
    void responderOferta_yaRespondida_retorna400() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(7);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Ana");
        when(intercambioServicio.responderOferta(1, true, 7, "Ana"))
                .thenThrow(new RuntimeException("Esta oferta ya fue respondida. Estado actual: ACEPTADA"));

        mockMvc.perform(put("/api/intercambios/1/responder")
                        .param("acepta", "true")
                        .header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // PUT /api/intercambios/{id}/cancelar
    // =====================================================================

    @Test
    void cancelarOferta_sinToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/intercambios/1/cancelar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelarOferta_exitoso_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(3);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Pedro");
        when(intercambioServicio.cancelarOferta(1, 3, "Pedro")).thenReturn(ofertaEjemplo);

        mockMvc.perform(put("/api/intercambios/1/cancelar").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk());
    }

    // =====================================================================
    // PUT /api/intercambios/{id}/completar
    // =====================================================================

    @Test
    void completarIntercambio_sinToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/intercambios/1/completar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completarIntercambio_exitoso_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(3);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Pedro");
        when(intercambioServicio.completarIntercambio(eq(1), eq(3), eq("Pedro"), anyString()))
                .thenReturn(ofertaEjemplo);

        mockMvc.perform(put("/api/intercambios/1/completar").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk());
    }
}
