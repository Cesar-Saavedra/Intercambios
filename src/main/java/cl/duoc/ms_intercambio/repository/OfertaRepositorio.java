package cl.duoc.ms_intercambio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.ms_intercambio.model.EstadoOferta;
import cl.duoc.ms_intercambio.model.Oferta;

@Repository
public interface OfertaRepositorio extends JpaRepository<Oferta, Integer> {

    List<Oferta> findByEmisorId(Integer emisorId);

    List<Oferta> findByReceptorId(Integer receptorId);

    // Ofertas pendientes recibidas por un usuario
    // Las que el receptor debe responder (aceptar o rechazar)
    List<Oferta> findByReceptorIdAndEstado(Integer receptorId, EstadoOferta estado);

    // Todas las ofertas de un usuario (enviadas y recibidas) con un estado especifico
    // Para ver el historial completo de intercambios completados
    List<Oferta> findByEmisorIdAndEstado(Integer emisorId, EstadoOferta estado);
}
