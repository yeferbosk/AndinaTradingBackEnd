package com.edu.unbosque.bolsa_service.controller;

import com.edu.unbosque.bolsa_service.model.Orden;
import com.edu.unbosque.bolsa_service.model.Posicion;
import com.edu.unbosque.bolsa_service.model.Transaccion;
import com.edu.unbosque.bolsa_service.Repository.OrdenRepository;
import com.edu.unbosque.bolsa_service.service.IBService;
import com.edu.unbosque.bolsa_service.service.PaperTradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/mercado-colombia")
public class MercadoColombiaController {
    
    @Autowired
    private IBService ibService;
    
    @Autowired
    private OrdenRepository ordenRepository;
    
    @Autowired
    private PaperTradingService paperTradingService;
    
    /**
     * Obtiene datos de mercado para Ecopetrol (EC)
     */
    @GetMapping("/ecopetrol")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> obtenerEcopetrol() {
        return obtenerDatosMercado("EC", "NYSE", "USD", "Ecopetrol");
    }
    
    /**
     * Obtiene datos de mercado para Bancolombia (CIB)
     */
    @GetMapping("/bancolombia")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> obtenerBancolombia() {
        return obtenerDatosMercado("CIB", "NYSE", "USD", "Bancolombia");
    }
    
    /**
     * Obtiene datos de mercado para Avianca (AVH)
     */
    @GetMapping("/avianca")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> obtenerAvianca() {
        return obtenerDatosMercado("AVH", "NYSE", "USD", "Avianca Holdings");
    }
    
    /**
     * Obtiene datos de mercado para cualquier acción colombiana
     */
    @GetMapping("/accion/{simbolo}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> obtenerAccion(
            @PathVariable String simbolo,
            @RequestParam(defaultValue = "NYSE") String exchange,
            @RequestParam(defaultValue = "USD") String currency) {
        return obtenerDatosMercado(simbolo, exchange, currency, simbolo);
    }
    
    /**
     * Lista todas las acciones colombianas disponibles
     */
    @GetMapping("/listado")
    public ResponseEntity<Map<String, Object>> listarAccionesColombanas() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Map<String, String>> acciones = new HashMap<>();
        
        // Ecopetrol
        Map<String, String> ecopetrol = new HashMap<>();
        ecopetrol.put("simbolo", "EC");
        ecopetrol.put("nombre", "Ecopetrol S.A.");
        ecopetrol.put("exchange", "NYSE");
        ecopetrol.put("moneda", "USD");
        ecopetrol.put("sector", "Petróleo y Gas");
        acciones.put("ecopetrol", ecopetrol);
        
        // Bancolombia
        Map<String, String> bancolombia = new HashMap<>();
        bancolombia.put("simbolo", "CIB");
        bancolombia.put("nombre", "Bancolombia S.A.");
        bancolombia.put("exchange", "NYSE");
        bancolombia.put("moneda", "USD");
        bancolombia.put("sector", "Bancario");
        acciones.put("bancolombia", bancolombia);
        
        // Avianca
        Map<String, String> avianca = new HashMap<>();
        avianca.put("simbolo", "AVH");
        avianca.put("nombre", "Avianca Holdings S.A.");
        avianca.put("exchange", "NYSE");
        avianca.put("moneda", "USD");
        avianca.put("sector", "Aerolíneas");
        acciones.put("avianca", avianca);
        
        response.put("success", true);
        response.put("acciones", acciones);
        response.put("total", acciones.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Coloca una orden para una acción colombiana (CON USUARIO ID)
     */
    @PostMapping("/orden/{simbolo}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> colocarOrden(
            @PathVariable String simbolo,
            @RequestParam Long usuarioId, // ID DEL USUARIO QUE HACE LA ORDEN
            @RequestParam String accion, // BUY o SELL
            @RequestParam int cantidad,
            @RequestParam double precio,
            @RequestParam(defaultValue = "NYSE") String exchange,
            @RequestParam(required = false) String nombreEmpresa) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificar acción válida
                if (!accion.equals("BUY") && !accion.equals("SELL")) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Acción inválida. Use BUY o SELL");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Crear contrato
                Map<String, Object> contract = new HashMap<>();
                contract.put("symbol", simbolo);
                contract.put("secType", "STK");
                contract.put("exchange", exchange);
                contract.put("currency", "USD");
                
                // Crear orden
                Map<String, Object> order = new HashMap<>();
                order.put("action", accion);
                order.put("totalQuantity", cantidad);
                order.put("orderType", "LMT");
                order.put("lmtPrice", precio);
                
                // Enviar orden a IB
                return ibService.placeOrder(contract, order).thenApply(orderId -> {
                    Map<String, Object> response = new HashMap<>();
                    
                    if (orderId > 0) {
                        // Guardar la orden en la base de datos local
                        Orden nuevaOrden = new Orden();
                        nuevaOrden.setUsuarioId((int)(long)usuarioId);  // Convertir Long a Integer
                        nuevaOrden.setSimbolo(simbolo);
                        nuevaOrden.setAccion(accion);
                        nuevaOrden.setTipo(accion.equals("BUY") ? "COMPRA" : "VENTA");
                        nuevaOrden.setCantidad((double) cantidad);
                        nuevaOrden.setPrecio(precio);
                        nuevaOrden.setEstado("EJECUTADA");
                        nuevaOrden.setIbOrderId(orderId);
                        nuevaOrden.setFechaCreacion(LocalDateTime.now());
                        nuevaOrden.setFechaActualizacion(LocalDateTime.now());
                        ordenRepository.save(nuevaOrden);
                        
                        response.put("success", true);
                        response.put("orderId", orderId);
                        response.put("ordenLocalId", nuevaOrden.getId());
                        response.put("simbolo", simbolo);
                        response.put("accion", accion);
                        response.put("cantidad", cantidad);
                        response.put("precio", precio);
                        response.put("usuarioId", usuarioId);
                        response.put("message", "Orden " + (accion.equals("BUY") ? "de compra" : "de venta") + " ejecutada exitosamente");
                    } else {
                        response.put("success", false);
                        response.put("message", "Error al enviar la orden a IB");
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
    
    // ===============================================
    // NUEVOS ENDPOINTS - PAPER TRADING MULTI-USUARIO
    // ===============================================
    
    /**
     * Comprar acciones con paper trading de IB (cuenta personal por usuario)
     * Obtiene el precio REAL del mercado desde IB Gateway y envía la orden real
     */
    @PostMapping("/paper/comprar")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> comprarPaper(
            @RequestParam Integer usuarioId,
            @RequestParam String simbolo,
            @RequestParam String nombreEmpresa,
            @RequestParam Integer cantidad) {
        
        // Usar el método que obtiene el precio real desde IB Gateway
        return paperTradingService.comprarAccionesConPrecioReal(
            usuarioId, simbolo, nombreEmpresa, cantidad)
            .thenApply(ResponseEntity::ok);
    }
    
    /**
     * Vender acciones con paper trading
     */
    @PostMapping("/paper/vender")
    public ResponseEntity<Map<String, Object>> venderPaper(
            @RequestParam Integer usuarioId,
            @RequestParam String simbolo,
            @RequestParam Integer cantidad,
            @RequestParam Double precio) {
        
        BigDecimal precioBD = BigDecimal.valueOf(precio);
        Map<String, Object> resultado = paperTradingService.venderAcciones(
            usuarioId, simbolo, cantidad, precioBD);
        
        return ResponseEntity.ok(resultado);
    }
    
    /**
     * Obtener resumen de cuenta del usuario
     */
    @GetMapping("/paper/cuenta")
    public ResponseEntity<Map<String, Object>> obtenerCuenta(@RequestParam Integer usuarioId) {
        Map<String, Object> resumen = paperTradingService.obtenerResumenCuenta(usuarioId);
        return ResponseEntity.ok(resumen);
    }
    
    /**
     * Obtener historial de transacciones del usuario
     */
    @GetMapping("/paper/historial")
    public ResponseEntity<List<Transaccion>> obtenerHistorial(@RequestParam Integer usuarioId) {
        List<Transaccion> historial = paperTradingService.obtenerHistorial(usuarioId);
        return ResponseEntity.ok(historial);
    }
    
    /**
     * Obtener posiciones activas del usuario
     */
    @GetMapping("/paper/posiciones")
    public ResponseEntity<List<Posicion>> obtenerPosiciones(@RequestParam Integer usuarioId) {
        List<Posicion> posiciones = paperTradingService.obtenerPosiciones(usuarioId);
        return ResponseEntity.ok(posiciones);
    }
    
    /**
     * Método auxiliar para obtener datos de mercado
     */
    private CompletableFuture<ResponseEntity<Map<String, Object>>> obtenerDatosMercado(
            String simbolo, String exchange, String currency, String nombre) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificar conexión
                if (!ibService.isConnected()) {
                    ibService.connect().join();
                }
                
                // Crear contrato
                Map<String, Object> contract = new HashMap<>();
                contract.put("symbol", simbolo);
                contract.put("secType", "STK");
                contract.put("exchange", exchange);
                contract.put("currency", currency);
                
                // Solicitar datos de mercado
                int reqId = (int) System.currentTimeMillis() % 100000;
                ibService.requestMarketData(reqId, contract).join();
                
                // Obtener datos actuales
                Map<String, Double> marketData = ibService.getMarketData();
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("simbolo", simbolo);
                response.put("nombre", nombre);
                response.put("exchange", exchange);
                response.put("moneda", currency);
                response.put("precio", marketData.getOrDefault(simbolo, 0.0));
                response.put("message", "Datos de mercado obtenidos");
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        });
    }
}
