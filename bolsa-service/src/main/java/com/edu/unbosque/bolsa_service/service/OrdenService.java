package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.Repository.OrdenRepository;
import com.edu.unbosque.bolsa_service.model.Orden;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;

    public OrdenService(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    public List<Orden> getAll() {
        return ordenRepository.findAll();
    }

    public Optional<Orden> getById(Long id) {
        return ordenRepository.findById(id);
    }

    public Orden save(Orden orden) {
        return ordenRepository.save(orden);
    }

    public void delete(Long id) {
        ordenRepository.deleteById(id);
    }

}
