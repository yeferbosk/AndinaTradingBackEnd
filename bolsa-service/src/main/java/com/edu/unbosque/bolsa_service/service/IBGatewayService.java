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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.*;
import java.util.Map;

/**
 * Servicio para conectar directamente con IB Gateway usando el protocolo de socket de IB
 * Este servicio se conecta al puerto 7497 (paper trading) y usa el protocolo binario de IB
 */
@Service
public class IBGatewayService {
    
    private static final Logger logger = LoggerFactory.getLogger(IBGatewayService.class);
    private static final int CLIENT_VERSION = 176; // Versión del cliente TWS API
    private static final String MIN_SERVER_VERSION = "v100..151";
    
    @Autowired
    private IBConfig ibConfig;
    
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean connected = false;
    private int nextOrderId = 1;
    private int nextRequestId = 1000;
    
    // Almacenamiento de datos en tiempo real
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private final Map<Integer, String> orderStatuses = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Double>> priceRequests = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("IBGatewayService inicializado - Configurado para {}:{}", 
                    ibConfig.getHost(), ibConfig.getPort());
    }
    
    /**
     * Conecta con IB Gateway
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connected) {
                    logger.info("Ya está conectado a IB Gateway");
                    return true;
                }
                
                logger.info("Conectando a IB Gateway en {}:{}", ibConfig.getHost(), ibConfig.getPort());
                
                // Crear socket TCP
                socket = new Socket();
                socket.connect(new InetSocketAddress(ibConfig.getHost(), ibConfig.getPort()), 10000);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                
                // Handshake inicial (protocolo IB)
                sendHandshake();
                
                // Esperar respuesta del servidor
                Thread.sleep(1000);
                
                // Iniciar listener de mensajes
                startMessageListener();
                
                connected = true;
                logger.info("✅ Conectado exitosamente a IB Gateway (Paper Trading)");
                
                // Solicitar próximo ID de orden válido
                requestNextOrderId();
                
                return true;
                
            } catch (Exception e) {
                logger.error("❌ Error al conectar con IB Gateway: {}", e.getMessage(), e);
                connected = false;
                return false;
            }
        });
    }
    
    /**
     * Envía el handshake inicial al servidor IB
     */
    private void sendHandshake() throws IOException {
        // Protocolo IB: enviar versión del cliente y client ID
        String handshake = "API\0" + MIN_SERVER_VERSION + "\0";
        out.writeBytes(handshake);
        out.flush();
        
        // Enviar client ID
        sendMessage(String.valueOf(ibConfig.getClientId()));
        
        logger.debug("Handshake enviado");
    }
    
    /**
     * Solicita el próximo ID de orden válido
     */
    private void requestNextOrderId() {
        try {
            // Mensaje tipo 8 = REQ_IDS
            sendMessage("8|1");
            logger.debug("Solicitando próximo order ID");
        } catch (Exception e) {
            logger.error("Error al solicitar order ID: {}", e.getMessage());
        }
    }
    
    /**
     * Obtiene el precio de mercado actual de un símbolo
     */
    public CompletableFuture<Double> getMarketPrice(String symbol) {
        CompletableFuture<Double> priceFuture = new CompletableFuture<>();
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.warn("No conectado a IB Gateway, intentando conectar...");
                    connect().join();
                }
                
                int reqId = nextRequestId++;
                priceRequests.put(reqId, priceFuture);
                
                // Solicitar datos de mercado (mensaje tipo 1 = REQ_MKT_DATA)
                StringBuilder msg = new StringBuilder();
                msg.append("1|");  // REQ_MKT_DATA
                msg.append(reqId).append("|");
                msg.append(symbol).append("|");
                msg.append("STK|");  // Tipo: Stock
                msg.append("|");  // Exchange (vacío = SMART)
                msg.append("USD|");  // Moneda
                msg.append("|");  // Primary exchange
                msg.append("0");  // snapshot = false
                
                sendMessage(msg.toString());
                logger.info("📊 Solicitando precio de mercado para: {}", symbol);
                
                // Timeout de 10 segundos
                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                    if (!priceFuture.isDone()) {
                        logger.warn("⏱️ Timeout al obtener precio de {}", symbol);
                        priceFuture.complete(0.0);  // Valor por defecto
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error al solicitar precio de mercado: {}", e.getMessage());
                priceFuture.complete(0.0);
            }
        });
        
        return priceFuture;
    }
    
    /**
     * Envía una orden de compra/venta al IB Gateway
     */
    public CompletableFuture<Integer> placeOrder(String symbol, String action, int quantity, double price) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.error("No conectado a IB Gateway");
                    return -1;
                }
                
                int orderId = nextOrderId++;
                
                // Construir mensaje de orden (tipo 3 = PLACE_ORDER)
                StringBuilder msg = new StringBuilder();
                msg.append("3|");  // PLACE_ORDER
                msg.append(orderId).append("|");
                msg.append(symbol).append("|");
                msg.append("STK|");  // Security type
                msg.append("|");  // Exchange (SMART)
                msg.append("USD|");  // Currency
                msg.append(action).append("|");  // BUY/SELL
                msg.append(quantity).append("|");
                msg.append("LMT|");  // Order type (LIMIT)
                msg.append(price).append("|");
                msg.append("|");  // Time in force (DAY)
                msg.append("0");  // Outside RTH
                
                sendMessage(msg.toString());
                
                orderStatuses.put(orderId, "SUBMITTED");
                logger.info("📤 Orden enviada a IB - OrderID: {} | {} {} {} @ ${}", 
                           orderId, action, quantity, symbol, price);
                
                return orderId;
                
            } catch (Exception e) {
                logger.error("Error al enviar orden: {}", e.getMessage(), e);
                return -1;
            }
        });
    }
    
    /**
     * Cancela una orden
     */
    public CompletableFuture<Void> cancelOrder(int orderId) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.error("No conectado a IB Gateway");
                    return;
                }
                
                sendMessage("4|" + orderId);  // CANCEL_ORDER
                orderStatuses.put(orderId, "CANCELLED");
                logger.info("❌ Orden cancelada: {}", orderId);
                
            } catch (Exception e) {
                logger.error("Error al cancelar orden: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Envía un mensaje al servidor IB
     */
    private synchronized void sendMessage(String message) {
        try {
            if (out != null && socket != null && !socket.isClosed()) {
                byte[] msgBytes = message.getBytes("UTF-8");
                // IB protocol: 4 bytes de longitud + mensaje
                out.writeInt(msgBytes.length);
                out.write(msgBytes);
                out.flush();
                logger.debug("→ Enviado: {}", message.substring(0, Math.min(100, message.length())));
            }
        } catch (Exception e) {
            logger.error("Error al enviar mensaje: {}", e.getMessage());
        }
    }
    
    /**
     * Inicia el listener de mensajes del servidor
     */
    private void startMessageListener() {
        CompletableFuture.runAsync(() -> {
            try {
                while (connected && socket != null && !socket.isClosed()) {
                    // Leer longitud del mensaje (4 bytes)
                    int msgLength = in.readInt();
                    
                    if (msgLength > 0 && msgLength < 1000000) {
                        byte[] msgBytes = new byte[msgLength];
                        in.readFully(msgBytes);
                        String message = new String(msgBytes, "UTF-8");
                        
                        processMessage(message);
                    }
                }
            } catch (EOFException e) {
                logger.info("Conexión cerrada por el servidor");
                connected = false;
            } catch (Exception e) {
                if (connected) {
                    logger.error("Error en message listener: {}", e.getMessage());
                    connected = false;
                }
            }
        });
    }
    
    /**
     * Procesa mensajes recibidos del servidor IB
     */
    private void processMessage(String message) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length == 0) return;
            
            String msgType = parts[0];
            logger.debug("← Recibido tipo: {} | {}", msgType, 
                        message.substring(0, Math.min(150, message.length())));
            
            switch (msgType) {
                case "1":  // TICK_PRICE
                    if (parts.length >= 4) {
                        int reqId = Integer.parseInt(parts[1]);
                        int tickType = Integer.parseInt(parts[2]);
                        double price = Double.parseDouble(parts[3]);
                        
                        // TickType 1 = BID, 2 = ASK, 4 = LAST, 9 = CLOSE
                        if (tickType == 4 || tickType == 9) {  // LAST price
                            logger.info("💰 Precio recibido: ${} (reqId: {})", price, reqId);
                            CompletableFuture<Double> future = priceRequests.remove(reqId);
                            if (future != null) {
                                future.complete(price);
                            }
                        }
                    }
                    break;
                    
                case "3":  // ORDER_STATUS
                    if (parts.length >= 3) {
                        int orderId = Integer.parseInt(parts[1]);
                        String status = parts[2];
                        orderStatuses.put(orderId, status);
                        logger.info("📋 Estado de orden {}: {}", orderId, status);
                    }
                    break;
                    
                case "9":  // NEXT_VALID_ID
                    if (parts.length >= 2) {
                        nextOrderId = Integer.parseInt(parts[1]);
                        logger.info("🔢 Próximo Order ID válido: {}", nextOrderId);
                    }
                    break;
                    
                case "4":  // ERR_MSG
                    if (parts.length >= 4) {
                        String errorMsg = parts[3];
                        logger.warn("⚠️ Mensaje de IB: {}", errorMsg);
                    }
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar mensaje: {}", e.getMessage());
        }
    }
    
    /**
     * Desconecta del IB Gateway
     */
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connected && socket != null) {
                    connected = false;
                    socket.close();
                    logger.info("Desconectado de IB Gateway");
                }
                return true;
            } catch (Exception e) {
                logger.error("Error al desconectar: {}", e.getMessage());
                return false;
            }
        });
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    public String getOrderStatus(int orderId) {
        return orderStatuses.getOrDefault(orderId, "UNKNOWN");
    }
    
    public Map<String, Double> getLastPrices() {
        return new ConcurrentHashMap<>(lastPrices);
    }
    
    @PreDestroy
    public void cleanup() {
        if (connected) {
            disconnect().join();
        }
    }
}

