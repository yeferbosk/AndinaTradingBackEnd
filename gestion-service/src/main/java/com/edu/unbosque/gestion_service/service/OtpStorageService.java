package com.edu.unbosque.gestion_service.service;


import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpStorageService {
    private final Map<String, String> otpMap = new ConcurrentHashMap<>();

    public void guardarOtp(String email, String codigo) {
        otpMap.put(email, codigo);
    }

    public String obtenerOtp(String email) {
        return otpMap.get(email);
    }

    public void eliminarOtp(String email) {
        otpMap.remove(email);
    }
}