package com.edu.unbosque.bolsa_service.Repository;

import com.edu.unbosque.bolsa_service.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    
    List<Transaccion> findByCuentaIdOrderByFechaTransaccionDesc(Long cuentaId);
    
    List<Transaccion> findByCuentaIdAndTipo(Long cuentaId, String tipo);
    
    List<Transaccion> findByCuentaIdAndSimbolo(Long cuentaId, String simbolo);
}

