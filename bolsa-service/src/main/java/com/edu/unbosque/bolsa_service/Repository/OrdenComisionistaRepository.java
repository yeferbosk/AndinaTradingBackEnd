package com.edu.unbosque.bolsa_service.Repository;

import com.edu.unbosque.bolsa_service.model.OrdenComisionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenComisionistaRepository extends JpaRepository<OrdenComisionista, Long> {
    
    /**
     * Obtiene todas las órdenes enviadas por un comisionista
     */
    List<OrdenComisionista> findByIdComisionistaOrderByFechaCreacionDesc(Integer idComisionista);
    
    /**
     * Obtiene todas las órdenes pendientes de aprobación de un trader
     */
    List<OrdenComisionista> findByIdTraderAndEstadoOrderByFechaCreacionDesc(Integer idTrader, String estado);
    
    /**
     * Obtiene todas las órdenes de un trader (cualquier estado)
     */
    List<OrdenComisionista> findByIdTraderOrderByFechaCreacionDesc(Integer idTrader);
    
    /**
     * Obtiene una orden específica por ID
     */
    Optional<OrdenComisionista> findById(Long id);
    
    /**
     * Verifica si existe una orden pendiente para el mismo trader, comisionista y símbolo
     */
    boolean existsByIdTraderAndIdComisionistaAndSimboloAndEstado(
        Integer idTrader, Integer idComisionista, String simbolo, String estado);
}
