package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.model.OrdenComisionista;
import com.edu.unbosque.bolsa_service.Repository.OrdenComisionistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrdenComisionistaService {

    private static final Logger logger = LoggerFactory.getLogger(OrdenComisionistaService.class);

    @Autowired
    private OrdenComisionistaRepository ordenComisionistaRepository;

    @Autowired
    private PaperTradingService paperTradingService;

    /**
     * Envía una orden de compra del comisionista al trader
     */
    @Transactional
    public Map<String, Object> enviarOrden(Integer idComisionista, Integer idTrader, 
                                            String simbolo, String nombreEmpresa, 
                                            Integer cantidad, BigDecimal precioLimite, 
                                            String mensaje) {
        Map<String, Object> resultado = new HashMap<>();

        // Validaciones
        if (cantidad <= 0) {
            resultado.put("success", false);
            resultado.put("message", "La cantidad debe ser mayor a 0");
            return resultado;
        }

        // Verificar si ya existe una orden pendiente para el mismo trader, comisionista y símbolo
        if (ordenComisionistaRepository.existsByIdTraderAndIdComisionistaAndSimboloAndEstado(
                idTrader, idComisionista, simbolo, "PENDIENTE_APROBACION")) {
            resultado.put("success", false);
            resultado.put("message", "Ya existe una orden pendiente para este trader con el mismo símbolo");
            return resultado;
        }

        // Crear la orden
        OrdenComisionista orden = new OrdenComisionista();
        orden.setIdComisionista(idComisionista);
        orden.setIdTrader(idTrader);
        orden.setSimbolo(simbolo);
        orden.setNombreEmpresa(nombreEmpresa != null ? nombreEmpresa : simbolo);
        orden.setCantidad(cantidad);
        orden.setPrecioLimite(precioLimite);
        orden.setMensaje(mensaje);
        orden.setEstado("PENDIENTE_APROBACION");

        orden = ordenComisionistaRepository.save(orden);

        logger.info("Orden de comisionista creada - ID: {}, Comisionista: {}, Trader: {}, Símbolo: {}, Cantidad: {}",
                   orden.getId(), idComisionista, idTrader, simbolo, cantidad);

        resultado.put("success", true);
        resultado.put("message", "Orden enviada exitosamente al trader");
        resultado.put("ordenId", orden.getId());
        resultado.put("estado", orden.getEstado());

        return resultado;
    }

    /**
     * Obtiene todas las órdenes enviadas por un comisionista
     */
    public List<OrdenComisionista> obtenerOrdenesDelComisionista(Integer idComisionista) {
        return ordenComisionistaRepository.findByIdComisionistaOrderByFechaCreacionDesc(idComisionista);
    }

    /**
     * Obtiene las órdenes pendientes de aprobación de un trader
     */
    public List<OrdenComisionista> obtenerOrdenesPendientesDelTrader(Integer idTrader) {
        return ordenComisionistaRepository.findByIdTraderAndEstadoOrderByFechaCreacionDesc(
            idTrader, "PENDIENTE_APROBACION");
    }

    /**
     * Obtiene todas las órdenes de un trader (cualquier estado)
     */
    public List<OrdenComisionista> obtenerTodasLasOrdenesDelTrader(Integer idTrader) {
        return ordenComisionistaRepository.findByIdTraderOrderByFechaCreacionDesc(idTrader);
    }

    /**
     * Acepta una orden y la ejecuta
     */
    @Transactional
    public Map<String, Object> aceptarOrden(Long ordenId) {
        Map<String, Object> resultado = new HashMap<>();

        Optional<OrdenComisionista> ordenOpt = ordenComisionistaRepository.findById(ordenId);
        
        if (ordenOpt.isEmpty()) {
            resultado.put("success", false);
            resultado.put("message", "Orden no encontrada");
            return resultado;
        }

        OrdenComisionista orden = ordenOpt.get();

        // Validar que esté pendiente
        if (!"PENDIENTE_APROBACION".equals(orden.getEstado())) {
            resultado.put("success", false);
            resultado.put("message", "La orden ya fue procesada. Estado actual: " + orden.getEstado());
            return resultado;
        }

        // Actualizar estado a ACEPTADA
        orden.setEstado("ACEPTADA");
        ordenComisionistaRepository.save(orden);

        logger.info("Orden aceptada - ID: {}, Trader: {}, Símbolo: {}, Cantidad: {}",
                   ordenId, orden.getIdTrader(), orden.getSimbolo(), orden.getCantidad());

        // Ejecutar la compra real
        try {
            Map<String, Object> compraResultado = paperTradingService
                .comprarAccionesConPrecioReal(
                    orden.getIdTrader(),
                    orden.getSimbolo(),
                    orden.getNombreEmpresa(),
                    orden.getCantidad()
                ).join();

            if ((Boolean) compraResultado.getOrDefault("success", false)) {
                // Actualizar orden como EJECUTADA
                orden.setEstado("EJECUTADA");
                
                // Si la compra retornó un ID de orden, guardarlo
                if (compraResultado.containsKey("ordenId")) {
                    orden.setOrdenEjecutadaId(((Number) compraResultado.get("ordenId")).longValue());
                }
                
                ordenComisionistaRepository.save(orden);

                resultado.put("success", true);
                resultado.put("message", "Orden aceptada y ejecutada exitosamente");
                resultado.put("ordenId", ordenId);
                resultado.put("compra", compraResultado);
            } else {
                // La compra falló
                orden.setEstado("ERROR_EJECUCION");
                ordenComisionistaRepository.save(orden);

                resultado.put("success", false);
                resultado.put("message", "Orden aceptada pero la ejecución falló: " + compraResultado.get("message"));
                resultado.put("errorCompra", compraResultado.get("message"));
            }

        } catch (Exception e) {
            logger.error("Error al ejecutar orden aceptada: {}", e.getMessage(), e);
            orden.setEstado("ERROR_EJECUCION");
            ordenComisionistaRepository.save(orden);

            resultado.put("success", false);
            resultado.put("message", "Error al ejecutar la orden: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Rechaza una orden
     */
    @Transactional
    public Map<String, Object> rechazarOrden(Long ordenId) {
        Map<String, Object> resultado = new HashMap<>();

        Optional<OrdenComisionista> ordenOpt = ordenComisionistaRepository.findById(ordenId);
        
        if (ordenOpt.isEmpty()) {
            resultado.put("success", false);
            resultado.put("message", "Orden no encontrada");
            return resultado;
        }

        OrdenComisionista orden = ordenOpt.get();

        // Validar que esté pendiente
        if (!"PENDIENTE_APROBACION".equals(orden.getEstado())) {
            resultado.put("success", false);
            resultado.put("message", "La orden ya fue procesada. Estado actual: " + orden.getEstado());
            return resultado;
        }

        // Actualizar estado a RECHAZADA
        orden.setEstado("RECHAZADA");
        ordenComisionistaRepository.save(orden);

        logger.info("Orden rechazada - ID: {}, Trader: {}, Símbolo: {}", 
                   ordenId, orden.getIdTrader(), orden.getSimbolo());

        resultado.put("success", true);
        resultado.put("message", "Orden rechazada exitosamente");
        resultado.put("ordenId", ordenId);

        return resultado;
    }

    /**
     * Obtiene una orden por su ID
     */
    public Optional<OrdenComisionista> obtenerOrdenPorId(Long ordenId) {
        return ordenComisionistaRepository.findById(ordenId);
    }
}

