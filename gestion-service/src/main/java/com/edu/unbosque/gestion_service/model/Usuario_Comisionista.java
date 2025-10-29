package com.edu.unbosque.gestion_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Usuario_Comisionista")
@IdClass(UsuarioComisionistaId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario_Comisionista {

    @Id
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_comisionista")
    private Comisionista comisionista;


}