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
 *
 * -------------------------------------------------------
 * ENDPOINTS DISPONIBLES:
 * -------------------------------------------------------
 * POST /api/intercambios
 *     -> Enviar una oferta de intercambio a otro jugador
 *
 * GET /api/intercambios/recibidas
 *     -> Ver las ofertas pendientes que recibi
 *
 * GET /api/intercambios/enviadas
 *     -> Ver todas las ofertas que he enviado (historial)
 *
 * PUT /api/intercambios/{id}/responder?acepta=true|false
 *     -> Aceptar o rechazar una oferta que recibi
 *
 * PUT /api/intercambios/{id}/cancelar
 *     -> Cancelar una oferta que envie (antes de que la respondan)
 *
 * PUT /api/intercambios/{id}/completar
 *     -> Confirmar que el intercambio fisico se realizo
 */
@RestController
@RequestMapping("/api/intercambios")
public class IntercambioController {

    @Autowired
    private IntercambioServicio intercambioServicio;

    @Autowired JwtUtil jwtUtil;

    // =========================================================
    // POST /api/intercambios
    // Enviar una oferta de intercambio
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     *
     * El emisorId se extrae del token, no del body.
     *
     * Body JSON:
     * {
     *   "receptorId": 7,
     *   "ofrecido": "Charizard ex full art (NM) + Pikachu V promo",
     *   "solicitado": "Blastoise ex holo en buen estado",
     *   "mensaje": "Hola! Te propongo este cambio, avísame si te interesa"
     * }
     *
     * Respuesta 201: la oferta creada con estado PENDIENTE
     * Respuesta 400: si falta algun campo obligatorio o te mandas la oferta a ti mismo
     */
    @PostMapping
    public ResponseEntity<?> enviarOferta(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
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

    // =========================================================
    // GET /api/intercambios/recibidas
    // Ver las ofertas pendientes que recibi
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     *
     * Devuelve solo las ofertas con estado PENDIENTE donde
     * el usuario autenticado es el receptor.
     * Son las que debe responder (aceptar o rechazar).
     *
     * Respuesta 200: lista de OfertaRespuestaDto
     *
     * Ejemplo en Postman:
     * GET http://localhost:8088/api/intercambios/recibidas
     */
    @GetMapping("/recibidas")
    public ResponseEntity<?> misOfertasRecibidas(
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

    // =========================================================
    // GET /api/intercambios/enviadas
    // Ver todas las ofertas que he enviado
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     *
     * Devuelve todas las ofertas enviadas por el usuario autenticado,
     * incluyendo todos los estados (PENDIENTE, ACEPTADA, RECHAZADA, etc.).
     * Es el historial completo de ofertas enviadas.
     *
     * Respuesta 200: lista de OfertaRespuestaDto
     *
     * Ejemplo en Postman:
     * GET http://localhost:8088/api/intercambios/enviadas
     */
    @GetMapping("/enviadas")
    public ResponseEntity<?> misOfertasEnviadas(
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

    // =========================================================
    // PUT /api/intercambios/{id}/responder
    // Aceptar o rechazar una oferta recibida
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id de la oferta a responder
     * Query param: acepta = true (aceptar) | false (rechazar)
     *
     * Solo el receptor de la oferta puede responderla.
     * Solo se puede responder si esta en estado PENDIENTE.
     *
     * Ejemplo en Postman (aceptar):
     * PUT http://localhost:8088/api/intercambios/1/responder?acepta=true
     *
     * Ejemplo en Postman (rechazar):
     * PUT http://localhost:8088/api/intercambios/1/responder?acepta=false
     *
     * Respuesta 200: la oferta con el nuevo estado (ACEPTADA o RECHAZADA)
     */
    @PutMapping("/{id}/responder")
    public ResponseEntity<?> responderOferta(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id,
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

    // =========================================================
    // PUT /api/intercambios/{id}/cancelar
    // Cancelar una oferta enviada (antes de que la respondan)
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id de la oferta a cancelar
     *
     * Solo el emisor puede cancelar su propia oferta.
     * Solo se puede cancelar si esta en estado PENDIENTE.
     *
     * Respuesta 200: la oferta con estado CANCELADA
     *
     * Ejemplo en Postman:
     * PUT http://localhost:8088/api/intercambios/1/cancelar
     */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarOferta(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
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

    // =========================================================
    // PUT /api/intercambios/{id}/completar
    // Confirmar que el intercambio fisico se realizo
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id de la oferta a completar
     *
     * Pueden confirmarlo el emisor O el receptor.
     * Solo se puede completar si la oferta esta en estado ACEPTADA.
     *
     * Al completarse:
     * - El estado cambia a COMPLETADA.
     * - ms-usuarios recibe notificacion para sumar +1 intercambio
     *   en el perfil de AMBOS jugadores.
     *
     * Respuesta 200: la oferta con estado COMPLETADA
     *
     * Ejemplo en Postman:
     * PUT http://localhost:8088/api/intercambios/1/completar
     */
    @PutMapping("/{id}/completar")
    public ResponseEntity<?> completarIntercambio(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
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
