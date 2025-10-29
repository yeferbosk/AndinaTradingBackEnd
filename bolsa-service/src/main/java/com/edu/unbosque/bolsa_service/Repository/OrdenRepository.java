package com.edu.unbosque.bolsa_service.Repository;

import com.edu.unbosque.bolsa_service.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    
    List<Orden> findByEstado(String estado);
    
    List<Orden> findByUsuarioId(Long usuarioId);
    
    List<Orden> findByUsuarioIdAndEstado(Long usuarioId, String estado);
    
    List<Orden> findByUsuarioIdAndSimbolo(Long usuarioId, String simbolo);
}
