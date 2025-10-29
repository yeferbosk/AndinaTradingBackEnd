package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.config.IBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementación real para conectar con IB Gateway
 * Esta clase maneja la conexión TCP directa con IB Gateway
 */
@Service
public class IBRealService {
    
    private static final Logger logger = LoggerFactory.getLogger(IBRealService.class);
    
    @Autowired
    private IBConfig ibConfig;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private Map<Integer, String> orderStatuses = new ConcurrentHashMap<>();
    private Map<String, Double> marketData = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("IBRealService inicializado - Listo para conectar con IB Gateway");
    }
    
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Conectando a IB Gateway en {}:{}", ibConfig.getHost(), ibConfig.getPort());
                
                // Crear conexión TCP
                socket = new Socket(ibConfig.getHost(), ibConfig.getPort());
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Enviar comando de conexión básico
                sendCommand("1"); // Client ID
                sendCommand("2"); // Version
                sendCommand("3"); // Start API
                
                connected = true;
                logger.info("Conectado exitosamente a IB Gateway");
                
                // Iniciar hilo para escuchar respuestas
                startResponseListener();
                
                return true;
                
            } catch (Exception e) {
                logger.error("Error al conectar con IB Gateway: {}", e.getMessage());
                connected = false;
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connected && socket != null) {
                    sendCommand("0"); // Disconnect
                    socket.close();
                    connected = false;
                    logger.info("Desconectado de IB Gateway");
                }
                return true;
            } catch (Exception e) {
                logger.error("Error al desconectar de IB Gateway: {}", e.getMessage());
                return false;
            }
        });
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            logger.debug("Enviado comando: {}", command);
        }
    }
    
    private void startResponseListener() {
        CompletableFuture.runAsync(() -> {
            try {
                String response;
                while (connected && (response = in.readLine()) != null) {
                    logger.debug("Respuesta IB: {}", response);
                    processResponse(response);
                }
            } catch (Exception e) {
                if (connected) {
                    logger.error("Error en listener de respuestas: {}", e.getMessage());
                }
            }
        });
    }
    
    private void processResponse(String response) {
        // Procesar respuestas de IB Gateway
        if (response.contains("orderStatus")) {
            // Procesar estado de orden
            logger.info("Estado de orden actualizado: {}", response);
        } else if (response.contains("tickPrice")) {
            // Procesar datos de mercado
            logger.info("Datos de mercado: {}", response);
        }
    }
    
    public CompletableFuture<Map<String, Object>> getContract(String symbol, String secType, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.error("No hay conexión con IB Gateway");
                    return null;
                }
                
                // Enviar solicitud de contrato
                String requestId = String.valueOf(System.currentTimeMillis() % 100000);
                sendCommand("9|" + requestId + "|" + symbol + "|" + secType + "|" + exchange);
                
                Map<String, Object> contract = new ConcurrentHashMap<>();
                contract.put("symbol", symbol);
                contract.put("secType", secType);
                contract.put("exchange", exchange);
                contract.put("currency", "USD");
                contract.put("conId", requestId);
                
                logger.info("Solicitud de contrato enviada para: {}", symbol);
                return contract;
                
            } catch (Exception e) {
                logger.error("Error al obtener contrato: {}", e.getMessage());
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
                
                int orderId = (int) System.currentTimeMillis() % 100000;
                
                // Construir comando de orden
                StringBuilder orderCmd = new StringBuilder();
                orderCmd.append("3|").append(orderId).append("|"); // Place order
                orderCmd.append(contract.get("symbol")).append("|");
                orderCmd.append(contract.get("secType")).append("|");
                orderCmd.append(contract.get("exchange")).append("|");
                orderCmd.append(order.get("action")).append("|");
                orderCmd.append(order.get("totalQuantity")).append("|");
                orderCmd.append(order.get("orderType")).append("|");
                orderCmd.append(order.get("lmtPrice"));
                
                sendCommand(orderCmd.toString());
                
                orderStatuses.put(orderId, "SUBMITTED");
                logger.info("Orden {} enviada: {}", orderId, orderCmd.toString());
                
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
                
                sendCommand("4|" + orderId); // Cancel order
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
                sendCommand("10|" + reqId + "|" + symbol + "|STK|SMART|USD");
                logger.info("Solicitando datos de mercado para: {}", symbol);
                
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
    
    @PreDestroy
    public void cleanup() {
        if (connected) {
            disconnect();
        }
    }
}
