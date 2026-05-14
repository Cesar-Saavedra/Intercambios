package cl.duoc.ms_intercambio.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cl.duoc.ms_intercambio.model.EstadoOferta;
import cl.duoc.ms_intercambio.model.Oferta;
import cl.duoc.ms_intercambio.repository.OfertaRepositorio;
import lombok.Value;

@Service
public class IntercambioServicio {

    @Autowired
    private IntercambioServicio intercambioServicio;

    @Autowired
    private RestTemplate restTemplate; 

    @Value ("${ms.usuarios.url}")
    private String urlMsUsuarios;

    // =========================================================
    // ENVIAR OFERTA DE INTERCAMBIO
    // =========================================================

    /*
     * El usuario autenticado envia una oferta de intercambio a otro jugador.
     *
     * Proceso:
     * 1. Validar que el emisor no se este enviando la oferta a si mismo.
     * 2. Validar que llegaron los campos obligatorios.
     * 3. Guardar la oferta en la BD con estado PENDIENTE.
     * 4. Devolver la oferta creada con los datos del emisor.
     *
     * @param dto        datos del formulario con receptorId, ofrecido, solicitado
     * @param emisorId   id del jugador que envia la oferta (viene del token)
     * @param nombreEmisor nombre del emisor (viene del token)
     */
    public OfertaRespuestaDto enviarOferta(EnviarOfertaDto dto, Integer emisorId, String nombreEmisor) {

        // Paso 1: no se puede enviar una oferta a uno mismo
        if (emisorId.equals(dto.getReceptorId())) {
            throw new RuntimeException("No puedes enviarte una oferta de intercambio a ti mismo.");
        }

        // Paso 2: validar campos obligatorios
        if (dto.getOfrecido() == null || dto.getOfrecido().isBlank()) {
            throw new RuntimeException("Debes describir que cartas estas ofreciendo.");
        }
        if (dto.getSolicitado() == null || dto.getSolicitado().isBlank()) {
            throw new RuntimeException("Debes describir que cartas estas solicitando a cambio.");
        }
        if (dto.getReceptorId() == null) {
            throw new RuntimeException("Debes indicar a quien le envias la oferta (receptorId).");
        }

        // Paso 3: crear y guardar la oferta
        Oferta nueva = new Oferta();
        nueva.setEmisorId(emisorId);
        nueva.setReceptorId(dto.getReceptorId());
        nueva.setOfrecido(dto.getOfrecido());
        nueva.setSolicitado(dto.getSolicitado());
        nueva.setMensaje(dto.getMensaje());
        nueva.setEstado(EstadoOferta.PENDIENTE);
        nueva.setFechaCreacion(LocalDateTime.now());
        nueva.setFechaRespuesta(null); // null hasta que el receptor responda

        Oferta guardada = OfertaRepositorio.save(nueva);

        // Paso 4: devolver con nombre del emisor del token
        // El nombre del receptor no lo tenemos sin llamar a ms-login
        return construirRespuesta(guardada, nombreEmisor, "Jugador #" + dto.getReceptorId());
    }

    // =========================================================
    // VER OFERTAS RECIBIDAS PENDIENTES
    // =========================================================

    /*
     * Devuelve las ofertas que el usuario autenticado recibio y aun no respondio.
     * Son las que debe responder (aceptar o rechazar).
     *
     * @param receptorId  id del jugador receptor (viene del token)
     * @param nombre      nombre del receptor (viene del token)
     */
    public List<OfertaRespuestaDto> misOfertasRecibidas(Integer receptorId, String nombre) {

        // Buscar solo las ofertas PENDIENTES donde yo soy el receptor
        List<Oferta> ofertasPendientes = OfertaRepositorio
                .findByReceptorIdAndEstado(receptorId, EstadoOferta.PENDIENTE);

        List<OfertaRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Oferta oferta : ofertasPendientes) {
            // El nombre del emisor no lo tenemos sin llamar a ms-login
            // Mostramos su id hasta que tengamos esa integracion
            listaRespuesta.add(construirRespuesta(
                    oferta,
                    "Jugador #" + oferta.getEmisorId(),
                    nombre
            ));
        }

        return listaRespuesta;
    }

    // =========================================================
    // VER OFERTAS ENVIADAS
    // =========================================================

    /*
     * Devuelve todas las ofertas que el usuario autenticado ha enviado.
     * Incluye todos los estados para ver el historial completo.
     *
     * @param emisorId  id del jugador emisor (viene del token)
     * @param nombre    nombre del emisor (viene del token)
     */
    public List<OfertaRespuestaDto> misOfertasEnviadas(Integer emisorId, String nombre) {

        List<Oferta> ofertas = ofertaRepository.findByEmisorId(emisorId);

        List<OfertaRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Oferta oferta : ofertas) {
            listaRespuesta.add(construirRespuesta(
                    oferta,
                    nombre,
                    "Jugador #" + oferta.getReceptorId()
            ));
        }

        return listaRespuesta;
    }

    // =========================================================
    // ACEPTAR O RECHAZAR UNA OFERTA
    // =========================================================

    /*
     * El receptor responde una oferta: la acepta o la rechaza.
     *
     * Proceso:
     * 1. Buscar la oferta por id.
     * 2. Verificar que el usuario autenticado sea realmente el receptor.
     * 3. Verificar que la oferta este en estado PENDIENTE.
     * 4. Cambiar el estado a ACEPTADA o RECHAZADA segun el parametro.
     * 5. Guardar y devolver la oferta actualizada.
     *
     * @param ofertaId    id de la oferta a responder
     * @param acepta      true = ACEPTADA, false = RECHAZADA
     * @param receptorId  id del usuario autenticado (para verificar que es el receptor)
     * @param nombre      nombre del receptor (viene del token)
     */
    public OfertaRespuestaDto responderOferta(Integer ofertaId, Boolean acepta,
                                               Integer receptorId, String nombre) {

        // Paso 1: buscar la oferta en la BD
        Oferta oferta = OfertaRepositorio.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con id: " + ofertaId));

        // Paso 2: verificar que el usuario autenticado sea el receptor de la oferta
        if (!oferta.getReceptorId().equals(receptorId)) {
            throw new RuntimeException("No puedes responder una oferta que no te enviaron a ti.");
        }

        // Paso 3: solo se puede responder si esta PENDIENTE
        if (!EstadoOferta.PENDIENTE.equals(oferta.getEstado())) {
            throw new RuntimeException("Esta oferta ya fue respondida. Estado actual: " + oferta.getEstado());
        }

        // Paso 4: cambiar el estado segun la decision del receptor
        oferta.setEstado(acepta ? EstadoOferta.ACEPTADA : EstadoOferta.RECHAZADA);
        oferta.setFechaRespuesta(LocalDateTime.now());

        // Paso 5: guardar y devolver
        Oferta actualizada = OfertaRepositorio.save(oferta);
        return construirRespuesta(actualizada, "Jugador #" + oferta.getEmisorId(), nombre);
    }

    // =========================================================
    // CANCELAR UNA OFERTA (solo el emisor)
    // =========================================================

    /*
     * El emisor cancela una oferta que envio antes de que el receptor la responda.
     *
     * @param ofertaId  id de la oferta a cancelar
     * @param emisorId  id del usuario autenticado (para verificar que es el emisor)
     * @param nombre    nombre del emisor (viene del token)
     */
    public OfertaRespuestaDto cancelarOferta(Integer ofertaId, Integer emisorId, String nombre) {

        Oferta oferta = OfertaRepositorio.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con id: " + ofertaId));

        // Solo el emisor puede cancelar su propia oferta
        if (!oferta.getEmisorId().equals(emisorId)) {
            throw new RuntimeException("No puedes cancelar una oferta que no enviaste tu.");
        }

        // Solo se puede cancelar si esta PENDIENTE
        if (!EstadoOferta.PENDIENTE.equals(oferta.getEstado())) {
            throw new RuntimeException("Solo puedes cancelar ofertas que esten PENDIENTES. "
                    + "Estado actual: " + oferta.getEstado());
        }

        oferta.setEstado(EstadoOferta.CANCELADA);
        oferta.setFechaRespuesta(LocalDateTime.now());

        Oferta actualizada = OfertaRepositorio.save(oferta);
        return construirRespuesta(actualizada, nombre, "Jugador #" + oferta.getReceptorId());
    }

    // =========================================================
    // COMPLETAR INTERCAMBIO (confirmar entrega fisica)
    // =========================================================

    /*
     * Confirma que el intercambio fisico de cartas se realizo exitosamente.
     * Pueden confirmarlo el emisor o el receptor.
     *
     * Cuando se completa:
     * - El estado cambia a COMPLETADA.
     * - Se notifica a ms-usuarios para sumar +1 intercambio en el perfil
     *   de ambos jugadores (emisor y receptor).
     *
     * @param ofertaId    id de la oferta a completar
     * @param usuarioId   id del usuario autenticado (debe ser emisor o receptor)
     * @param nombre      nombre del usuario (viene del token)
     * @param authHeader  header para notificar a ms-usuarios
     */
    public OfertaRespuestaDto completarIntercambio(Integer ofertaId, Integer usuarioId,
                                                    String nombre, String authHeader) {

        Oferta oferta = OfertaRepositorio.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con id: " + ofertaId));

        // Verificar que quien confirma sea el emisor o el receptor
        boolean esEmisor   = oferta.getEmisorId().equals(usuarioId);
        boolean esReceptor = oferta.getReceptorId().equals(usuarioId);

        if (!esEmisor && !esReceptor) {
            throw new RuntimeException("No eres parte de este intercambio.");
        }

        // Solo se puede completar si estaba ACEPTADA
        if (!EstadoOferta.ACEPTADA.equals(oferta.getEstado())) {
            throw new RuntimeException("Solo se pueden completar intercambios que fueron ACEPTADOS. "
                    + "Estado actual: " + oferta.getEstado());
        }

        // Cambiar el estado a COMPLETADA
        oferta.setEstado(EstadoOferta.COMPLETADA);
        oferta.setFechaRespuesta(LocalDateTime.now());
        Oferta completada = OfertaRepositorio.save(oferta);

        // Notificar a ms-usuarios para sumar +1 intercambio a AMBOS jugadores
        // Si ms-usuarios no responde, el intercambio igual queda completado
        notificarIntercambioCompletado(oferta.getEmisorId(), authHeader);
        notificarIntercambioCompletado(oferta.getReceptorId(), authHeader);

        return construirRespuesta(completada, "Jugador #" + oferta.getEmisorId(), nombre);
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    /*
     * Llama a ms-usuarios para notificar que un jugador completo un intercambio.
     * ms-usuarios incrementa en 1 el contador de intercambios del perfil del jugador.
     *
     * @param usuarioId  id del jugador a notificar
     * @param authHeader header completo "Bearer eyJ..." para la peticion
     */
    private void notificarIntercambioCompletado(Integer usuarioId, String authHeader) {
        try {
            String url = urlMsUsuarios + "/api/perfiles/sumar-intercambio/" + usuarioId;

            // Crear el header con el token para que ms-usuarios autorice la peticion
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> peticion = new HttpEntity<>(headers);

            // Hacer la peticion PUT (sin body, solo incrementa el contador)
            restTemplate.exchange(url, HttpMethod.PUT, peticion, Void.class);

        } catch (Exception e) {
            // Si ms-usuarios no responde, el intercambio queda igual completado
            System.out.println("[ms-intercambio] No se pudo notificar a ms-usuarios "
                    + "para usuario " + usuarioId + ": " + e.getMessage());
        }
    }

    /*
     * Convierte una entidad Oferta al DTO de respuesta.
     *
     * @param oferta         entidad con los datos de la BD
     * @param nombreEmisor   nombre del emisor
     * @param nombreReceptor nombre del receptor
     */
    private OfertaRespuestaDto construirRespuesta(Oferta oferta,
                                                   String nombreEmisor,
                                                   String nombreReceptor) {
        OfertaRespuestaDto respuesta = new OfertaRespuestaDto();
        respuesta.setId(oferta.getId());
        respuesta.setEmisorId(oferta.getEmisorId());
        respuesta.setNombreEmisor(nombreEmisor);
        respuesta.setReceptorId(oferta.getReceptorId());
        respuesta.setNombreReceptor(nombreReceptor);
        respuesta.setOfrecido(oferta.getOfrecido());
        respuesta.setSolicitado(oferta.getSolicitado());
        respuesta.setMensaje(oferta.getMensaje());
        respuesta.setEstado(oferta.getEstado());
        respuesta.setFechaCreacion(oferta.getFechaCreacion());
        respuesta.setFechaRespuesta(oferta.getFechaRespuesta());
        return respuesta;
    }





}
