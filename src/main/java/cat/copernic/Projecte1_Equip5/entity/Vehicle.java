/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 *
 * @author carle
 */

@Entity
@Table(name = "vehicles")
public class Vehicle {
    
    @Id
    private String matricula;

    private String marca;

    private String categoria;

    private Integer any_fabricacio;
    
    private Double preu_diari;
    
    private enum EstatVehicle { Disponible, Llogat, Actiu };
    
    private LocalDate data_alta;
    
    
}
