package com.edu.unbosque.bolsa_service.Repository;

import com.edu.unbosque.bolsa_service.model.Posicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosicionRepository extends JpaRepository<Posicion, Long> {
    
    List<Posicion> findByCuentaId(Long cuentaId);
    
    Optional<Posicion> findByCuentaIdAndSimbolo(Long cuentaId, String simbolo);
    
    boolean existsByCuentaIdAndSimbolo(Long cuentaId, String simbolo);
}

