package com.edu.unbosque.gestion_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario_comisionista")
@IdClass(UsuarioComisionistaId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario_Comisionista {

    @Id
    @ManyToOne
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario")
    private Usuario usuario;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_comisionista", referencedColumnName = "id_comisionista", 
                foreignKey = @ForeignKey(name = "FK_usuario_comisionista_comisionista"))
    private Comisionista comisionista;


}