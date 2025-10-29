package com.edu.unbosque.gestion_service.service;

import com.edu.unbosque.gestion_service.repository.ComisionistaRepository;
import com.edu.unbosque.gestion_service.repository.UsuarioRepository;
import com.edu.unbosque.gestion_service.repository.UsuarioComisionistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edu.unbosque.gestion_service.model.Comisionista;
import com.edu.unbosque.gestion_service.model.Usuario;

import java.util.List;
import java.util.Optional;

@Service
public class ComisionistaService {

    private ComisionistaRepository comisionistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioComisionistaRepository usuarioComisionistaRepository;


    @Autowired
    public ComisionistaService(ComisionistaRepository comisionistaRepository, ComisionistaRepository comisionistaRepositorory, ComisionistaRepository comisionistaRepository1, UsuarioRepository usuarioRepository, UsuarioComisionistaRepository usuarioComisionistaRepository) {
        this.comisionistaRepository = comisionistaRepository1;


        this.usuarioRepository = usuarioRepository;
        this.usuarioComisionistaRepository = usuarioComisionistaRepository;
    }

    public List<Comisionista> listarComisionistasActivos() {
        return comisionistaRepository.findByEstadoTrue();
    }

    @Transactional
    public boolean desvincularUsuarioDeComisionista(Integer idUsuario, Integer idComisionista) {
        // Buscar el usuario y el comisionista por sus ID
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        Optional<Comisionista> comisionistaOpt = comisionistaRepository.findById(idComisionista);

        if (usuarioOpt.isPresent() && comisionistaOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            Comisionista comisionista = comisionistaOpt.get();

            // Eliminar la relación de la tabla intermedia
            usuarioComisionistaRepository.deleteByUsuarioAndComisionista(usuario, comisionista);
            return true;
        }

        return false;
    }
}