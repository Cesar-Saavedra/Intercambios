package cl.duoc.ms_intercambio.model;

public enum  EstadoOferta {

    PENDIENTE, // La oferta fue enviada y espera respuesta del receptor
    ACEPTADA,  // El receptor acepto el intercambio (falta hacerlo fisicamente)
    RECHAZADA, // El receptor rechazo la oferta
    CANCELADA, // El emisor cancelo la oferta antes de recibir respuesta
    COMPLETADA // El intercambio fisico se realizo y ambos confirmaron
}
