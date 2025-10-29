package com.edu.unbosque.bolsa_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuenta_paper_trading")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPaperTrading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Integer usuarioId;

    @Column(name = "balance_inicial", precision = 15, scale = 2)
    private BigDecimal balanceInicial = BigDecimal.valueOf(100000.00);

    @Column(name = "balance_actual", precision = 15, scale = 2)
    private BigDecimal balanceActual = BigDecimal.valueOf(100000.00);

    @Column(name = "balance_disponible", precision = 15, scale = 2)
    private BigDecimal balanceDisponible = BigDecimal.valueOf(100000.00);

    @Column(name = "balance_invertido", precision = 15, scale = 2)
    private BigDecimal balanceInvertido = BigDecimal.ZERO;

    @Column(name = "ganancia_perdida_total", precision = 15, scale = 2)
    private BigDecimal gananciaPerdidaTotal = BigDecimal.ZERO;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Column(name = "activa")
    private Boolean activa = true;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

