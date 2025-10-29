package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.Repository.CuentaPaperTradingRepository;
import com.edu.unbosque.bolsa_service.Repository.PosicionRepository;
import com.edu.unbosque.bolsa_service.Repository.TransaccionRepository;
import com.edu.unbosque.bolsa_service.Repository.OrdenRepository;
import com.edu.unbosque.bolsa_service.model.CuentaPaperTrading;
import com.edu.unbosque.bolsa_service.model.Posicion;
import com.edu.unbosque.bolsa_service.model.Transaccion;
import com.edu.unbosque.bolsa_service.model.Orden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class PaperTradingService {

    private static final Logger logger = LoggerFactory.getLogger(PaperTradingService.class);

    @Autowired
    private CuentaPaperTradingRepository cuentaRepository;

    @Autowired
    private PosicionRepository posicionRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;
    
    @Autowired
    private OrdenRepository ordenRepository;
    
    @Autowired
    private IBService ibService;
    
    @Autowired
    private IBGatewayService ibGatewayService;
    
    @Autowired
    private IBTwsService ibTwsService;

    /**
     * Obtiene o crea una cuenta de paper trading para un usuario
     */
    @Transactional
    public CuentaPaperTrading obtenerOCrearCuenta(Integer usuarioId) {
        return cuentaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearNuevaCuenta(usuarioId));
    }

    /**
     * Crea una nueva cuenta de paper trading
     */
    private CuentaPaperTrading crearNuevaCuenta(Integer usuarioId) {
        CuentaPaperTrading cuenta = new CuentaPaperTrading();
        cuenta.setUsuarioId(usuarioId);
        cuenta.setBalanceInicial(BigDecimal.valueOf(100000.00));
        cuenta.setBalanceActual(BigDecimal.valueOf(100000.00));
        cuenta.setBalanceDisponible(BigDecimal.valueOf(100000.00));
        cuenta.setBalanceInvertido(BigDecimal.ZERO);
        cuenta.setGananciaPerdidaTotal(BigDecimal.ZERO);
        cuenta.setActiva(true);
        
        logger.info("Creando nueva cuenta de paper trading para usuario: {}", usuarioId);
        return cuentaRepository.save(cuenta);
    }

    /**
     * Procesa una compra de acciones obteniendo el precio REAL desde IB Gateway
     */
    @Transactional
    public CompletableFuture<Map<String, Object>> comprarAccionesConPrecioReal(Integer usuarioId, String simbolo, 
                                                                                String nombreEmpresa, Integer cantidad) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> resultado = new HashMap<>();
            
            try {
                // 1. CONECTAR A TWS con API oficial
                if (!ibTwsService.isConnected()) {
                    logger.info("Conectando a TWS con API oficial...");
                    boolean conectado = ibTwsService.connect().join();
                    if (!conectado) {
                        resultado.put("success", false);
                        resultado.put("message", "No se pudo conectar a TWS. Verifica que TWS esté corriendo y configurado.");
                        return resultado;
                    }
                    Thread.sleep(2000); // Esperar a que se estabilice la conexión
                }
                
                // 2. OBTENER PRECIO REAL desde TWS
                logger.info("🔍 Obteniendo precio real de {} desde TWS...", simbolo);
                Double precioReal = ibTwsService.getMarketPrice(simbolo).join();
                
                if (precioReal == null || precioReal <= 0) {
                    // FALLBACK: Usar precio de referencia cuando no hay suscripción de datos
                    logger.warn("⚠️ No se pudo obtener precio de TWS. Usando precio de referencia para testing.");
                    precioReal = obtenerPrecioReferencia(simbolo);
                    
                    if (precioReal == null || precioReal <= 0) {
                        resultado.put("success", false);
                        resultado.put("message", "No se pudo obtener precio para " + simbolo + ". Símbolo no soportado.");
                        return resultado;
                    }
                }
                
                BigDecimal precio = BigDecimal.valueOf(precioReal);
                logger.info("💰 Precio real obtenido desde TWS: ${} para {}", precio, simbolo);
                
                // 3. CONTINUAR CON LA COMPRA usando el precio real
                return comprarAccionesIB(usuarioId, simbolo, nombreEmpresa, cantidad, precio).join();
                
            } catch (Exception e) {
                logger.error("Error al comprar con precio real: {}", e.getMessage(), e);
                resultado.put("success", false);
                resultado.put("message", "Error: " + e.getMessage());
                return resultado;
            }
        });
    }
    
    /**
     * Procesa una compra de acciones USANDO LA API DE INTERACTIVE BROKERS (con precio conocido)
     */
    @Transactional
    public CompletableFuture<Map<String, Object>> comprarAccionesIB(Integer usuarioId, String simbolo, 
                                                                      String nombreEmpresa, Integer cantidad, 
                                                                      BigDecimal precio) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> resultado = new HashMap<>();
            
            try {
                // Obtener o crear cuenta local del usuario
                CuentaPaperTrading cuenta = obtenerOCrearCuenta(usuarioId);
                
                // Calcular costos
                BigDecimal precioTotal = precio.multiply(BigDecimal.valueOf(cantidad));
                BigDecimal comision = precioTotal.multiply(BigDecimal.valueOf(0.001)); // 0.1% de comisión
                BigDecimal costoTotal = precioTotal.add(comision);
                
                // Verificar saldo suficiente en cuenta local del usuario
                if (cuenta.getBalanceDisponible().compareTo(costoTotal) < 0) {
                    resultado.put("success", false);
                    resultado.put("message", "Saldo insuficiente. Disponible: $" + cuenta.getBalanceDisponible() 
                                + ", Necesario: $" + costoTotal);
                    return resultado;
                }
                
                // ENVIAR ORDEN A IB GATEWAY usando TWS API oficial
                if (!ibTwsService.isConnected()) {
                    logger.info("Conectando a IB Gateway con TWS API...");
                    ibTwsService.connect().join();
                    Thread.sleep(2000); // Esperar estabilización
                }
                
                // Enviar orden usando TWS API oficial
                Integer ibOrderId = ibTwsService.placeOrder(simbolo, "BUY", cantidad, precio.doubleValue()).join();
                
                if (ibOrderId <= 0) {
                    resultado.put("success", false);
                    resultado.put("message", "Error al enviar orden a Interactive Brokers");
                    return resultado;
                }
                
                logger.info("Orden enviada a IB - OrderID: {}, Usuario: {}, Símbolo: {}", 
                           ibOrderId, usuarioId, simbolo);
                
                // Guardar orden en BD con el usuarioId y cuenta_id
                Orden ordenLocal = new Orden();
                ordenLocal.setUsuarioId(usuarioId);
                ordenLocal.setCuentaId(cuenta.getId());
                ordenLocal.setSimbolo(simbolo);
                ordenLocal.setAccion("BUY");
                ordenLocal.setTipo("COMPRA");
                ordenLocal.setCantidad(cantidad.doubleValue());
                ordenLocal.setPrecio(precio.doubleValue());
                ordenLocal.setEstado("ENVIADA_IB");
                ordenLocal.setIbOrderId(ibOrderId);
                ordenLocal.setFechaCreacion(LocalDateTime.now());
                ordenLocal.setFechaActualizacion(LocalDateTime.now());
                ordenRepository.save(ordenLocal);
            
                // Actualizar o crear posición LOCAL del usuario
                Posicion posicion = posicionRepository.findByCuentaIdAndSimbolo(cuenta.getId(), simbolo)
                        .orElse(new Posicion());
                
                if (posicion.getId() == null) {
                    // Nueva posición
                    posicion.setCuentaId(cuenta.getId());
                    posicion.setSimbolo(simbolo);
                    posicion.setNombreEmpresa(nombreEmpresa);
                    posicion.setCantidad(cantidad);
                    posicion.setPrecioPromedio(precio);
                } else {
                    // Actualizar posición existente (calcular nuevo precio promedio)
                    BigDecimal valorAnterior = posicion.getPrecioPromedio()
                            .multiply(BigDecimal.valueOf(posicion.getCantidad()));
                    BigDecimal valorNuevo = precio.multiply(BigDecimal.valueOf(cantidad));
                    BigDecimal valorTotal = valorAnterior.add(valorNuevo);
                    Integer cantidadTotal = posicion.getCantidad() + cantidad;
                    BigDecimal nuevoPrecioPromedio = valorTotal.divide(BigDecimal.valueOf(cantidadTotal), 
                                                                       4, RoundingMode.HALF_UP);
                    
                    posicion.setCantidad(cantidadTotal);
                    posicion.setPrecioPromedio(nuevoPrecioPromedio);
                }
                
                posicionRepository.save(posicion);
                
                // Actualizar balance LOCAL del usuario
                BigDecimal balanceAnterior = cuenta.getBalanceActual();
                cuenta.setBalanceDisponible(cuenta.getBalanceDisponible().subtract(costoTotal));
                cuenta.setBalanceActual(cuenta.getBalanceActual().subtract(costoTotal));
                cuenta.setBalanceInvertido(cuenta.getBalanceInvertido().add(precioTotal));
                cuentaRepository.save(cuenta);
                
                // Registrar transacción
                Transaccion transaccion = new Transaccion();
                transaccion.setCuentaId(cuenta.getId());
                transaccion.setOrdenId(ordenLocal.getId());
                transaccion.setTipo("COMPRA");
                transaccion.setSimbolo(simbolo);
                transaccion.setCantidad(cantidad);
                transaccion.setPrecioUnitario(precio);
                transaccion.setMontoTotal(precioTotal);
                transaccion.setComision(comision);
                transaccion.setBalanceAnterior(balanceAnterior);
                transaccion.setBalancePosterior(cuenta.getBalanceActual());
                transaccion.setDescripcion("Compra de " + cantidad + " acciones de " + simbolo 
                                         + " a $" + precio + " c/u (IB Order: " + ibOrderId + ")");
                transaccionRepository.save(transaccion);
                
                logger.info("Compra exitosa - Usuario: {}, IB OrderID: {}, Símbolo: {}, Cantidad: {}", 
                           usuarioId, ibOrderId, simbolo, cantidad);
                
                resultado.put("success", true);
                resultado.put("message", "Compra realizada exitosamente en IB Paper Trading");
                resultado.put("ibOrderId", ibOrderId);
                resultado.put("ordenLocalId", ordenLocal.getId());
                resultado.put("transaccionId", transaccion.getId());
                resultado.put("costoTotal", costoTotal);
                resultado.put("comision", comision);
                resultado.put("balanceDisponible", cuenta.getBalanceDisponible());
                resultado.put("posicion", posicion);
                
            } catch (Exception e) {
                logger.error("Error al procesar compra en IB: {}", e.getMessage(), e);
                resultado.put("success", false);
                resultado.put("message", "Error al procesar la compra: " + e.getMessage());
            }
            
            return resultado;
        });
    }

    /**
     * Procesa una venta de acciones
     */
    @Transactional
    public Map<String, Object> venderAcciones(Integer usuarioId, String simbolo, Integer cantidad, 
                                               BigDecimal precio) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            CuentaPaperTrading cuenta = obtenerOCrearCuenta(usuarioId);
            
            // Verificar que tenga la posición
            Optional<Posicion> posicionOpt = posicionRepository.findByCuentaIdAndSimbolo(cuenta.getId(), simbolo);
            if (!posicionOpt.isPresent()) {
                resultado.put("success", false);
                resultado.put("message", "No tienes acciones de " + simbolo);
                return resultado;
            }
            
            Posicion posicion = posicionOpt.get();
            
            // Verificar cantidad suficiente
            if (posicion.getCantidad() < cantidad) {
                resultado.put("success", false);
                resultado.put("message", "Cantidad insuficiente. Tienes: " + posicion.getCantidad() 
                            + ", Intentas vender: " + cantidad);
                return resultado;
            }
            
            // Calcular montos
            BigDecimal precioTotal = precio.multiply(BigDecimal.valueOf(cantidad));
            BigDecimal comision = precioTotal.multiply(BigDecimal.valueOf(0.001)); // 0.1% de comisión
            BigDecimal ingresoNeto = precioTotal.subtract(comision);
            
            // Calcular ganancia/pérdida
            BigDecimal costoOriginal = posicion.getPrecioPromedio().multiply(BigDecimal.valueOf(cantidad));
            BigDecimal gananciaPerdida = ingresoNeto.subtract(costoOriginal);
            
            // Actualizar posición
            posicion.setCantidad(posicion.getCantidad() - cantidad);
            if (posicion.getCantidad() == 0) {
                posicionRepository.delete(posicion);
            } else {
                posicionRepository.save(posicion);
            }
            
            // Actualizar cuenta
            BigDecimal balanceAnterior = cuenta.getBalanceActual();
            cuenta.setBalanceDisponible(cuenta.getBalanceDisponible().add(ingresoNeto));
            cuenta.setBalanceActual(cuenta.getBalanceActual().add(ingresoNeto));
            cuenta.setBalanceInvertido(cuenta.getBalanceInvertido().subtract(costoOriginal));
            cuenta.setGananciaPerdidaTotal(cuenta.getGananciaPerdidaTotal().add(gananciaPerdida));
            cuentaRepository.save(cuenta);
            
            // Registrar transacción
            Transaccion transaccion = new Transaccion();
            transaccion.setCuentaId(cuenta.getId());
            transaccion.setTipo("VENTA");
            transaccion.setSimbolo(simbolo);
            transaccion.setCantidad(cantidad);
            transaccion.setPrecioUnitario(precio);
            transaccion.setMontoTotal(precioTotal);
            transaccion.setComision(comision);
            transaccion.setBalanceAnterior(balanceAnterior);
            transaccion.setBalancePosterior(cuenta.getBalanceActual());
            transaccion.setDescripcion("Venta de " + cantidad + " acciones de " + simbolo 
                                     + " a $" + precio + " c/u. Ganancia/Pérdida: $" + gananciaPerdida);
            transaccionRepository.save(transaccion);
            
            logger.info("Venta exitosa - Usuario: {}, Símbolo: {}, Cantidad: {}, Precio: ${}, G/P: ${}", 
                       usuarioId, simbolo, cantidad, precio, gananciaPerdida);
            
            resultado.put("success", true);
            resultado.put("message", "Venta realizada exitosamente");
            resultado.put("transaccionId", transaccion.getId());
            resultado.put("ingresoNeto", ingresoNeto);
            resultado.put("comision", comision);
            resultado.put("gananciaPerdida", gananciaPerdida);
            resultado.put("balanceDisponible", cuenta.getBalanceDisponible());
            
        } catch (Exception e) {
            logger.error("Error al procesar venta: {}", e.getMessage(), e);
            resultado.put("success", false);
            resultado.put("message", "Error al procesar la venta: " + e.getMessage());
        }
        
        return resultado;
    }

    /**
     * Obtiene el balance/resumen de la cuenta
     */
    public Map<String, Object> obtenerResumenCuenta(Integer usuarioId) {
        CuentaPaperTrading cuenta = obtenerOCrearCuenta(usuarioId);
        List<Posicion> posiciones = posicionRepository.findByCuentaId(cuenta.getId());
        
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("cuentaId", cuenta.getId());
        resumen.put("balanceInicial", cuenta.getBalanceInicial());
        resumen.put("balanceActual", cuenta.getBalanceActual());
        resumen.put("balanceDisponible", cuenta.getBalanceDisponible());
        resumen.put("balanceInvertido", cuenta.getBalanceInvertido());
        resumen.put("gananciaPerdidaTotal", cuenta.getGananciaPerdidaTotal());
        resumen.put("posiciones", posiciones);
        resumen.put("cantidadPosiciones", posiciones.size());
        
        return resumen;
    }

    /**
     * Obtiene el historial de transacciones
     */
    public List<Transaccion> obtenerHistorial(Integer usuarioId) {
        CuentaPaperTrading cuenta = obtenerOCrearCuenta(usuarioId);
        return transaccionRepository.findByCuentaIdOrderByFechaTransaccionDesc(cuenta.getId());
    }

    /**
     * Obtiene las posiciones activas del usuario
     */
    public List<Posicion> obtenerPosiciones(Integer usuarioId) {
        CuentaPaperTrading cuenta = obtenerOCrearCuenta(usuarioId);
        return posicionRepository.findByCuentaId(cuenta.getId());
    }
    
    /**
     * Obtiene precio de referencia para testing cuando TWS no tiene suscripción de datos
     * NOTA: Estos son precios aproximados, solo para testing
     */
    private Double obtenerPrecioReferencia(String simbolo) {
        // Precios de referencia aproximados (Octubre 2025)
        Map<String, Double> preciosReferencia = new HashMap<>();
        
        // Empresas estadounidenses
        preciosReferencia.put("AAPL", 235.50);
        preciosReferencia.put("MSFT", 425.75);
        preciosReferencia.put("GOOGL", 175.30);
        preciosReferencia.put("AMZN", 185.20);
        preciosReferencia.put("TSLA", 265.40);
        preciosReferencia.put("META", 520.15);
        preciosReferencia.put("NVDA", 880.50);
        preciosReferencia.put("SPY", 575.25);
        
        // Empresas colombianas cotizando en NYSE/mercados internacionales
        preciosReferencia.put("EC", 10.45);      // Ecopetrol (NYSE)
        preciosReferencia.put("CIB", 8.30);      // Bancolombia (NYSE)
        preciosReferencia.put("PFBCO", 52.75);   // Banco Davivienda (BVC)
        preciosReferencia.put("NUTRESA", 25.20); // Grupo Nutresa (BVC)
        preciosReferencia.put("CELSIA", 3.85);   // Celsia (BVC)
        preciosReferencia.put("ISA", 15.60);     // ISA (BVC)
        preciosReferencia.put("CEMARGOS", 7.90); // Cementos Argos (BVC)
        preciosReferencia.put("BOGOTA", 48.50);  // Banco de Bogotá (BVC)
        
        return preciosReferencia.get(simbolo);
    }
}

