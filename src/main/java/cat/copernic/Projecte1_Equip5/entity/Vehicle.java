/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @Column(name = "matricula_vehicle", nullable = false)
    private String matricula;

    @Column(name = "marca_vehicle", nullable = false)
    private String marca;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_vehicle", nullable = false)
    private Enums.CategoriaVahicle categoria;
    
    @Column(name = "model_vehicle", nullable = false)
    private String model;

    @Column(name = "any_fabricacio_vehicle", nullable = false)
    private Integer any_fabricacio;
    
    @Column(name = "preu_diari_vehicle", nullable = false, updatable = true)
    private Double preu_diari;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estat_vehicle", nullable = false, updatable = true)
    private Enums.EstatVehicle estat = Enums.EstatVehicle.Disponible;
   
    @Column(name = "data_alts_vehicle", nullable = false)
    private LocalDate data_alta;

    
    //Constructors
    public Vehicle(String matricula, String marca, Enums.CategoriaVahicle categoria, String model, Integer any_fabricacio, Double preu_diari, LocalDate data_alta) {
        this.matricula = matricula;
        this.marca = marca;
        this.categoria = categoria;
        this.model = model;
        this.any_fabricacio = any_fabricacio;
        this.preu_diari = preu_diari;
        this.data_alta = data_alta;
    }

    public Vehicle(String marca, Enums.CategoriaVahicle categoria, String model, Integer any_fabricacio, Double preu_diari, LocalDate data_alta) {
        this.marca = marca;
        this.categoria = categoria;
        this.model = model;
        this.any_fabricacio = any_fabricacio;
        this.preu_diari = preu_diari;
        this.data_alta = data_alta;
    }

    public Vehicle() {
    }

    //Getters i setters
    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public Enums.CategoriaVahicle getCategoria() {
        return categoria;
    }

    public void setCategoria(Enums.CategoriaVahicle categoria) {
        this.categoria = categoria;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getAny_fabricacio() {
        return any_fabricacio;
    }

    public void setAny_fabricacio(Integer any_fabricacio) {
        this.any_fabricacio = any_fabricacio;
    }

    public Double getPreu_diari() {
        return preu_diari;
    }

    public void setPreu_diari(Double preu_diari) {
        this.preu_diari = preu_diari;
    }

    public Enums.EstatVehicle getEstat() {
        return estat;
    }

    public void setEstat(Enums.EstatVehicle estat) {
        this.estat = estat;
    }

    public LocalDate getData_alta() {
        return data_alta;
    }

    public void setData_alta(LocalDate data_alta) {
        this.data_alta = data_alta;
    }

    
    
    
}
