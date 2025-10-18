package com.edu.unbosque.bolsa_service.Repository;

import com.edu.unbosque.bolsa_service.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
}
