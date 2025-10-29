package com.edu.unbosque.bolsa_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // COMPRA, VENTA, DEPOSITO, RETIRO

    @Column(name = "simbolo", length = 50)
    private String simbolo;

    @Column(name = "cantidad")
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 15, scale = 4)
    private BigDecimal precioUnitario;

    @Column(name = "monto_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "comision", precision = 15, scale = 2)
    private BigDecimal comision = BigDecimal.ZERO;

    @Column(name = "balance_anterior", precision = 15, scale = 2)
    private BigDecimal balanceAnterior;

    @Column(name = "balance_posterior", precision = 15, scale = 2)
    private BigDecimal balancePosterior;

    @Column(name = "fecha_transaccion")
    private LocalDateTime fechaTransaccion = LocalDateTime.now();

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @PrePersist
    protected void onCreate() {
        fechaTransaccion = LocalDateTime.now();
    }
}

