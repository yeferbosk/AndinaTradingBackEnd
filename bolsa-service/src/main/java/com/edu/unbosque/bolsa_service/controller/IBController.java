package com.edu.unbosque.bolsa_service.controller;

import com.edu.unbosque.bolsa_service.service.IBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/ib")
public class IBController {
    
    @Autowired
    private IBService ibService;
    
    @PostMapping("/connect")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> connect() {
        return ibService.connect().thenApply(connected -> {
            Map<String, Object> response = new HashMap<>();
            response.put("connected", connected);
            response.put("message", connected ? "Conectado exitosamente a IB Gateway" : "Error al conectar con IB Gateway");
            return ResponseEntity.ok(response);
        });
    }
    
    @PostMapping("/disconnect")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> disconnect() {
        return ibService.disconnect().thenApply(disconnected -> {
            Map<String, Object> response = new HashMap<>();
            response.put("disconnected", disconnected);
            response.put("message", "Desconectado de IB Gateway");
            return ResponseEntity.ok(response);
        });
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("connected", ibService.isConnected());
        response.put("message", ibService.isConnected() ? "Conectado" : "Desconectado");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/contract")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getContract(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "STK") String secType,
            @RequestParam(defaultValue = "SMART") String exchange) {
        
        return ibService.getContract(symbol, secType, exchange).thenApply(contract -> {
            Map<String, Object> response = new HashMap<>();
            if (contract != null) {
                response.put("success", true);
                response.put("contract", contract);
                response.put("message", "Contrato obtenido exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Error al obtener el contrato");
            }
            return ResponseEntity.ok(response);
        });
    }
    
    @PostMapping("/order")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> placeOrder(
            @RequestParam String symbol,
            @RequestParam String action, // BUY o SELL
            @RequestParam int quantity,
            @RequestParam double price,
            @RequestParam(defaultValue = "STK") String secType,
            @RequestParam(defaultValue = "SMART") String exchange) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Crear contrato
                Map<String, Object> contract = new HashMap<>();
                contract.put("symbol", symbol);
                contract.put("secType", secType);
                contract.put("exchange", exchange);
                contract.put("currency", "USD");
                
                // Crear orden
                Map<String, Object> order = new HashMap<>();
                order.put("action", action);
                order.put("totalQuantity", quantity);
                order.put("orderType", "LMT"); // Limit order
                order.put("lmtPrice", price);
                
                // Enviar orden
                return ibService.placeOrder(contract, order).thenApply(orderId -> {
                    Map<String, Object> response = new HashMap<>();
                    if (orderId > 0) {
                        response.put("success", true);
                        response.put("orderId", orderId);
                        response.put("message", "Orden enviada exitosamente");
                    } else {
                        response.put("success", false);
                        response.put("message", "Error al enviar la orden");
                    }
                    return ResponseEntity.ok(response);
                }).join();
                
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        });
    }
    
    @PostMapping("/cancel/{orderId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> cancelOrder(@PathVariable int orderId) {
        return ibService.cancelOrder(orderId).thenApply(v -> {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("message", "Orden cancelada");
            return ResponseEntity.ok(response);
        });
    }
    
    @PostMapping("/market-data")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> requestMarketData(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "STK") String secType,
            @RequestParam(defaultValue = "SMART") String exchange,
            @RequestParam(defaultValue = "1") int reqId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> contract = new HashMap<>();
                contract.put("symbol", symbol);
                contract.put("secType", secType);
                contract.put("exchange", exchange);
                contract.put("currency", "USD");
                
                ibService.requestMarketData(reqId, contract);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("reqId", reqId);
                response.put("symbol", symbol);
                response.put("message", "Solicitud de datos de mercado enviada");
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        });
    }
    
    @GetMapping("/market-data")
    public ResponseEntity<Map<String, Object>> getMarketData() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("marketData", ibService.getMarketData());
        response.put("message", "Datos de mercado obtenidos");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/order-status/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable int orderId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("orderId", orderId);
        response.put("status", ibService.getOrderStatus(orderId));
        response.put("message", "Estado de orden obtenido");
        return ResponseEntity.ok(response);
    }
}
