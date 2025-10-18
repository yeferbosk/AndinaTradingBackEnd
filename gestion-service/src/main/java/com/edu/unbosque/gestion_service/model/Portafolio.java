package com.edu.unbosque.gestion_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portafolio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portafolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private Integer cantidadAcciones;
}