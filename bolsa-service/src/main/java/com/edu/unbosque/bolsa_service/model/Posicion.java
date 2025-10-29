package com.edu.unbosque.bolsa_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "posicion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Posicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "simbolo", nullable = false, length = 50)
    private String simbolo;

    @Column(name = "nombre_empresa", length = 200)
    private String nombreEmpresa;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 0;

    @Column(name = "precio_promedio", nullable = false, precision = 15, scale = 4)
    private BigDecimal precioPromedio;

    @Column(name = "valor_mercado_actual", precision = 15, scale = 2)
    private BigDecimal valorMercadoActual;

    @Column(name = "ganancia_perdida", precision = 15, scale = 2)
    private BigDecimal gananciaPerdida;

    @Column(name = "porcentaje_ganancia", precision = 10, scale = 2)
    private BigDecimal porcentajeGanancia;

    @Column(name = "fecha_primera_compra")
    private LocalDateTime fechaPrimeraCompra = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        fechaPrimeraCompra = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

