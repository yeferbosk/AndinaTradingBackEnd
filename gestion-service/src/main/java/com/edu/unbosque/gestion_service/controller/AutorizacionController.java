package com.edu.unbosque.gestion_service.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.edu.unbosque.gestion_service.service.UsuarioService;
import com.edu.unbosque.gestion_service.service.OtpStorageService;
import com.edu.unbosque.gestion_service.service.CorreosService;
import com.edu.unbosque.gestion_service.service.ComisionistaService;
import com.edu.unbosque.gestion_service.config.TokenAdmin;
import com.edu.unbosque.gestion_service.model.Usuario;
import com.edu.unbosque.gestion_service.model.Comisionista;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/auth")
public class AutorizacionController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private OtpStorageService otpStorageService;

    @Autowired
    private CorreosService correosService;

    @Autowired
    private TokenAdmin tokenAdmin;

    @Autowired
    private ComisionistaService comisionistaService;

    /**
     * Login unificado que detecta automáticamente si es Trader o Comisionista
     * Intenta primero como Usuario (Trader), si no encuentra, busca en Comisionista
     * Retorna información para redirigir al panel correspondiente
     */
    @PostMapping("/login-unificado")
    public ResponseEntity<?> loginUnificado(@RequestBody Map<String, String> jsonParametros) {
        String email = jsonParametros.get("email");
        String password = jsonParametros.get("password") != null ? jsonParametros.get("password") : jsonParametros.get("contrasena");
        
        if (email == null || password == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Email y contraseña son requeridos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> respuesta = new HashMap<>();

        // 1. Intentar como Usuario (Trader) primero
        if (usuarioService.validarCredenciales(email, password)) {
            Optional<Usuario> usuarioOpt = usuarioService.encontrarUsuarioCorreo(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                respuesta.put("success", true);
                respuesta.put("message", "Login exitoso");
                respuesta.put("tipoUsuario", "TRADER");
                respuesta.put("id", usuario.getIdUsuario());
                respuesta.put("nombre", usuario.getNombre() != null ? usuario.getNombre() : "");
                respuesta.put("apellido", usuario.getApellido() != null ? usuario.getApellido() : "");
                respuesta.put("email", usuario.getEmail());
                respuesta.put("rol", usuario.getRol() != null ? usuario.getRol() : "");
                respuesta.put("estado", usuario.isEstado());
                respuesta.put("redirigirA", "panel-trader"); // Indicador para el frontend
                return ResponseEntity.ok(respuesta);
            }
        }

        // 2. Si no es Usuario, intentar como Comisionista
        if (comisionistaService.validarCredenciales(email, password)) {
            Optional<Comisionista> comisionistaOpt = comisionistaService.encontrarComisionistaPorEmail(email);
            if (comisionistaOpt.isPresent()) {
                Comisionista comisionista = comisionistaOpt.get();
                respuesta.put("success", true);
                respuesta.put("message", "Login exitoso");
                respuesta.put("tipoUsuario", "COMISIONISTA");
                respuesta.put("id", comisionista.getIdComisionista());
                respuesta.put("nombre", comisionista.getNombre() != null ? comisionista.getNombre() : "");
                respuesta.put("apellido", comisionista.getApellido() != null ? comisionista.getApellido() : "");
                respuesta.put("email", comisionista.getEmail());
                respuesta.put("estado", comisionista.isEstado());
                respuesta.put("redirigirA", "panel-comisionista"); // Indicador para el frontend
                return ResponseEntity.ok(respuesta);
            }
        }

        // 3. Si no encontró en ninguno
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Credenciales inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> jsonParametros) {
        String email = jsonParametros.get("email");
        String contrasena = jsonParametros.get("contrasena");

        if (usuarioService.validarCredenciales(email, contrasena)) {
            Optional<Usuario> usuarioOpt = usuarioService.encontrarUsuarioCorreo(email);
            if (usuarioOpt.isPresent()) {
                // Generar código OTP aleatorio de 6 dígitos
                String codigoEnviado = String.valueOf(new Random().nextInt(900000) + 100000);

                // Guardar y enviar OTP
                otpStorageService.guardarOtp(email, codigoEnviado);
                correosService.sendOtpEmail(email, codigoEnviado);

                // Log para depuración
                System.out.println("[DEBUG] OTP generado para " + email + ": " + codigoEnviado);

                return ResponseEntity.ok("Código enviado al correo");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas.");
    }

    @PostMapping("/mfa/verificar")
    public ResponseEntity<String> verificarOtp(@RequestBody Map<String, String> jsonParametros) {
        String email = jsonParametros.get("email");
        String codigoOtp = jsonParametros.get("codigoOtp");

        String storedOtp = otpStorageService.obtenerOtp(email);

        // Log de depuración
        System.out.println("[DEBUG] Email recibido: " + email);
        System.out.println("[DEBUG] OTP ingresado: " + codigoOtp);
        System.out.println("[DEBUG] OTP almacenado: " + storedOtp);

        if (storedOtp != null && storedOtp.trim().equals(codigoOtp.trim())) {
            otpStorageService.eliminarOtp(email);

            Optional<Usuario> usuarioOpt = usuarioService.encontrarUsuarioCorreo(email);
            if (usuarioOpt.isPresent()) {
                String id = usuarioOpt.get().getIdUsuario().toString();
                String token = tokenAdmin.generarToken(id);
                return ResponseEntity.ok(token);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Código incorrecto.");
        }
    }
}