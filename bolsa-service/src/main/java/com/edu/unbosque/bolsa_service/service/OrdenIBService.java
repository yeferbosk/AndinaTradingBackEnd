package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.model.Orden;
import com.edu.unbosque.bolsa_service.Repository.OrdenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrdenIBService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrdenIBService.class);
    
    @Autowired
    private OrdenRepository ordenRepository;
    
    @Autowired
    private IBService ibService;
    
    /**
     * Procesa una orden local y la envía a Interactive Brokers
     */
    public CompletableFuture<Orden> procesarOrdenConIB(Long ordenId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Orden> ordenOpt = ordenRepository.findById(ordenId);
                if (ordenOpt.isEmpty()) {
                    logger.error("Orden {} no encontrada", ordenId);
                    return null;
                }
                
                Orden orden = ordenOpt.get();
                logger.info("Procesando orden {} con IB: {} {} {} @ {}", 
                    ordenId, orden.getAccion(), orden.getCantidad(), 
                    orden.getSimbolo(), orden.getPrecio());
                
                // Verificar conexión con IB
                if (!ibService.isConnected()) {
                    logger.warn("No hay conexión con IB Gateway, conectando...");
                    ibService.connect().join();
                }
                
                // Crear contrato IB
                Map<String, Object> contract = new HashMap<>();
                contract.put("symbol", orden.getSimbolo());
                contract.put("secType", "STK");
                contract.put("exchange", "SMART");
                contract.put("currency", "USD");
                
                // Crear orden IB
                Map<String, Object> ibOrder = new HashMap<>();
                ibOrder.put("action", orden.getAccion().equals("COMPRA") ? "BUY" : "SELL");
                ibOrder.put("totalQuantity", orden.getCantidad().intValue());
                ibOrder.put("orderType", "LMT");
                ibOrder.put("lmtPrice", orden.getPrecio());
                
                // Enviar orden a IB
                Integer ibOrderId = ibService.placeOrder(contract, ibOrder).join();
                
                if (ibOrderId > 0) {
                    // Actualizar orden local con ID de IB
                    orden.setEstado("ENVIADA_IB");
                    orden.setIbOrderId(ibOrderId);
                    orden = ordenRepository.save(orden);
                    
                    logger.info("Orden {} enviada exitosamente a IB con ID: {}", ordenId, ibOrderId);
                } else {
                    orden.setEstado("ERROR_IB");
                    orden = ordenRepository.save(orden);
                    logger.error("Error al enviar orden {} a IB", ordenId);
                }
                
                return orden;
                
            } catch (Exception e) {
                logger.error("Error al procesar orden {} con IB: {}", ordenId, e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Cancela una orden en Interactive Brokers
     */
    public CompletableFuture<Boolean> cancelarOrdenEnIB(Long ordenId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Orden> ordenOpt = ordenRepository.findById(ordenId);
                if (ordenOpt.isEmpty()) {
                    logger.error("Orden {} no encontrada", ordenId);
                    return false;
                }
                
                Orden orden = ordenOpt.get();
                if (orden.getIbOrderId() == null) {
                    logger.warn("Orden {} no tiene ID de IB", ordenId);
                    return false;
                }
                
                // Cancelar en IB
                ibService.cancelOrder(orden.getIbOrderId()).join();
                
                // Actualizar estado local
                orden.setEstado("CANCELADA");
                ordenRepository.save(orden);
                
                logger.info("Orden {} cancelada en IB", ordenId);
                return true;
                
            } catch (Exception e) {
                logger.error("Error al cancelar orden {} en IB: {}", ordenId, e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Obtiene el estado de una orden desde Interactive Brokers
     */
    public CompletableFuture<String> obtenerEstadoOrdenIB(Long ordenId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Orden> ordenOpt = ordenRepository.findById(ordenId);
                if (ordenOpt.isEmpty()) {
                    return "ORDEN_NO_ENCONTRADA";
                }
                
                Orden orden = ordenOpt.get();
                if (orden.getIbOrderId() == null) {
                    return "SIN_ID_IB";
                }
                
                // En una implementación real, aquí se consultaría el estado desde IB
                // Por ahora retornamos el estado local
                return orden.getEstado();
                
            } catch (Exception e) {
                logger.error("Error al obtener estado de orden {}: {}", ordenId, e.getMessage());
                return "ERROR";
            }
        });
    }
    
    /**
     * Sincroniza todas las órdenes pendientes con Interactive Brokers
     */
    public CompletableFuture<Integer> sincronizarOrdenesPendientes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Orden> ordenesPendientes = ordenRepository.findByEstado("PENDIENTE");
                int procesadas = 0;
                
                logger.info("Sincronizando {} órdenes pendientes con IB", ordenesPendientes.size());
                
                for (Orden orden : ordenesPendientes) {
                    try {
                        procesarOrdenConIB(orden.getId()).join();
                        procesadas++;
                        Thread.sleep(1000); // Pausa entre órdenes
                    } catch (Exception e) {
                        logger.error("Error al procesar orden {}: {}", orden.getId(), e.getMessage());
                    }
                }
                
                logger.info("Sincronización completada: {} órdenes procesadas", procesadas);
                return procesadas;
                
            } catch (Exception e) {
                logger.error("Error en sincronización de órdenes: {}", e.getMessage());
                return 0;
            }
        });
    }
}
