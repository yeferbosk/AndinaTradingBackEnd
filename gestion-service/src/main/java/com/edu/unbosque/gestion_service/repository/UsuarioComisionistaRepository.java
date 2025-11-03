package com.edu.unbosque.gestion_service.repository;


import com.edu.unbosque.gestion_service.model.Usuario_Comisionista;
import com.edu.unbosque.gestion_service.model.Usuario;
import com.edu.unbosque.gestion_service.model.Comisionista;
import com.edu.unbosque.gestion_service.model.UsuarioComisionistaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;  
import java.util.Optional;

@Repository
public interface UsuarioComisionistaRepository extends JpaRepository<Usuario_Comisionista, UsuarioComisionistaId> {
    List<Usuario_Comisionista> findByUsuario_IdUsuario(Integer idUsuario);
    List<Usuario_Comisionista> findByComisionista_IdComisionista(Integer idComisionista);
    void deleteByUsuarioAndComisionista(Usuario usuario, Comisionista comisionista);



}