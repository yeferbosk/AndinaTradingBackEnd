package com.edu.unbosque.gestion_service.controller;

import com.edu.unbosque.gestion_service.model.Comisionista;
import com.edu.unbosque.gestion_service.service.ComisionistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comisionistas")
public class ComisionistaController {

    @Autowired
    private ComisionistaService comisionistaService;

    /**
     * Obtener lista de todos los comisionistas
     * Devuelve: id, nombre, apellido, telefono, email, estado
     */
    @GetMapping("/listado")
    public ResponseEntity<List<Map<String, Object>>> listarComisionistas(
            @RequestParam(required = false, defaultValue = "false") Boolean soloActivos) {
        
        List<Comisionista> comisionistas;
        if (Boolean.TRUE.equals(soloActivos)) {
            comisionistas = comisionistaService.listarComisionistasActivos();
        } else {
            comisionistas = comisionistaService.listarTodosLosComisionistas();
        }
        
        List<Map<String, Object>> comisionistasList = comisionistas.stream()
                .map(this::mapearComisionista)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(comisionistasList);
    }

    /**
     * Login para comisionistas
     * Valida email y contraseña, y verifica que el comisionista esté activo
     * IMPORTANTE: Debe estar antes de /{idComisionista} para evitar conflictos
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginComisionista(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        if (email == null || password == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Email y contraseña son requeridos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
            
        boolean credencialesValidas = comisionistaService.validarCredenciales(email, password);

        if (credencialesValidas) {
            Optional<Comisionista> comisionistaOpt = comisionistaService.encontrarComisionistaPorEmail(email);
            if (comisionistaOpt.isPresent()) {
                Comisionista comisionista = comisionistaOpt.get();
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("success", true);
                respuesta.put("message", "Login exitoso");
                respuesta.put("idComisionista", comisionista.getIdComisionista());
                respuesta.put("nombre", comisionista.getNombre());
                respuesta.put("apellido", comisionista.getApellido());
                respuesta.put("email", comisionista.getEmail());
                respuesta.put("estado", comisionista.isEstado());
                return ResponseEntity.ok(respuesta);
            }
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Credenciales inválidas o comisionista inactivo");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Obtener los traders (usuarios) asociados a un comisionista
     * Permite a un comisionista ver qué traders tiene vinculados
     */
    @GetMapping("/traders/{idComisionista}")
    public ResponseEntity<?> obtenerTradersDelComisionista(@PathVariable Integer idComisionista) {
        // Verificar que el comisionista existe
        Optional<Comisionista> comisionistaOpt = comisionistaService.obtenerComisionistaPorId(idComisionista);
        
        if (comisionistaOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Comisionista no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        List<Map<String, Object>> traders = comisionistaService.obtenerTradersDelComisionista(idComisionista);
        return ResponseEntity.ok(traders);
    }

    /**
     * Obtener un comisionista por su ID
     * Devuelve: id, nombre, apellido, telefono, email, estado
     */
    @GetMapping("/{idComisionista}")
    public ResponseEntity<?> obtenerComisionistaPorId(@PathVariable Integer idComisionista) {
        Optional<Comisionista> comisionistaOpt = comisionistaService.obtenerComisionistaPorId(idComisionista);
        
        if (comisionistaOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Comisionista no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Map<String, Object> comisionista = mapearComisionista(comisionistaOpt.get());
        return ResponseEntity.ok(comisionista);
    }

    /**
     * Vincular un usuario (trader) a un comisionista activo
     * Valida que el comisionista esté activo antes de crear la relación
     */
    @PostMapping("/vincular")
    public ResponseEntity<Map<String, Object>> vincularUsuarioAComisionista(
            @RequestParam Integer idUsuario,
            @RequestParam Integer idComisionista) {
        
        Map<String, Object> resultado = comisionistaService.vincularUsuarioAComisionista(idUsuario, idComisionista);
        
        if ((Boolean) resultado.get("success")) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
        }
    }
    
    /**
     * Desvincular un usuario de un comisionista
     */
    @DeleteMapping("/desvincular")
    public ResponseEntity<Map<String, Object>> desvincularUsuarioDeComisionista(
            @RequestParam Integer idUsuario,
            @RequestParam Integer idComisionista) {
        
        boolean desvinculado = comisionistaService.desvincularUsuarioDeComisionista(idUsuario, idComisionista);
        
        Map<String, Object> resultado = new HashMap<>();
        if (desvinculado) {
            resultado.put("success", true);
            resultado.put("message", "Usuario desvinculado del comisionista exitosamente");
        } else {
            resultado.put("success", false);
            resultado.put("message", "No se pudo desvincular. Verifique que la relación exista.");
        }
        
        return desvinculado 
            ? ResponseEntity.ok(resultado)
            : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
    }
    
    /**
     * Obtener los comisionistas vinculados a un usuario
     */
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Map<String, Object>>> obtenerComisionistasDelUsuario(
            @PathVariable Integer idUsuario) {
        
        List<Map<String, Object>> comisionistas = comisionistaService.obtenerComisionistasDelUsuario(idUsuario);
        return ResponseEntity.ok(comisionistas);
    }



    /**
     * Mapea un Comisionista a un Map con solo los campos solicitados
     * (sin password por seguridad)
     */
    private Map<String, Object> mapearComisionista(Comisionista comisionista) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("id", comisionista.getIdComisionista());
        mapa.put("nombre", comisionista.getNombre() != null ? comisionista.getNombre() : "");
        mapa.put("apellido", comisionista.getApellido() != null ? comisionista.getApellido() : "");
        mapa.put("telefono", comisionista.getTelefono() != null ? comisionista.getTelefono() : "");
        mapa.put("email", comisionista.getEmail() != null ? comisionista.getEmail() : "");
        mapa.put("estado", comisionista.isEstado());
        return mapa;
    }
}

