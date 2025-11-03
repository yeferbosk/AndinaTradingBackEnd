package com.edu.unbosque.bolsa_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una orden de compra enviada por un comisionista a un trader
 */
@Entity
@Table(name = "orden_comisionista")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenComisionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_comisionista", nullable = false)
    private Integer idComisionista;

    @Column(name = "id_trader", nullable = false)
    private Integer idTrader;

    @Column(name = "simbolo", nullable = false, length = 50)
    private String simbolo;

    @Column(name = "nombre_empresa")
    private String nombreEmpresa;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_limite", precision = 15, scale = 4)
    private BigDecimal precioLimite; // Opcional: precio máximo que el trader aceptaría pagar

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE_APROBACION"; // PENDIENTE_APROBACION, ACEPTADA, RECHAZADA, EJECUTADA, CANCELADA

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje; // Mensaje opcional del comisionista

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "orden_ejecutada_id")
    private Long ordenEjecutadaId; // ID de la orden ejecutada en la tabla orden (si fue aceptada)

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (estado == null) {
            estado = "PENDIENTE_APROBACION";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

