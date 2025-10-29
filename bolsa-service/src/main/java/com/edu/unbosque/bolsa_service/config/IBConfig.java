package com.edu.unbosque.bolsa_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ib")
public class IBConfig {
    
    private String host = "127.0.0.1";
    private int port = 7497; // Puerto por defecto para IB Gateway (7497 para paper trading, 7496 para live)
    private int clientId = 1;
    private boolean paperTrading = true;
    
    // Getters y Setters
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
    
    public boolean isPaperTrading() {
        return paperTrading;
    }
    
    public void setPaperTrading(boolean paperTrading) {
        this.paperTrading = paperTrading;
    }
}
