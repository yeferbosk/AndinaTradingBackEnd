package com.edu.unbosque.bolsa_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orden")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipo; // compra o venta

    @Column(nullable = false)
    private Double cantidad;

    @Column(nullable = false)
    private Double precio;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "empresa_id")
    private Long empresaId;  // Opcional para el nuevo sistema

    @Column(name = "usuario_id")
    private Integer usuarioId;
    
    // Campos para integración con Interactive Brokers
    @Column(name = "cuenta_id")
    private Long cuentaId; // ID de la cuenta de paper trading del usuario
    
    @Column(name = "simbolo")
    private String simbolo; // Símbolo del instrumento (ej: AAPL, MSFT)
    
    @Column(name = "accion")
    private String accion; // COMPRA o VENTA
    
    @Column(name = "estado")
    private String estado = "PENDIENTE"; // PENDIENTE, ENVIADA_IB, EJECUTADA, CANCELADA, ERROR_IB
    
    @Column(name = "ib_order_id")
    private Integer ibOrderId; // ID de la orden en Interactive Brokers
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

}
