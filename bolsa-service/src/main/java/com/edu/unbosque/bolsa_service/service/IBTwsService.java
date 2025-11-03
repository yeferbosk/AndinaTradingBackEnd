package com.edu.unbosque.bolsa_service.service;

import com.edu.unbosque.bolsa_service.config.IBConfig;
import com.ib.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Servicio REAL de Interactive Brokers usando TWS API oficial
 * Implementa EWrapper para recibir callbacks de IB Gateway
 */
@Service
public class IBTwsService implements EWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(IBTwsService.class);
    
    @Autowired
    private IBConfig ibConfig;
    
    private EClientSocket clientSocket;
    private EReaderSignal readerSignal;
    private boolean connected = false;
    private int nextOrderId = -1;
    private int nextRequestId = 1000;
    
    // Almacenamiento de datos
    private final Map<Integer, Double> lastPrices = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Double>> priceRequests = new ConcurrentHashMap<>();
    private final Map<Integer, String> orderStatuses = new ConcurrentHashMap<>();
    
    // Para datos históricos
    private final Map<Integer, CompletableFuture<List<Bar>>> historicalDataRequests = new ConcurrentHashMap<>();
    private final Map<Integer, List<Bar>> historicalDataCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        readerSignal = new EJavaSignal();
        clientSocket = new EClientSocket(this, readerSignal);
        logger.info("✅ IBTwsService inicializado con TWS API oficial");
    }
    
    /**
     * Conecta con IB Gateway usando TWS API oficial
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connected) {
                    logger.info("Ya conectado a IB Gateway");
                    return true;
                }
                
                logger.info("🔌 Conectando a IB Gateway en {}:{}", ibConfig.getHost(), ibConfig.getPort());
                
                // Conectar con IB Gateway
                clientSocket.eConnect(ibConfig.getHost(), ibConfig.getPort(), ibConfig.getClientId());
                
                if (clientSocket.isConnected()) {
                    // Iniciar el reader
                    final EReader reader = new EReader(clientSocket, readerSignal);
                    reader.start();
                    
                    // Procesar mensajes en un hilo separado
                    new Thread(() -> {
                        while (clientSocket.isConnected()) {
                            readerSignal.waitForSignal();
                            try {
                                reader.processMsgs();
                            } catch (Exception e) {
                                logger.error("Error procesando mensajes: {}", e.getMessage());
                            }
                        }
                    }).start();
                    
                    connected = true;
                    
                    // Solicitar próximo ID de orden
                    clientSocket.reqIds(-1);
                    
                    // Esperar a recibir el nextOrderId
                    Thread.sleep(2000);
                    
                    logger.info("✅ Conectado exitosamente a IB Gateway (TWS API)");
                    return true;
                } else {
                    logger.error("❌ No se pudo conectar a IB Gateway");
                    return false;
                }
                
            } catch (Exception e) {
                logger.error("❌ Error al conectar: {}", e.getMessage(), e);
                connected = false;
                return false;
            }
        });
    }
    
    /**
     * Obtiene el precio de mercado actual
     */
    public CompletableFuture<Double> getMarketPrice(String symbol) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!isConnected()) {
                    connect().join();
                }
                
                int reqId = nextRequestId++;
                priceRequests.put(reqId, future);
                
                // Crear contrato
                Contract contract = new Contract();
                contract.symbol(symbol);
                contract.secType("STK");
                contract.currency("USD");
                contract.exchange("SMART");
                
                // Solicitar datos de mercado DELAYED (gratis para paper trading)
                // El parámetro "236" solicita datos delayed frozen (snapshot delayed)
                clientSocket.reqMarketDataType(3); // 3 = Delayed data
                clientSocket.reqMktData(reqId, contract, "", false, false, null);
                logger.info("📊 Solicitando precio DELAYED de {} con reqId: {}", symbol, reqId);
                
                // Timeout de 10 segundos
                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                    if (!future.isDone()) {
                        logger.warn("⏱️ Timeout al obtener precio de {}", symbol);
                        clientSocket.cancelMktData(reqId);
                        priceRequests.remove(reqId);
                        future.complete(0.0);
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error al solicitar precio: {}", e.getMessage());
                future.complete(0.0);
            }
        });
        
        return future;
    }
    
    /**
     * Envía una orden de compra/venta
     */
    public CompletableFuture<Integer> placeOrder(String symbol, String action, int quantity, double price) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected() || nextOrderId < 0) {
                    logger.error("No conectado o nextOrderId no válido");
                    return -1;
                }
                
                int orderId = nextOrderId++;
                
                // Crear contrato
                Contract contract = new Contract();
                contract.symbol(symbol);
                contract.secType("STK");
                contract.currency("USD");
                contract.exchange("SMART");
                
                // Crear orden
                Order order = new Order();
                order.action(action); // BUY o SELL
                order.totalQuantity(Decimal.get(quantity));
                order.orderType("LMT"); // Limit order
                order.lmtPrice(price);
                
                // Enviar orden
                clientSocket.placeOrder(orderId, contract, order);
                
                orderStatuses.put(orderId, "SUBMITTED");
                logger.info("📤 Orden enviada - OrderID: {} | {} {} {} @ ${}", 
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
                if (isConnected()) {
                    // En API 10.37+, cancelOrder acepta un objeto OrderCancel
                    OrderCancel orderCancel = new OrderCancel();
                    orderCancel.manualOrderCancelTime("");
                    clientSocket.cancelOrder(orderId, orderCancel);
                    orderStatuses.put(orderId, "CANCELLED");
                    logger.info("❌ Orden {} cancelada", orderId);
                }
            } catch (Exception e) {
                logger.error("Error al cancelar orden: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Obtiene datos históricos de precios para los últimos 5 días hábiles
     * @param symbol Símbolo de la acción
     * @return CompletableFuture con lista de barras (Bar) con datos históricos
     */
    public CompletableFuture<List<Bar>> getHistoricalData(String symbol) {
        CompletableFuture<List<Bar>> future = new CompletableFuture<>();
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!isConnected()) {
                    connect().join();
                }
                
                int reqId = nextRequestId++;
                historicalDataRequests.put(reqId, future);
                historicalDataCache.put(reqId, new java.util.ArrayList<>());
                
                // Crear contrato
                Contract contract = new Contract();
                contract.symbol(symbol);
                contract.secType("STK");
                contract.currency("USD");
                contract.exchange("SMART");
                
                // Solicitar datos históricos: 5 días hábiles, barras diarias
                // Formato: "5 D" = 5 días, "1 day" = barras diarias
                String duration = "5 D";
                String barSize = "1 day";
                String whatToShow = "TRADES"; // Precio de cierre
                int useRTH = 1; // Regular Trading Hours
                
                clientSocket.reqHistoricalData(reqId, contract, "", duration, barSize, 
                                                whatToShow, useRTH, 1, false, null);
                
                logger.info("📊 Solicitando datos históricos de {} con reqId: {} | Duración: {} | Bar size: {}", 
                           symbol, reqId, duration, barSize);
                
                // Timeout de 30 segundos
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                    if (!future.isDone()) {
                        logger.warn("⏱️ Timeout al obtener datos históricos de {}", symbol);
                        clientSocket.cancelHistoricalData(reqId);
                        historicalDataRequests.remove(reqId);
                        historicalDataCache.remove(reqId);
                        future.complete(new java.util.ArrayList<>());
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error al solicitar datos históricos: {}", e.getMessage(), e);
                future.complete(new java.util.ArrayList<>());
            }
        });
        
        return future;
    }
    
    public boolean isConnected() {
        return connected && clientSocket != null && clientSocket.isConnected();
    }
    
    public String getOrderStatus(int orderId) {
        return orderStatuses.getOrDefault(orderId, "UNKNOWN");
    }
    
    @PreDestroy
    public void cleanup() {
        if (connected && clientSocket != null) {
            clientSocket.eDisconnect();
            connected = false;
            logger.info("Desconectado de IB Gateway");
        }
    }
    
    // ============================================
    // IMPLEMENTACIÓN DE CALLBACKS DE EWrapper
    // ============================================
    
    @Override
    public void nextValidId(int orderId) {
        nextOrderId = orderId;
        logger.info("🔢 Próximo Order ID válido: {}", nextOrderId);
    }
    
    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        // field: 1=BID, 2=ASK, 4=LAST, 6=HIGH, 7=LOW, 9=CLOSE
        if (field == 4 || field == 9) { // LAST o CLOSE price
            logger.info("💰 Precio recibido - ReqID: {} | Precio: ${}", tickerId, price);
            lastPrices.put(tickerId, price);
            
            CompletableFuture<Double> future = priceRequests.remove(tickerId);
            if (future != null && !future.isDone()) {
                future.complete(price);
                // Cancelar suscripción de datos de mercado
                clientSocket.cancelMktData(tickerId);
            }
        }
    }
    
    // Implementación de orderStatus con firma de API 10.37
    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                          double avgFillPrice, long permId, int parentId, double lastFillPrice,
                          int clientId, String whyHeld, double mktCapPrice) {
        orderStatuses.put(orderId, status);
        logger.info("📋 Estado de orden {} actualizado: {} | Ejecutadas: {} | Restantes: {}", 
                   orderId, status, filled, remaining);
    }
    
    // Implementación de error con 4 parámetros
    // @Override - Comentado porque la firma puede ser diferente en API 10.37
    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        handleError(id, errorCode, errorMsg);
    }
    
    // Sobrecarga para 3 parámetros (compatibilidad)
    public void error(int id, int errorCode, String errorMsg) {
        handleError(id, errorCode, errorMsg);
    }
    
    // Método común para manejar errores
    private void handleError(int id, int errorCode, String errorMsg) {
        if (errorCode == 2104 || errorCode == 2106 || errorCode == 2158) {
            // Mensajes informativos, no son errores reales
            logger.debug("Info IB ({}): {}", errorCode, errorMsg);
        } else if (errorCode >= 1000 && errorCode < 2000) {
            // Errores del sistema
            logger.error("⚠️ Error IB ({}) - ID: {} | {}", errorCode, id, errorMsg);
        } else {
            logger.warn("⚠️ Mensaje IB ({}) - ID: {} | {}", errorCode, id, errorMsg);
        }
    }
    
    @Override
    public void error(Exception e) {
        logger.error("❌ Exception de IB: {}", e.getMessage(), e);
    }
    
    @Override
    public void error(String str) {
        logger.error("❌ Error de IB: {}", str);
    }
    
    @Override
    public void error(int reqId, long orderId, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        logger.error("❌ Error IB - ReqID: {}, OrderID: {}, Code: {} | {} | JSON: {}", reqId, orderId, errorCode, errorMsg, advancedOrderRejectJson);
    }
    
    @Override
    public void connectionClosed() {
        connected = false;
        logger.warn("⚠️ Conexión con IB Gateway cerrada");
    }
    
    @Override
    public void connectAck() {
        logger.info("✅ Conexión con IB Gateway confirmada");
    }
    
    // Métodos no implementados (requeridos por EWrapper)
    @Override public void tickSize(int tickerId, int field, Decimal size) {}
    @Override public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {}
    @Override public void tickGeneric(int tickerId, int tickType, double value) {}
    @Override public void tickString(int tickerId, int tickType, String value) {}
    @Override public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate) {}
    @Override public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {}
    @Override public void openOrderEnd() {}
    @Override public void updateAccountValue(String key, String value, String currency, String accountName) {}
    @Override public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {}
    @Override public void updateAccountTime(String timeStamp) {}
    @Override public void accountDownloadEnd(String accountName) {}
    @Override public void contractDetails(int reqId, ContractDetails contractDetails) {}
    @Override public void bondContractDetails(int reqId, ContractDetails contractDetails) {}
    @Override public void contractDetailsEnd(int reqId) {}
    @Override public void execDetails(int reqId, Contract contract, Execution execution) {}
    @Override public void execDetailsEnd(int reqId) {}
    @Override public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {}
    @Override public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {}
    @Override public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {}
    @Override public void managedAccounts(String accountsList) { logger.info("📊 Cuentas disponibles: {}", accountsList); }
    @Override public void receiveFA(int faDataType, String xml) {}
    @Override 
    public void historicalData(int reqId, Bar bar) {
        // Capturar cada barra de datos históricos
        List<Bar> bars = historicalDataCache.get(reqId);
        if (bars != null && bar != null) {
            bars.add(bar);
            logger.debug("📊 Barra histórica recibida - ReqID: {} | Fecha: {} | Close: ${}", 
                        reqId, bar.time(), bar.close());
        }
    }
    
    @Override 
    public void historicalDataUpdate(int reqId, Bar bar) {
        // Actualizaciones en tiempo real de datos históricos (no usado para datos pasados)
        logger.debug("📊 Actualización histórica - ReqID: {}", reqId);
    }
    
    @Override 
    public void historicalDataEnd(int reqId, String startDate, String endDate) {
        // Cuando termina la solicitud de datos históricos, completar el futuro
        CompletableFuture<List<Bar>> future = historicalDataRequests.remove(reqId);
        List<Bar> bars = historicalDataCache.remove(reqId);
        
        if (future != null && !future.isDone()) {
            if (bars != null && !bars.isEmpty()) {
                logger.info("✅ Datos históricos completados - ReqID: {} | Barras: {} | Periodo: {} a {}", 
                           reqId, bars.size(), startDate, endDate);
                future.complete(bars);
            } else {
                logger.warn("⚠️ No se recibieron datos históricos para ReqID: {}", reqId);
                future.complete(new java.util.ArrayList<>());
            }
        }
    }
    @Override public void scannerParameters(String xml) {}
    @Override public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {}
    @Override public void scannerDataEnd(int reqId) {}
    @Override public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {}
    @Override public void currentTime(long time) {}
    @Override public void currentTimeInMillis(long timeInMillis) {}
    @Override public void fundamentalData(int reqId, String data) {}
    @Override public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {}
    @Override public void tickSnapshotEnd(int reqId) {}
    @Override public void marketDataType(int reqId, int marketDataType) {}
    // @Override public void commissionReport(CommissionReport commissionReport) {}
    @Override public void position(String account, Contract contract, Decimal pos, double avgCost) {}
    @Override public void positionEnd() {}
    @Override public void accountSummary(int reqId, String account, String tag, String value, String currency) {}
    @Override public void accountSummaryEnd(int reqId) {}
    @Override public void verifyMessageAPI(String apiData) {}
    @Override public void verifyCompleted(boolean isSuccessful, String errorText) {}
    @Override public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) {}
    @Override public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {}
    @Override public void displayGroupList(int reqId, String groups) {}
    @Override public void displayGroupUpdated(int reqId, String contractInfo) {}
    @Override public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {}
    @Override public void positionMultiEnd(int reqId) {}
    @Override public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {}
    @Override public void accountUpdateMultiEnd(int reqId) {}
    @Override public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {}
    @Override public void securityDefinitionOptionalParameterEnd(int reqId) {}
    @Override public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {}
    @Override public void familyCodes(FamilyCode[] familyCodes) {}
    @Override public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {}
    @Override public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {}
    @Override public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline, String extraData) {}
    @Override public void smartComponents(int reqId, Map<Integer, Map.Entry<String, Character>> theMap) {}
    @Override public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {}
    @Override public void newsProviders(NewsProvider[] newsProviders) {}
    @Override public void newsArticle(int requestId, int articleType, String articleText) {}
    @Override public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {}
    @Override public void historicalNewsEnd(int requestId, boolean hasMore) {}
    @Override public void headTimestamp(int reqId, String headTimestamp) {}
    @Override public void histogramData(int reqId, List<HistogramEntry> items) {}
    @Override public void rerouteMktDataReq(int reqId, int conId, String exchange) {}
    @Override public void rerouteMktDepthReq(int reqId, int conId, String exchange) {}
    @Override public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {}
    @Override public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {}
    @Override public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {}
    @Override public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean done) {}
    @Override public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {}
    @Override public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {}
    @Override public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions) {}
    @Override public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {}
    @Override public void tickByTickMidPoint(int reqId, long time, double midPoint) {}
    @Override public void orderBound(long orderId, int apiClientId, int apiOrderId) {}
    @Override public void commissionAndFeesReport(CommissionAndFeesReport commissionAndFeesReport) {}
    @Override public void completedOrder(Contract contract, Order order, OrderState orderState) {}
    @Override public void completedOrdersEnd() {}
    @Override public void replaceFAEnd(int reqId, String text) {}
    @Override public void wshMetaData(int reqId, String dataJson) {}
    @Override public void wshEventData(int reqId, String dataJson) {}
    @Override public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {}
    @Override public void userInfo(int reqId, String whiteBrandingId) {}
    
    // Métodos nuevos de API 10.37 con ProtoBuf
    @Override public void marketDepthExchangesProtoBuf(com.ib.client.protobuf.MarketDepthExchangesProto.MarketDepthExchanges exchanges) {}
    @Override public void displayGroupUpdatedProtoBuf(com.ib.client.protobuf.DisplayGroupUpdatedProto.DisplayGroupUpdated displayGroupUpdated) {}
    @Override public void displayGroupListProtoBuf(com.ib.client.protobuf.DisplayGroupListProto.DisplayGroupList displayGroupList) {}
    @Override public void verifyCompletedProtoBuf(com.ib.client.protobuf.VerifyCompletedProto.VerifyCompleted verifyCompleted) {}
    @Override public void verifyMessageApiProtoBuf(com.ib.client.protobuf.VerifyMessageApiProto.VerifyMessageApi verifyMessageApi) {}
    @Override public void receiveFAProtoBuf(com.ib.client.protobuf.ReceiveFAProto.ReceiveFA receiveFA) {}
    @Override public void scannerParametersProtoBuf(com.ib.client.protobuf.ScannerParametersProto.ScannerParameters scannerParameters) {}
    @Override public void newsProvidersProtoBuf(com.ib.client.protobuf.NewsProvidersProto.NewsProviders newsProviders) {}
    @Override public void currentTimeInMillisProtoBuf(com.ib.client.protobuf.CurrentTimeInMillisProto.CurrentTimeInMillis currentTimeInMillis) {}
    @Override public void currentTimeProtoBuf(com.ib.client.protobuf.CurrentTimeProto.CurrentTime currentTime) {}
    @Override public void managedAccountsProtoBuf(com.ib.client.protobuf.ManagedAccountsProto.ManagedAccounts managedAccounts) {}
    @Override public void nextValidIdProtoBuf(com.ib.client.protobuf.NextValidIdProto.NextValidId nextValidId) { nextOrderId = nextValidId.getOrderId(); logger.info("🔢 Próximo Order ID válido (ProtoBuf): {}", nextOrderId); }
    @Override public void userInfoProtoBuf(com.ib.client.protobuf.UserInfoProto.UserInfo userInfo) {}
    @Override public void rerouteMarketDepthRequestProtoBuf(com.ib.client.protobuf.RerouteMarketDepthRequestProto.RerouteMarketDepthRequest rerouteMarketDepthRequest) {}
    @Override public void rerouteMarketDataRequestProtoBuf(com.ib.client.protobuf.RerouteMarketDataRequestProto.RerouteMarketDataRequest rerouteMarketDataRequest) {}
    @Override public void historicalScheduleProtoBuf(com.ib.client.protobuf.HistoricalScheduleProto.HistoricalSchedule historicalSchedule) {}
    @Override public void wshMetaDataProtoBuf(com.ib.client.protobuf.WshMetaDataProto.WshMetaData wshMetaData) {}
    @Override public void wshEventDataProtoBuf(com.ib.client.protobuf.WshEventDataProto.WshEventData wshEventData) {}
    @Override public void marketRuleProtoBuf(com.ib.client.protobuf.MarketRuleProto.MarketRule marketRule) {}
    @Override public void smartComponentsProtoBuf(com.ib.client.protobuf.SmartComponentsProto.SmartComponents smartComponents) {}
    @Override public void symbolSamplesProtoBuf(com.ib.client.protobuf.SymbolSamplesProto.SymbolSamples symbolSamples) {}
    @Override public void familyCodesProtoBuf(com.ib.client.protobuf.FamilyCodesProto.FamilyCodes familyCodes) {}
    @Override public void historicalTicksProtoBuf(com.ib.client.protobuf.HistoricalTicksProto.HistoricalTicks historicalTicks) {}
    @Override public void historicalTicksBidAskProtoBuf(com.ib.client.protobuf.HistoricalTicksBidAskProto.HistoricalTicksBidAsk historicalTicksBidAsk) {}
    @Override public void historicalTicksLastProtoBuf(com.ib.client.protobuf.HistoricalTicksLastProto.HistoricalTicksLast historicalTicksLast) {}
    @Override public void softDollarTiersProtoBuf(com.ib.client.protobuf.SoftDollarTiersProto.SoftDollarTiers softDollarTiers) {}
    @Override public void secDefOptParameterEndProtoBuf(com.ib.client.protobuf.SecDefOptParameterEndProto.SecDefOptParameterEnd secDefOptParameterEnd) {}
    @Override public void secDefOptParameterProtoBuf(com.ib.client.protobuf.SecDefOptParameterProto.SecDefOptParameter secDefOptParameter) {}
    @Override public void commissionAndFeesReportProtoBuf(com.ib.client.protobuf.CommissionAndFeesReportProto.CommissionAndFeesReport commissionAndFeesReport) {}
    @Override public void replaceFAEndProtoBuf(com.ib.client.protobuf.ReplaceFAEndProto.ReplaceFAEnd replaceFAEnd) {}
    @Override public void completedOrderProtoBuf(com.ib.client.protobuf.CompletedOrderProto.CompletedOrder completedOrder) {}
    @Override public void completedOrdersEndProtoBuf(com.ib.client.protobuf.CompletedOrdersEndProto.CompletedOrdersEnd completedOrdersEnd) {}
    @Override public void pnlProtoBuf(com.ib.client.protobuf.PnLProto.PnL pnl) {}
    @Override public void pnlSingleProtoBuf(com.ib.client.protobuf.PnLSingleProto.PnLSingle pnlSingle) {}
    @Override public void fundamentalsDataProtoBuf(com.ib.client.protobuf.FundamentalsDataProto.FundamentalsData fundamentalsData) {}
    @Override public void scannerDataProtoBuf(com.ib.client.protobuf.ScannerDataProto.ScannerData scannerData) {}
    @Override public void historicalDataProtoBuf(com.ib.client.protobuf.HistoricalDataProto.HistoricalData historicalData) {}
    @Override public void historicalDataEndProtoBuf(com.ib.client.protobuf.HistoricalDataEndProto.HistoricalDataEnd historicalDataEnd) {}
    @Override public void headTimestampProtoBuf(com.ib.client.protobuf.HeadTimestampProto.HeadTimestamp headTimestamp) {}
    @Override public void histogramDataProtoBuf(com.ib.client.protobuf.HistogramDataProto.HistogramData histogramData) {}
    @Override public void tickNewsProtoBuf(com.ib.client.protobuf.TickNewsProto.TickNews tickNews) {}
    @Override public void newsArticleProtoBuf(com.ib.client.protobuf.NewsArticleProto.NewsArticle newsArticle) {}
    @Override public void historicalNewsProtoBuf(com.ib.client.protobuf.HistoricalNewsProto.HistoricalNews historicalNews) {}
    @Override public void historicalNewsEndProtoBuf(com.ib.client.protobuf.HistoricalNewsEndProto.HistoricalNewsEnd historicalNewsEnd) {}
    @Override public void tickReqParamsProtoBuf(com.ib.client.protobuf.TickReqParamsProto.TickReqParams tickReqParams) {}
    @Override public void updateNewsBulletinProtoBuf(com.ib.client.protobuf.NewsBulletinProto.NewsBulletin newsBulletin) {}
    @Override public void tickByTickDataProtoBuf(com.ib.client.protobuf.TickByTickDataProto.TickByTickData tickByTickData) {}
    @Override public void orderBoundProtoBuf(com.ib.client.protobuf.OrderBoundProto.OrderBound orderBound) {}
    @Override public void realTimeBarTickProtoBuf(com.ib.client.protobuf.RealTimeBarTickProto.RealTimeBarTick realTimeBarTick) {}
    @Override public void accountSummaryProtoBuf(com.ib.client.protobuf.AccountSummaryProto.AccountSummary accountSummary) {}
    @Override public void accountSummaryEndProtoBuf(com.ib.client.protobuf.AccountSummaryEndProto.AccountSummaryEnd accountSummaryEnd) {}
    @Override public void accountUpdateMultiProtoBuf(com.ib.client.protobuf.AccountUpdateMultiProto.AccountUpdateMulti accountUpdateMulti) {}
    @Override public void accountUpdateMultiEndProtoBuf(com.ib.client.protobuf.AccountUpdateMultiEndProto.AccountUpdateMultiEnd accountUpdateMultiEnd) {}
    @Override public void positionProtoBuf(com.ib.client.protobuf.PositionProto.Position position) {}
    @Override public void positionEndProtoBuf(com.ib.client.protobuf.PositionEndProto.PositionEnd positionEnd) {}
    @Override public void positionMultiProtoBuf(com.ib.client.protobuf.PositionMultiProto.PositionMulti positionMulti) {}
    @Override public void positionMultiEndProtoBuf(com.ib.client.protobuf.PositionMultiEndProto.PositionMultiEnd positionMultiEnd) {}
    @Override public void historicalDataUpdateProtoBuf(com.ib.client.protobuf.HistoricalDataUpdateProto.HistoricalDataUpdate historicalDataUpdate) {}
    @Override public void accountDataEndProtoBuf(com.ib.client.protobuf.AccountDataEndProto.AccountDataEnd accountDataEnd) {}
    @Override public void openOrderProtoBuf(com.ib.client.protobuf.OpenOrderProto.OpenOrder openOrder) {}
    @Override public void orderStatusProtoBuf(com.ib.client.protobuf.OrderStatusProto.OrderStatus orderStatus) {}
    @Override public void updatePortfolioProtoBuf(com.ib.client.protobuf.PortfolioValueProto.PortfolioValue portfolioValue) {}
    @Override public void tickPriceProtoBuf(com.ib.client.protobuf.TickPriceProto.TickPrice tickPrice) {}
    @Override public void tickSizeProtoBuf(com.ib.client.protobuf.TickSizeProto.TickSize tickSize) {}
    @Override public void tickStringProtoBuf(com.ib.client.protobuf.TickStringProto.TickString tickString) {}
    @Override public void tickGenericProtoBuf(com.ib.client.protobuf.TickGenericProto.TickGeneric tickGeneric) {}
    @Override public void tickOptionComputationProtoBuf(com.ib.client.protobuf.TickOptionComputationProto.TickOptionComputation tickOptionComputation) {}
    @Override public void tickSnapshotEndProtoBuf(com.ib.client.protobuf.TickSnapshotEndProto.TickSnapshotEnd tickSnapshotEnd) {}
    @Override public void updateAccountTimeProtoBuf(com.ib.client.protobuf.AccountUpdateTimeProto.AccountUpdateTime accountUpdateTime) {}
    @Override public void updateAccountValueProtoBuf(com.ib.client.protobuf.AccountValueProto.AccountValue accountValue) {}
    @Override public void marketDataTypeProtoBuf(com.ib.client.protobuf.MarketDataTypeProto.MarketDataType marketDataType) {}
    @Override public void updateMarketDepthL2ProtoBuf(com.ib.client.protobuf.MarketDepthL2Proto.MarketDepthL2 marketDepthL2) {}
    @Override public void updateMarketDepthProtoBuf(com.ib.client.protobuf.MarketDepthProto.MarketDepth marketDepth) {}
    @Override public void contractDataEndProtoBuf(com.ib.client.protobuf.ContractDataEndProto.ContractDataEnd contractDataEnd) {}
    @Override public void bondContractDataProtoBuf(com.ib.client.protobuf.ContractDataProto.ContractData contractData) {}
    @Override public void contractDataProtoBuf(com.ib.client.protobuf.ContractDataProto.ContractData contractData) {}
    @Override public void execDetailsEndProtoBuf(com.ib.client.protobuf.ExecutionDetailsEndProto.ExecutionDetailsEnd executionDetailsEnd) {}
    @Override public void execDetailsProtoBuf(com.ib.client.protobuf.ExecutionDetailsProto.ExecutionDetails executionDetails) {}
    @Override public void errorProtoBuf(com.ib.client.protobuf.ErrorMessageProto.ErrorMessage errorMessage) {}
    @Override public void openOrdersEndProtoBuf(com.ib.client.protobuf.OpenOrdersEndProto.OpenOrdersEnd openOrdersEnd) {}
}

