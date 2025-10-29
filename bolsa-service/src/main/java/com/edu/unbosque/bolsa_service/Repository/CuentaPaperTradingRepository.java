package com.edu.unbosque.bolsa_service.Repository;

import com.edu.unbosque.bolsa_service.model.CuentaPaperTrading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaPaperTradingRepository extends JpaRepository<CuentaPaperTrading, Long> {
    
    Optional<CuentaPaperTrading> findByUsuarioId(Integer usuarioId);
    
    boolean existsByUsuarioId(Integer usuarioId);
}

