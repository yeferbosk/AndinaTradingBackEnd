package com.edu.unbosque.bolsa_service.controller;

import com.edu.unbosque.bolsa_service.model.OrdenComisionista;
import com.edu.unbosque.bolsa_service.service.OrdenComisionistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ordenes-comisionista")
public class OrdenComisionistaController {

    @Autowired
    private OrdenComisionistaService ordenComisionistaService;

    /**
     * Enviar una orden de compra del comisionista al trader
     * POST /api/ordenes-comisionista/enviar
     */
    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviarOrden(
            @RequestParam Integer idComisionista,
            @RequestParam Integer idTrader,
            @RequestParam String simbolo,
            @RequestParam(required = false) String nombreEmpresa,
            @RequestParam Integer cantidad,
            @RequestParam(required = false) BigDecimal precioLimite,
            @RequestParam(required = false) String mensaje) {

        Map<String, Object> resultado = ordenComisionistaService.enviarOrden(
            idComisionista, idTrader, simbolo, nombreEmpresa, cantidad, precioLimite, mensaje);

        if ((Boolean) resultado.getOrDefault("success", false)) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
        }
    }

    /**
     * Obtener todas las órdenes enviadas por un comisionista
     * GET /api/ordenes-comisionista/comisionista/{idComisionista}
     */
    @GetMapping("/comisionista/{idComisionista}")
    public ResponseEntity<List<Map<String, Object>>> obtenerOrdenesDelComisionista(
            @PathVariable Integer idComisionista) {

        List<OrdenComisionista> ordenes = ordenComisionistaService.obtenerOrdenesDelComisionista(idComisionista);
        
        List<Map<String, Object>> ordenesResponse = ordenes.stream()
            .map(this::mapearOrden)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ordenesResponse);
    }

    /**
     * Obtener las órdenes pendientes de aprobación de un trader
     * GET /api/ordenes-comisionista/trader/{idTrader}/pendientes
     */
    @GetMapping("/trader/{idTrader}/pendientes")
    public ResponseEntity<List<Map<String, Object>>> obtenerOrdenesPendientes(
            @PathVariable Integer idTrader) {

        List<OrdenComisionista> ordenes = ordenComisionistaService.obtenerOrdenesPendientesDelTrader(idTrader);
        
        List<Map<String, Object>> ordenesResponse = ordenes.stream()
            .map(this::mapearOrden)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ordenesResponse);
    }

    /**
     * Obtener todas las órdenes de un trader (cualquier estado)
     * GET /api/ordenes-comisionista/trader/{idTrader}
     */
    @GetMapping("/trader/{idTrader}")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodasLasOrdenesDelTrader(
            @PathVariable Integer idTrader) {

        List<OrdenComisionista> ordenes = ordenComisionistaService.obtenerTodasLasOrdenesDelTrader(idTrader);
        
        List<Map<String, Object>> ordenesResponse = ordenes.stream()
            .map(this::mapearOrden)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ordenesResponse);
    }

    /**
     * Aceptar una orden y ejecutarla
     * POST /api/ordenes-comisionista/{ordenId}/aceptar
     */
    @PostMapping("/{ordenId}/aceptar")
    public ResponseEntity<Map<String, Object>> aceptarOrden(@PathVariable Long ordenId) {
        Map<String, Object> resultado = ordenComisionistaService.aceptarOrden(ordenId);

        if ((Boolean) resultado.getOrDefault("success", false)) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
        }
    }

    /**
     * Rechazar una orden
     * POST /api/ordenes-comisionista/{ordenId}/rechazar
     */
    @PostMapping("/{ordenId}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarOrden(@PathVariable Long ordenId) {
        Map<String, Object> resultado = ordenComisionistaService.rechazarOrden(ordenId);

        if ((Boolean) resultado.getOrDefault("success", false)) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
        }
    }

    /**
     * Obtener una orden por su ID
     * GET /api/ordenes-comisionista/{ordenId}
     */
    @GetMapping("/{ordenId}")
    public ResponseEntity<?> obtenerOrdenPorId(@PathVariable Long ordenId) {
        Optional<OrdenComisionista> ordenOpt = ordenComisionistaService.obtenerOrdenPorId(ordenId);

        if (ordenOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Orden no encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(mapearOrden(ordenOpt.get()));
    }

    /**
     * Mapea una OrdenComisionista a un Map para la respuesta
     */
    private Map<String, Object> mapearOrden(OrdenComisionista orden) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("id", orden.getId());
        mapa.put("idComisionista", orden.getIdComisionista());
        mapa.put("idTrader", orden.getIdTrader());
        mapa.put("simbolo", orden.getSimbolo());
        mapa.put("nombreEmpresa", orden.getNombreEmpresa());
        mapa.put("cantidad", orden.getCantidad());
        mapa.put("precioLimite", orden.getPrecioLimite());
        mapa.put("estado", orden.getEstado());
        mapa.put("mensaje", orden.getMensaje());
        mapa.put("fechaCreacion", orden.getFechaCreacion());
        mapa.put("fechaActualizacion", orden.getFechaActualizacion());
        mapa.put("ordenEjecutadaId", orden.getOrdenEjecutadaId());
        return mapa;
    }
}

