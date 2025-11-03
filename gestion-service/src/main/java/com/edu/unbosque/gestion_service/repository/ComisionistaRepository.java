package com.edu.unbosque.gestion_service.repository;

import com.edu.unbosque.gestion_service.model.Comisionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComisionistaRepository extends JpaRepository<Comisionista, Integer> {

    @Query(value = "SELECT * FROM `Comisionista`", nativeQuery = true)
    @Override
    List<Comisionista> findAll();

    @Query(value = "SELECT * FROM `Comisionista` WHERE estado = true", nativeQuery = true)
    List<Comisionista> findByEstadoTrue();

    @Query(value = "SELECT * FROM `Comisionista` WHERE id_comisionista = ?1", nativeQuery = true)
    @Override
    java.util.Optional<Comisionista> findById(Integer id);

    @Query(value = "SELECT * FROM `Comisionista` WHERE email = ?1", nativeQuery = true)
    Optional<Comisionista> findByEmail(String email);
}
