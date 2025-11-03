package com.edu.unbosque.gestion_service.controller;


import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.edu.unbosque.gestion_service.service.UsuarioService;
import com.edu.unbosque.gestion_service.config.TokenAdmin;
import com.edu.unbosque.gestion_service.repository.UsuarioRepository;
import com.edu.unbosque.gestion_service.service.CorreosService;
import com.edu.unbosque.gestion_service.model.Usuario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final Logger log = LogManager.getLogger(UsuarioController.class);
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TokenAdmin tokenAdmin;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CorreosService correosService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    


    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }


    @GetMapping("/listadoUsuarios")
    public ResponseEntity<List<Usuario>> listadoUsuario(@RequestParam int idUsuarioLogeado) {
        List<Usuario> usuarios = usuarioService.listadoGeneralUsuariosFiltro(idUsuarioLogeado);
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/registroUsuario")
    public ResponseEntity<String> registroDeUsuario(@Valid @RequestBody Usuario usuario){
        Optional<Usuario> usuarioGuardado = usuarioService.guardarUsuario(usuario);

        if (usuarioGuardado.isPresent()) {
            return ResponseEntity.ok("Usuario registrado exitosamente.");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El usuario ya existe.");
        }
    }

    @PutMapping("/editarContrasena")
    public ResponseEntity<String> cambiarContrasena(@RequestParam String sesionUsuario,
                                                    @RequestBody String nuevaContrasena ){
        int idusuario  = Integer.parseInt(tokenAdmin.validarTokenIdentificadorUsuario(sesionUsuario));
        log.info("usuario: {}", idusuario);
        log.info("nuevaContrasena: {}", nuevaContrasena.hashCode());

        if (usuarioService.existeUsuario(idusuario)){
            log.info("si existe el usuario: {}", idusuario);
            usuarioService.actualizarCredencialesUsuario(idusuario, nuevaContrasena);
            return ResponseEntity.ok("Contrasena actualizada exitosamente.");
        }
        log.info("si no existe el usuario: {}", passwordEncoder.encode(nuevaContrasena));
        return ResponseEntity.status(HttpStatus.CONFLICT).body("El usuario ya existe.");
    }


    @PostMapping("/login")
    public ResponseEntity<?> loginUsuario(@RequestBody Map<String, String> loginRequest) {
        String correo = loginRequest.get("correo");
        String contrasena = loginRequest.get("contrasena");

        boolean credencialesValidas = usuarioService.validarCredenciales(correo, contrasena);

        if (credencialesValidas) {
            Optional<Usuario> usuarioOpt = usuarioService.encontrarUsuarioCorreo(correo);
            if (usuarioOpt.isPresent()) {
                return ResponseEntity.ok(usuarioOpt.get().getRol()); // 🔥 Solo devuelve el rol como string
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }

    @GetMapping("/by-email/{correo}")
    public ResponseEntity<?> obtenerUsuarioPorCorreo(@PathVariable String correo) {
        Optional<Usuario> usuarioOpt = usuarioService.encontrarUsuarioCorreo(correo);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
        }

        Usuario usuario = usuarioOpt.get();
        Map<String, Object> response = Map.of(
                "id", usuario.getIdUsuario(),
                "nombre", usuario.getNombre(),
                "apellido", usuario.getApellido(),
                "rol", usuario.getRol()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener perfil completo del usuario
     * Devuelve toda la información del perfil (sin contraseña)
     */
    @GetMapping("/perfil/{idUsuario}")
    public ResponseEntity<?> obtenerPerfil(@PathVariable Integer idUsuario) {
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorId(idUsuario);
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        Usuario usuario = usuarioOpt.get();
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("idUsuario", usuario.getIdUsuario());
        perfil.put("nombre", usuario.getNombre() != null ? usuario.getNombre() : "");
        perfil.put("apellido", usuario.getApellido() != null ? usuario.getApellido() : "");
        perfil.put("email", usuario.getEmail());
        perfil.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
        perfil.put("rol", usuario.getRol() != null ? usuario.getRol() : "");
        perfil.put("estado", usuario.isEstado());

        return ResponseEntity.ok(perfil);
    }

    /**
     * Actualizar perfil del usuario
     * Permite actualizar: nombre, apellido, email, telefono
     */
    @PutMapping("/perfil/{idUsuario}")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Integer idUsuario,
            @RequestBody Map<String, String> datosPerfil) {
        
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorId(idUsuario);
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        Usuario usuario = usuarioOpt.get();
        
        // Actualizar campos permitidos
        if (datosPerfil.containsKey("nombre")) {
            usuario.setNombre(datosPerfil.get("nombre"));
        }
        if (datosPerfil.containsKey("apellido")) {
            usuario.setApellido(datosPerfil.get("apellido"));
        }
        if (datosPerfil.containsKey("email")) {
            // Validar que el email no esté en uso por otro usuario
            Optional<Usuario> usuarioConEmail = usuarioService.encontrarUsuarioCorreo(datosPerfil.get("email"));
            if (usuarioConEmail.isPresent() && !usuarioConEmail.get().getIdUsuario().equals(idUsuario)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El email ya está en uso por otro usuario"));
            }
            usuario.setEmail(datosPerfil.get("email"));
        }
        if (datosPerfil.containsKey("telefono")) {
            usuario.setTelefono(datosPerfil.get("telefono"));
        }

        usuarioService.guardarUsuario(usuario);

        Map<String, Object> perfilActualizado = new HashMap<>();
        perfilActualizado.put("idUsuario", usuario.getIdUsuario());
        perfilActualizado.put("nombre", usuario.getNombre() != null ? usuario.getNombre() : "");
        perfilActualizado.put("apellido", usuario.getApellido() != null ? usuario.getApellido() : "");
        perfilActualizado.put("email", usuario.getEmail());
        perfilActualizado.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
        perfilActualizado.put("rol", usuario.getRol() != null ? usuario.getRol() : "");
        perfilActualizado.put("estado", usuario.isEstado());
        perfilActualizado.put("mensaje", "Perfil actualizado exitosamente");

        return ResponseEntity.ok(perfilActualizado);
    }

}