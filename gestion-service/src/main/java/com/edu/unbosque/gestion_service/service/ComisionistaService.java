package com.edu.unbosque.gestion_service.service;

import com.edu.unbosque.gestion_service.repository.ComisionistaRepository;
import com.edu.unbosque.gestion_service.repository.UsuarioRepository;
import com.edu.unbosque.gestion_service.repository.UsuarioComisionistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edu.unbosque.gestion_service.model.Comisionista;
import com.edu.unbosque.gestion_service.model.Usuario;
import com.edu.unbosque.gestion_service.model.Usuario_Comisionista;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<Comisionista> listarTodosLosComisionistas() {
        return comisionistaRepository.findAll();
    }

    public Optional<Comisionista> obtenerComisionistaPorId(Integer idComisionista) {
        return comisionistaRepository.findById(idComisionista);
    }

    /**
     * Validar credenciales de un comisionista
     * @param email Email del comisionista
     * @param password Contraseña del comisionista
     * @return true si las credenciales son válidas, false en caso contrario
     */
    public boolean validarCredenciales(String email, String password) {
        Optional<Comisionista> comisionistaOpt = comisionistaRepository.findByEmail(email);
        if (comisionistaOpt.isPresent()) {
            Comisionista comisionista = comisionistaOpt.get();
            // Verificar que esté activo
            if (!comisionista.isEstado()) {
                return false;
            }
            // Verificar contraseña
            return password.equals(comisionista.getPassword());
        }
        return false;
    }

    /**
     * Encontrar comisionista por email
     */
    public Optional<Comisionista> encontrarComisionistaPorEmail(String email) {
        return comisionistaRepository.findByEmail(email);
    }

    /**
     * Vincular un usuario (trader) a un comisionista activo
     * Valida que el comisionista esté activo antes de crear la relación
     */
    @Transactional
    public Map<String, Object> vincularUsuarioAComisionista(Integer idUsuario, Integer idComisionista) {
        Map<String, Object> resultado = new HashMap<>();
        
        // Verificar que el usuario existe
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        if (usuarioOpt.isEmpty()) {
            resultado.put("success", false);
            resultado.put("message", "Usuario no encontrado");
            return resultado;
        }
        
        // Verificar que el comisionista existe
        Optional<Comisionista> comisionistaOpt = comisionistaRepository.findById(idComisionista);
        if (comisionistaOpt.isEmpty()) {
            resultado.put("success", false);
            resultado.put("message", "Comisionista no encontrado");
            return resultado;
        }
        
        Comisionista comisionista = comisionistaOpt.get();
        
        // Validar que el comisionista esté activo
        if (!comisionista.isEstado()) {
            resultado.put("success", false);
            resultado.put("message", "No se puede vincular a un comisionista inactivo");
            return resultado;
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Verificar si ya existe la relación
        List<Usuario_Comisionista> relacionesExistentes = usuarioComisionistaRepository.findByUsuario_IdUsuario(idUsuario);
        boolean yaEstaVinculado = relacionesExistentes.stream()
                .anyMatch(rel -> rel.getComisionista().getIdComisionista().equals(idComisionista));
        
        if (yaEstaVinculado) {
            resultado.put("success", false);
            resultado.put("message", "El usuario ya está vinculado a este comisionista");
            return resultado;
        }
        
        // Crear la relación
        Usuario_Comisionista usuarioComisionista = new Usuario_Comisionista(usuario, comisionista);
        usuarioComisionistaRepository.save(usuarioComisionista);
        
        resultado.put("success", true);
        resultado.put("message", "Usuario vinculado al comisionista exitosamente");
        resultado.put("usuarioId", idUsuario);
        resultado.put("comisionistaId", idComisionista);
        resultado.put("comisionistaNombre", comisionista.getNombre() + " " + comisionista.getApellido());
        
        return resultado;
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
    
    /**
     * Obtener los comisionistas vinculados a un usuario
     */
    public List<Map<String, Object>> obtenerComisionistasDelUsuario(Integer idUsuario) {
        List<Usuario_Comisionista> relaciones = usuarioComisionistaRepository.findByUsuario_IdUsuario(idUsuario);
        
        return relaciones.stream()
                .map(rel -> {
                    Comisionista com = rel.getComisionista();
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", com.getIdComisionista());
                    mapa.put("nombre", com.getNombre() != null ? com.getNombre() : "");
                    mapa.put("apellido", com.getApellido() != null ? com.getApellido() : "");
                    mapa.put("email", com.getEmail() != null ? com.getEmail() : "");
                    mapa.put("telefono", com.getTelefono() != null ? com.getTelefono() : "");
                    mapa.put("estado", com.isEstado());
                    return mapa;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Obtener los traders (usuarios) asociados a un comisionista
     */
    public List<Map<String, Object>> obtenerTradersDelComisionista(Integer idComisionista) {
        List<Usuario_Comisionista> relaciones = usuarioComisionistaRepository.findByComisionista_IdComisionista(idComisionista);
        
        return relaciones.stream()
                .map(rel -> {
                    Usuario usuario = rel.getUsuario();
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", usuario.getIdUsuario());
                    mapa.put("nombre", usuario.getNombre() != null ? usuario.getNombre() : "");
                    mapa.put("apellido", usuario.getApellido() != null ? usuario.getApellido() : "");
                    mapa.put("email", usuario.getEmail() != null ? usuario.getEmail() : "");
                    mapa.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
                    mapa.put("rol", usuario.getRol() != null ? usuario.getRol() : "");
                    mapa.put("estado", usuario.isEstado());
                    return mapa;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}