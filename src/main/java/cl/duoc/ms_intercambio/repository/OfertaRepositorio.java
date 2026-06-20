package cl.duoc.ms_intercambio.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.duoc.ms_intercambio.model.EstadoOferta;
import cl.duoc.ms_intercambio.model.Oferta;

@Repository
public interface OfertaRepositorio extends JpaRepository<Oferta, Integer> {

    /*
     * Busca la oferta y bloquea la fila (SELECT ... FOR UPDATE) hasta que
     * termine la transaccion actual. Evita que dos requests concurrentes
     * (ej. responder + cancelar la misma oferta al mismo tiempo) lean
     * ambos el estado PENDIENTE antes de que cualquiera lo cambie.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Oferta o WHERE o.id = :id")
    Optional<Oferta> findByIdConBloqueo(@Param("id") Integer id);

    List<Oferta> findByEmisorId(Integer emisorId);

    List<Oferta> findByReceptorId(Integer receptorId);

    // Ofertas pendientes recibidas por un usuario
    // Las que el receptor debe responder (aceptar o rechazar)
    List<Oferta> findByReceptorIdAndEstado(Integer receptorId, EstadoOferta estado);

    // Todas las ofertas de un usuario (enviadas y recibidas) con un estado especifico
    // Para ver el historial completo de intercambios completados
    List<Oferta> findByEmisorIdAndEstado(Integer emisorId, EstadoOferta estado);
}
