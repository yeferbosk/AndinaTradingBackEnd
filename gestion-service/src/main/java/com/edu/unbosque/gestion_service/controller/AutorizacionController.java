package com.edu.unbosque.gestion_service.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.edu.unbosque.gestion_service.service.UsuarioService;
import com.edu.unbosque.gestion_service.service.OtpStorageService;
import com.edu.unbosque.gestion_service.service.CorreosService;
import com.edu.unbosque.gestion_service.config.TokenAdmin;
import com.edu.unbosque.gestion_service.model.Usuario;


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