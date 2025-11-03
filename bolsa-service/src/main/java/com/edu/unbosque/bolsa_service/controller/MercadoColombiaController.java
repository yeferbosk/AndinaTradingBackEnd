package com.edu.unbosque.bolsa_service.controller;

import com.edu.unbosque.bolsa_service.model.Orden;
import com.edu.unbosque.bolsa_service.model.Posicion;
import com.edu.unbosque.bolsa_service.model.Transaccion;
import com.edu.unbosque.bolsa_service.Repository.OrdenRepository;
import com.edu.unbosque.bolsa_service.service.IBService;
import com.edu.unbosque.bolsa_service.service.PaperTradingService;
import com.edu.unbosque.bolsa_service.service.IBTwsService;
import com.edu.unbosque.bolsa_service.service.OrdenComisionistaService;
import com.ib.client.Bar;
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
    
    @Autowired
    private IBTwsService ibTwsService;
    
    @Autowired
    private OrdenComisionistaService ordenComisionistaService;
    
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
     * Obtiene el precio REAL del mercado desde TWS automáticamente
     */
    @PostMapping("/paper/vender")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> venderPaper(
            @RequestParam Integer usuarioId,
            @RequestParam String simbolo,
            @RequestParam Integer cantidad) {
        
        // Usar el método que obtiene el precio real desde TWS
        return paperTradingService.venderAccionesConPrecioReal(
            usuarioId, simbolo, cantidad)
            .thenApply(ResponseEntity::ok);
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
     * Obtener portafolio completo del usuario con valor total
     * Incluye: balance disponible, valor de posiciones, valor total del portafolio, ganancia/pérdida
     */
    @GetMapping("/paper/portafolio")
    public ResponseEntity<Map<String, Object>> obtenerPortafolio(@RequestParam Integer usuarioId) {
        Map<String, Object> portafolio = paperTradingService.obtenerPortafolio(usuarioId);
        return ResponseEntity.ok(portafolio);
    }
    
    /**
     * Obtener resumen financiero claro: ganancias/pérdidas, balance total, estado
     * Muestra si está ganando o perdiendo y cuánto
     */
    @GetMapping("/paper/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumenFinanciero(@RequestParam Integer usuarioId) {
        Map<String, Object> resumen = paperTradingService.obtenerResumenFinanciero(usuarioId);
        return ResponseEntity.ok(resumen);
    }
    
    /**
     * Obtener evolución del valor de una acción durante los últimos 5 días hábiles
     * Retorna datos históricos en formato JSON para visualización en gráfico
     */
    @GetMapping("/paper/historial-precios/{simbolo}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> obtenerHistorialPrecios(
            @PathVariable String simbolo) {
        
        return ibTwsService.getHistoricalData(simbolo).thenApply(bars -> {
            Map<String, Object> response = new HashMap<>();
            
            if (bars == null || bars.isEmpty()) {
                response.put("success", false);
                response.put("message", "No se pudieron obtener datos históricos para " + simbolo);
                response.put("simbolo", simbolo);
                response.put("datos", new java.util.ArrayList<>());
                return ResponseEntity.status(404).body(response);
            }
            
            // Formatear datos para gráfico
            List<Map<String, Object>> datosGrafico = new java.util.ArrayList<>();
            for (Bar bar : bars) {
                Map<String, Object> punto = new HashMap<>();
                punto.put("fecha", bar.time()); // Fecha en formato de IB
                punto.put("precioCierre", bar.close());
                punto.put("precioApertura", bar.open());
                punto.put("precioMaximo", bar.high());
                punto.put("precioMinimo", bar.low());
                // Convertir Decimal a double de forma segura
                if (bar.volume() != null) {
                    try {
                        punto.put("volumen", Double.parseDouble(bar.volume().toString()));
                    } catch (Exception e) {
                        punto.put("volumen", 0.0);
                    }
                } else {
                    punto.put("volumen", 0.0);
                }
                datosGrafico.add(punto);
            }
            
            response.put("success", true);
            response.put("simbolo", simbolo);
            response.put("periodo", "Últimos 5 días hábiles");
            response.put("totalPuntos", datosGrafico.size());
            response.put("datos", datosGrafico);
            response.put("message", "Datos históricos obtenidos exitosamente");
            
            return ResponseEntity.ok(response);
        });
    }
    
    // ===============================================
    // ENDPOINTS PARA GESTIONAR ORDENES DE COMISIONISTA
    // ===============================================
    
    /**
     * Obtener órdenes pendientes del trader (del comisionista)
     * GET /api/mercado-colombia/paper/ordenes-comisionista/pendientes?usuarioId={id}
     */
    @GetMapping("/paper/ordenes-comisionista/pendientes")
    public ResponseEntity<List<Map<String, Object>>> obtenerOrdenesPendientes(
            @RequestParam Integer usuarioId) {
        
        List<com.edu.unbosque.bolsa_service.model.OrdenComisionista> ordenes = 
            ordenComisionistaService.obtenerOrdenesPendientesDelTrader(usuarioId);
        
        List<Map<String, Object>> ordenesResponse = ordenes.stream()
            .map(orden -> {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", orden.getId());
                mapa.put("idComisionista", orden.getIdComisionista());
                mapa.put("simbolo", orden.getSimbolo());
                mapa.put("nombreEmpresa", orden.getNombreEmpresa());
                mapa.put("cantidad", orden.getCantidad());
                mapa.put("precioLimite", orden.getPrecioLimite());
                mapa.put("mensaje", orden.getMensaje());
                mapa.put("fechaCreacion", orden.getFechaCreacion());
                mapa.put("estado", orden.getEstado());
                return mapa;
            })
            .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ordenesResponse);
    }
    
    /**
     * Aceptar una orden del comisionista (ejecuta la compra)
     * POST /api/mercado-colombia/paper/ordenes-comisionista/{ordenId}/aceptar
     */
    @PostMapping("/paper/ordenes-comisionista/{ordenId}/aceptar")
    public ResponseEntity<Map<String, Object>> aceptarOrdenComisionista(@PathVariable Long ordenId) {
        Map<String, Object> resultado = ordenComisionistaService.aceptarOrden(ordenId);

        if ((Boolean) resultado.getOrDefault("success", false)) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(resultado);
        }
    }
    
    /**
     * Rechazar una orden del comisionista
     * POST /api/mercado-colombia/paper/ordenes-comisionista/{ordenId}/rechazar
     */
    @PostMapping("/paper/ordenes-comisionista/{ordenId}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarOrdenComisionista(@PathVariable Long ordenId) {
        Map<String, Object> resultado = ordenComisionistaService.rechazarOrden(ordenId);

        if ((Boolean) resultado.getOrDefault("success", false)) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(resultado);
        }
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
