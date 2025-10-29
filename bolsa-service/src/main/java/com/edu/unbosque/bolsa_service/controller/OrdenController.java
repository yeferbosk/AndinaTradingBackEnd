package com.edu.unbosque.bolsa_service.controller;

import com.edu.unbosque.bolsa_service.model.Orden;
import com.edu.unbosque.bolsa_service.service.OrdenService;
import com.edu.unbosque.bolsa_service.service.OrdenIBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/ordenes")
public class OrdenController {

    private final OrdenService ordenService;
    
    @Autowired
    private OrdenIBService ordenIBService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping("/all")
    public List<Orden> getAll() {
        return ordenService.getAll();
    }

    @GetMapping("/{id}")
    public Optional<Orden> getById(@PathVariable Long id) {
        return ordenService.getById(id);
    }

    @PostMapping("/create")
    public Orden create(@RequestBody Orden orden) {
        return ordenService.save(orden);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ordenService.delete(id);
    }
    
    // ===========================================
    // ENDPOINTS PARA INTEGRACIÓN CON IB
    // ===========================================
    
    @PostMapping("/{id}/enviar-ib")
    public CompletableFuture<ResponseEntity<Orden>> enviarOrdenAIB(@PathVariable Long id) {
        return ordenIBService.procesarOrdenConIB(id)
            .thenApply(orden -> {
                if (orden != null) {
                    return ResponseEntity.ok(orden);
                } else {
                    return ResponseEntity.notFound().build();
                }
            });
    }
    
    @PostMapping("/{id}/cancelar-ib")
    public CompletableFuture<ResponseEntity<String>> cancelarOrdenEnIB(@PathVariable Long id) {
        return ordenIBService.cancelarOrdenEnIB(id)
            .thenApply(cancelada -> {
                if (cancelada) {
                    return ResponseEntity.ok("Orden cancelada exitosamente en IB");
                } else {
                    return ResponseEntity.badRequest().body("Error al cancelar la orden en IB");
                }
            });
    }
    
    @GetMapping("/{id}/estado-ib")
    public CompletableFuture<ResponseEntity<String>> obtenerEstadoIB(@PathVariable Long id) {
        return ordenIBService.obtenerEstadoOrdenIB(id)
            .thenApply(estado -> ResponseEntity.ok(estado));
    }
    
    @PostMapping("/sincronizar-ib")
    public CompletableFuture<ResponseEntity<String>> sincronizarOrdenesConIB() {
        return ordenIBService.sincronizarOrdenesPendientes()
            .thenApply(procesadas -> ResponseEntity.ok(
                "Sincronización completada. Órdenes procesadas: " + procesadas));
    }

}
