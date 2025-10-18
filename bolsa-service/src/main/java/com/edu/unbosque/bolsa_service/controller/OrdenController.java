package com.edu.unbosque.bolsa_service.controller;

import com.edu.unbosque.bolsa_service.model.Orden;
import com.edu.unbosque.bolsa_service.service.OrdenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/ordenes")
public class OrdenController {

    private final OrdenService ordenService;

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

}
