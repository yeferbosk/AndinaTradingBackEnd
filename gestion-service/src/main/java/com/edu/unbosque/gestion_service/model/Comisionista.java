package com.edu.unbosque.gestion_service.model;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Comisionista")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comisionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comisionista")
    private Integer idComisionista;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "email")
    private String email;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "password")
    private String password;

    @Column(name = "estado")
    private boolean estado;

}