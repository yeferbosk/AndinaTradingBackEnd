package com.edu.unbosque.gestion_service.repository;

import com.edu.unbosque.gestion_service.model.Comisionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComisionistaRepository extends JpaRepository<Comisionista, Integer> {

    List<Comisionista> findByEstadoTrue();
}
