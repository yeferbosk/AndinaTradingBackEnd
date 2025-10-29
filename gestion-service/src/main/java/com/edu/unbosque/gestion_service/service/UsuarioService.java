package com.edu.unbosque.gestion_service.service;


import com.edu.unbosque.gestion_service.repository.UsuarioRepository;
import com.edu.unbosque.gestion_service.repository.UsuarioComisionistaRepository;
import com.edu.unbosque.gestion_service.model.Usuario;
import com.edu.unbosque.gestion_service.model.Comisionista;
import com.edu.unbosque.gestion_service.model.Usuario_Comisionista;
import com.edu.unbosque.gestion_service.config.AppConfig;
import com.edu.unbosque.gestion_service.model.Roles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {


    private final UsuarioRepository usuarioRepository;
    private final UsuarioComisionistaRepository usuarioComisionistaRepository;


    @Autowired
    private AppConfig appConfig;
    @Autowired
    private DataSourceAutoConfiguration dataSourceAutoConfiguration;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioComisionistaRepository usuarioComisionistaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioComisionistaRepository = usuarioComisionistaRepository;
    }

    public boolean existeUsuario(int id) {
        return usuarioRepository.existsById(id);
    }

    public Optional<Usuario> obtenerUsuarioPorId(Integer idUsuario) {
        try {
            Usuario usuario = usuarioRepository.encontrarUsuarioBasicoPorId(idUsuario);
            return Optional.of(usuario);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * @param usuario
     * @return Regresa la entidad si le es posible guardarla , se cambiara a un boolean por comodidad en el siguiente
     * pull request
     */
    public Optional<Usuario> guardarUsuario(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isEmpty()) {
            usuario.setRol(Roles.buscarRol(usuario.getRol()));
            return Optional.of(usuarioRepository.save(usuario));
        }
        return Optional.empty();
    }

    /**
     * @param correo
     * @param contrasena
     * @return regresa true o false dependiendo si existe el usuario y sus credenciales son correctas
     */
    public boolean validarCredenciales(String correo, String contrasena) {
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(correo);
        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();
            return contrasena.equals(user.getPassword());
        }
        return false;
    }

    public Optional<Usuario> encontrarUsuarioCorreo(String correo) {
        return usuarioRepository.findByEmail(correo);
    }

    @Transactional
    public void actualizarCredencialesUsuario(int idUsuario, String contrasena) {
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findById(idUsuario);

        if (usuarioEncontrado.isPresent()) {
            Usuario usuario = usuarioEncontrado.get();
            usuario.setPassword(contrasena);
            usuarioRepository.save(usuario);
        }
    }


    public List<Usuario> listadoGeneralUsuariosFiltro(int idUsuarioLogeado) {
        Optional<Usuario> usuarioSesion = usuarioRepository.findById(idUsuarioLogeado);
        String rol = usuarioSesion.get().getRol();
        return switch (rol) {
            case "Administrador" -> usuarioRepository.findAll();
            case "Trader" -> usuarioRepository.findByRol("Comisionista");
            case "Comisionista" -> usuarioRepository.findByRol("Trader");
            default -> List.of();
        };
    }

    public boolean asociarUsuarioComisionista(int idComisionistaSeleccionado, int idUsuarioLogeado) {
        Optional<Usuario> idComisionista = usuarioRepository.findById(idComisionistaSeleccionado);
        Optional<Usuario> idUsuarioLoge = usuarioRepository.findById(idUsuarioLogeado);
        Comisionista comisionista = new Comisionista();
        comisionista.setNombre(idComisionista.get().getNombre());
        comisionista.setApellido(idComisionista.get().getApellido());
        comisionista.setEmail(idComisionista.get().getEmail());
        comisionista.setTelefono(idComisionista.get().getTelefono());
        comisionista.setPassword(idComisionista.get().getPassword());
        comisionista.setEstado(true);

        Usuario_Comisionista usuarioComisionista = new Usuario_Comisionista(idUsuarioLoge.get(), comisionista);
        usuarioComisionistaRepository.save(usuarioComisionista);
        return true;
    }
}