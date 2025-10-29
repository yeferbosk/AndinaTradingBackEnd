package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.config.IBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class IBService {
    
    private static final Logger logger = LoggerFactory.getLogger(IBService.class);
    
    @Autowired
    private IBConfig ibConfig;
    
    private boolean connected = false;
    private Map<Integer, String> orderStatuses = new ConcurrentHashMap<>();
    private Map<String, Double> marketData = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("IBService inicializado correctamente (Modo Mock)");
        logger.info("Configuración IB - Host: {}, Puerto: {}, Client ID: {}", 
            ibConfig.getHost(), ibConfig.getPort(), ibConfig.getClientId());
    }
    
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Conectando a IB Gateway en {}:{} (Modo Mock)", ibConfig.getHost(), ibConfig.getPort());
                
                // Simular tiempo de conexión
                Thread.sleep(1000);
                
                connected = true;
                logger.info("Conectado exitosamente a IB Gateway (Modo Mock)");
                
                // Simular datos de mercado iniciales
                marketData.put("AAPL", 150.25);
                marketData.put("MSFT", 300.50);
                marketData.put("GOOGL", 2500.75);
                
                return true;
            } catch (Exception e) {
                logger.error("Error al conectar con IB Gateway: {}", e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connected) {
                    connected = false;
                    logger.info("Desconectado de IB Gateway (Modo Mock)");
                }
                return true;
            } catch (Exception e) {
                logger.error("Error al desconectar de IB Gateway: {}", e.getMessage());
                return false;
            }
        });
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public CompletableFuture<Map<String, Object>> getContract(String symbol, String secType, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> contract = new ConcurrentHashMap<>();
                contract.put("symbol", symbol);
                contract.put("secType", secType);
                contract.put("exchange", exchange);
                contract.put("currency", "USD");
                contract.put("conId", System.currentTimeMillis() % 100000);
                
                logger.info("Contrato obtenido para {}: {}", symbol, contract);
                return contract;
            } catch (Exception e) {
                logger.error("Error al obtener contrato para {}: {}", symbol, e.getMessage());
                return null;
            }
        });
    }
    
    public CompletableFuture<Integer> placeOrder(Map<String, Object> contract, Map<String, Object> order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.error("No hay conexión con IB Gateway");
                    return -1;
                }
                
                // Generar un ID único para la orden
                int orderId = (int) System.currentTimeMillis() % 100000;
                
                // Simular procesamiento de orden
                Thread.sleep(500);
                
                orderStatuses.put(orderId, "SUBMITTED");
                
                logger.info("Orden {} enviada para {} - Cantidad: {}, Precio: {}", 
                    orderId, contract.get("symbol"), order.get("totalQuantity"), order.get("lmtPrice"));
                
                // Simular cambio de estado después de un tiempo
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(2000);
                        orderStatuses.put(orderId, "FILLED");
                        logger.info("Orden {} ejecutada", orderId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                
                return orderId;
            } catch (Exception e) {
                logger.error("Error al enviar orden: {}", e.getMessage());
                return -1;
            }
        });
    }
    
    public CompletableFuture<Void> cancelOrder(int orderId) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.error("No hay conexión con IB Gateway");
                    return;
                }
                
                orderStatuses.put(orderId, "CANCELLED");
                logger.info("Orden {} cancelada", orderId);
            } catch (Exception e) {
                logger.error("Error al cancelar orden {}: {}", orderId, e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Void> requestMarketData(int reqId, Map<String, Object> contract) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.error("No hay conexión con IB Gateway");
                    return;
                }
                
                String symbol = (String) contract.get("symbol");
                logger.info("Solicitando datos de mercado para {} (ReqId: {})", symbol, reqId);
                
                // Simular datos de mercado
                if (marketData.containsKey(symbol)) {
                    double price = marketData.get(symbol);
                    logger.info("Datos de mercado para {}: Precio = {}", symbol, price);
                }
            } catch (Exception e) {
                logger.error("Error al solicitar datos de mercado: {}", e.getMessage());
            }
        });
    }
    
    public String getOrderStatus(int orderId) {
        return orderStatuses.getOrDefault(orderId, "UNKNOWN");
    }
    
    public Map<String, Double> getMarketData() {
        return new ConcurrentHashMap<>(marketData);
    }
    
    public void updateMarketData(String symbol, double price) {
        marketData.put(symbol, price);
        logger.info("Datos de mercado actualizados: {} = {}", symbol, price);
    }
    
    @PreDestroy
    public void cleanup() {
        if (connected) {
            disconnect();
        }
    }
}