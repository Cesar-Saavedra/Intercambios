package cl.duoc.ms_intercambio.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_intercambio.dto.EnviarOfertaDto;
import cl.duoc.ms_intercambio.dto.OfertaRespuestaDto;
import cl.duoc.ms_intercambio.security.JwtUtil;
import cl.duoc.ms_intercambio.service.IntercambioServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/*
 * Controlador REST de ms-intercambio.
 * Puerto: 8088
 * Base URL: http://localhost:8088/api/intercambios
 *
 * Arquitectura: Controller -> Service -> Repository (CSR)
 * Este controlador solo valida el token y delega al servicio.
 *
 * TODOS los endpoints requieren:
 *   Header: Authorization: Bearer {token}
 */
@RestController
@RequestMapping("/api/intercambios")
@Tag(name = "Intercambios", description = "Gestión de ofertas de intercambio de cartas entre jugadores")
public class IntercambioController {

    @Autowired
    private IntercambioServicio intercambioServicio;

    @Autowired JwtUtil jwtUtil;

    @PostMapping
    @Operation(summary = "Enviar oferta", description = "Envía una oferta de intercambio de cartas a otro jugador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Oferta creada con estado PENDIENTE", content = @Content(
                    examples = @ExampleObject(name = "OfertaCreada", value = """
                            {
                              "id": 1,
                              "emisorId": 5,
                              "receptorId": 7,
                              "ofrecido": "Charizard ex full art (NM) + Pikachu V promo",
                              "solicitado": "Blastoise ex holo en buen estado",
                              "mensaje": "Hola! Te propongo este cambio, avísame si te interesa",
                              "estado": "PENDIENTE"
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Falta algún campo obligatorio o el usuario se envía la oferta a sí mismo"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> enviarOferta(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos de la oferta de intercambio", required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "receptorId": 7,
                              "ofrecido": "Charizard ex full art (NM) + Pikachu V promo",
                              "solicitado": "Blastoise ex holo en buen estado",
                              "mensaje": "Hola! Te propongo este cambio, avísame si te interesa"
                            }
                            """)))
            @Valid @RequestBody EnviarOfertaDto dto) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para enviar una oferta.");
        }

        // Extraer datos del emisor desde el token (nunca del body)
        Integer emisorId    = jwtUtil.extraerId(token);
        String  nombreEmisor = jwtUtil.extraerNombre(token);

        try {
            OfertaRespuestaDto oferta = intercambioServicio.enviarOferta(dto, emisorId, nombreEmisor);
            return ResponseEntity.status(HttpStatus.CREATED).body(oferta);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    @GetMapping("/recibidas")
    @Operation(summary = "Ofertas recibidas", description = "Devuelve las ofertas pendientes que el usuario autenticado ha recibido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ofertas recibidas pendientes"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> misOfertasRecibidas(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        Integer receptorId = jwtUtil.extraerId(token);
        String  nombre     = jwtUtil.extraerNombre(token);

        try {
            List<OfertaRespuestaDto> ofertas = intercambioServicio.misOfertasRecibidas(receptorId, nombre);
            return ResponseEntity.ok(ofertas);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    @GetMapping("/enviadas")
    @Operation(summary = "Ofertas enviadas", description = "Historial de todas las ofertas enviadas por el usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de ofertas enviadas"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> misOfertasEnviadas(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        Integer emisorId = jwtUtil.extraerId(token);
        String  nombre   = jwtUtil.extraerNombre(token);

        try {
            List<OfertaRespuestaDto> ofertas = intercambioServicio.misOfertasEnviadas(emisorId, nombre);
            return ResponseEntity.ok(ofertas);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    @PutMapping("/{id}/responder")
    @Operation(summary = "Responder oferta", description = "Acepta o rechaza una oferta recibida. Query param: acepta=true|false.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta con el nuevo estado (ACEPTADA o RECHAZADA)"),
            @ApiResponse(responseCode = "400", description = "La oferta no está en estado PENDIENTE u otro error de negocio"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o el usuario no es el receptor de la oferta")
    })
    public ResponseEntity<?> responderOferta(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer al receptor de la oferta", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la oferta a responder", required = true, example = "1")
            @PathVariable Integer id,
            @Parameter(description = "true para aceptar la oferta, false para rechazarla", required = true, example = "true")
            @RequestParam Boolean acepta) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para responder una oferta.");
        }

        Integer receptorId = jwtUtil.extraerId(token);
        String  nombre     = jwtUtil.extraerNombre(token);

        try {
            OfertaRespuestaDto oferta = intercambioServicio.responderOferta(id, acepta, receptorId, nombre);
            return ResponseEntity.ok(oferta);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar oferta", description = "Cancela una oferta enviada antes de que el receptor la responda. Solo el emisor puede hacerlo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta con estado CANCELADA"),
            @ApiResponse(responseCode = "400", description = "La oferta no está en estado PENDIENTE u otro error de negocio"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o el usuario no es el emisor de la oferta")
    })
    public ResponseEntity<?> cancelarOferta(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer al emisor de la oferta", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la oferta a cancelar", required = true, example = "1")
            @PathVariable Integer id) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        Integer emisorId = jwtUtil.extraerId(token);
        String  nombre   = jwtUtil.extraerNombre(token);

        try {
            OfertaRespuestaDto oferta = intercambioServicio.cancelarOferta(id, emisorId, nombre);
            return ResponseEntity.ok(oferta);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    @PutMapping("/{id}/completar")
    @Operation(summary = "Completar intercambio", description = "Confirma que la entrega física de cartas se realizó. Puede hacerlo el emisor o el receptor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta con estado COMPLETADA"),
            @ApiResponse(responseCode = "400", description = "La oferta no está en estado ACEPTADA u otro error de negocio"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o el usuario no participa en la oferta")
    })
    public ResponseEntity<?> completarIntercambio(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la oferta a completar", required = true, example = "1")
            @PathVariable Integer id) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        Integer usuarioId = jwtUtil.extraerId(token);
        String  nombre    = jwtUtil.extraerNombre(token);

        try {
            OfertaRespuestaDto oferta = intercambioServicio.completarIntercambio(
                    id, usuarioId, nombre, authHeader);
            return ResponseEntity.ok(oferta);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    // Valida el header y devuelve el token limpio, o null si es invalido
    private String validarHeader(String authHeader) {
        String token = jwtUtil.obtenerTokenDelHeader(authHeader);
        if (token == null || !jwtUtil.esTokenValido(token)) {
            return null;
        }
        return token;
    }

    // Respuesta estandar 401 Unauthorized
    private ResponseEntity<?> respuestaNoAutorizado(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Respuesta estandar 400 Bad Request para errores de negocio
    private ResponseEntity<?> respuestaError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
