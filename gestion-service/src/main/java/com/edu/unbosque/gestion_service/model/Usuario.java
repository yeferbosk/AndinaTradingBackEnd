package com.edu.unbosque.gestion_service.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "usuario")
@Data
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario") // este sí se mantiene con snake_case porque es el nombre en BD

    private Integer idUsuario;
    private String nombre;
    private String apellido;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El formato del correo no es válido")
    private String email;
    private String telefono;
    private String password;
    private boolean estado;
    private String rol;


}