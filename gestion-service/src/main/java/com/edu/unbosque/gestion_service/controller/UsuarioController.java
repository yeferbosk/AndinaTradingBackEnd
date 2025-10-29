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

}