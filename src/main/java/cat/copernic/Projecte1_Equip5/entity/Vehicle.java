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

    public Vehicle(String matricula, String marca, String categoria, Integer any_fabricacio, Double preu_diari, LocalDate data_alta) {
        this.matricula = matricula;
        this.marca = marca;
        this.categoria = categoria;
        this.any_fabricacio = any_fabricacio;
        this.preu_diari = preu_diari;
        this.data_alta = data_alta;
    }

    public Vehicle(String marca, String categoria, Integer any_fabricacio, Double preu_diari, LocalDate data_alta) {
        this.marca = marca;
        this.categoria = categoria;
        this.any_fabricacio = any_fabricacio;
        this.preu_diari = preu_diari;
        this.data_alta = data_alta;
    }

    public Vehicle() {
    }

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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
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

    public LocalDate getData_alta() {
        return data_alta;
    }

    public void setData_alta(LocalDate data_alta) {
        this.data_alta = data_alta;
    }
    
    
}
