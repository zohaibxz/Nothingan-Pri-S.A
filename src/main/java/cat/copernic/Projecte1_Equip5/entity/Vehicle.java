/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
    private Enums.CategoriaVehicle categoria;
    
    @Column(name = "model_vehicle", nullable = false)
    private String model;

    @Column(name = "any_matriculacio_vehicle", nullable = false)
    private Integer any_matriculacio;
    
    @Column(name = "preu_per_hores", nullable = false, updatable = true)
    private Double preu_hores;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estat_vehicle", nullable = false, updatable = true)
    private Enums.EstatVehicle estat = Enums.EstatVehicle.Disponible;
   
    @Column(name = "data_alts_vehicle", nullable = false)
    private LocalDate data_alta;

    // ---------- Dades comercials (públiques): característiques tècniques ----------
    @Enumerated(EnumType.STRING)
    @Column(name = "combustible_vehicle")
    private Enums.Combustible combustible;

    @Column(name = "potencia_cv_vehicle")
    private Integer potencia;

    @Column(name = "color_vehicle", length = 30)
    private String color;

    @Column(name = "places_vehicle")
    private Integer places;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmissio_vehicle")
    private Enums.Transmissio transmissio;

    // ---------- Dades comercials (públiques): condicions de lloguer ----------
    // Límit de quilometratge (km). null = sense límit.
    @Column(name = "limit_km_vehicle")
    private Integer limit_km;

    // Mínim de dies de lloguer (mai inferior a 1)
    @Column(name = "min_dies_lloguer")
    private Integer min_dies = 1;

    @Column(name = "max_dies_lloguer")
    private Integer max_dies;

    // Fiança estàndard (€)
    @Column(name = "fianca_estandard", precision = 10, scale = 2)
    private BigDecimal fianca;

    // ---------- Dades privades: NOMÉS les pot veure personal (agent/administrador) ----------
    // Les fotos i els documents (jpg, png, pdf, text) es desen a MongoDB (VehicleArxiu).
    @Column(name = "comentaris_privats_agent", length = 2000)
    private String comentaris_privats;

    //Constructors
    public Vehicle(String matricula, String marca, Enums.CategoriaVehicle categoria, String model, Integer any_matriculacio, Double preu_hores, LocalDate data_alta, Enums.Combustible combustible, Integer potencia, String color, Integer places, Enums.Transmissio transmissio, Integer limit_km, Integer max_dies, BigDecimal fianca, String comentaris_privats) {
        this.matricula = matricula;
        this.marca = marca;
        this.categoria = categoria;
        this.model = model;
        this.any_matriculacio = any_matriculacio;
        this.preu_hores = preu_hores;
        this.data_alta = data_alta;
        this.combustible = combustible;
        this.potencia = potencia;
        this.color = color;
        this.places = places;
        this.transmissio = transmissio;
        this.limit_km = limit_km;
        this.max_dies = max_dies;
        this.fianca = fianca;
        this.comentaris_privats = comentaris_privats;
        
        
    }

    public Vehicle(String marca, Enums.CategoriaVehicle categoria, String model, Integer any_matriculacio, Double preu_hores, LocalDate data_alta, Enums.Combustible combustible, Integer potencia, String color, Integer places, Enums.Transmissio transmissio, Integer limit_km, Integer max_dies, BigDecimal fianca, String comentaris_privats) {
        this.marca = marca;
        this.categoria = categoria;
        this.model = model;
        this.any_matriculacio = any_matriculacio;
        this.preu_hores = preu_hores;
        this.data_alta = data_alta;
        this.combustible = combustible;
        this.potencia = potencia;
        this.color = color;
        this.places = places;
        this.transmissio = transmissio;
        this.limit_km = limit_km;
        this.max_dies = max_dies;
        this.fianca = fianca;
        this.comentaris_privats = comentaris_privats;
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

    public Enums.CategoriaVehicle getCategoria() {
        return categoria;
    }

    public void setCategoria(Enums.CategoriaVehicle categoria) {
        this.categoria = categoria;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getAny_matriculacio() {
        return any_matriculacio;
    }

    public void setAny_matriculacio(Integer any_matriculacio) {
        this.any_matriculacio = any_matriculacio;
    }

    public Double getPreu_hores() {
        return preu_hores;
    }

    public void setPreu_hores(Double preu_hores) {
        this.preu_hores = preu_hores;
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

    public Enums.Combustible getCombustible() {
        return combustible;
    }

    public void setCombustible(Enums.Combustible combustible) {
        this.combustible = combustible;
    }

    public Integer getPotencia() {
        return potencia;
    }

    public void setPotencia(Integer potencia) {
        this.potencia = potencia;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getPlaces() {
        return places;
    }

    public void setPlaces(Integer places) {
        this.places = places;
    }

    public Enums.Transmissio getTransmissio() {
        return transmissio;
    }

    public void setTransmissio(Enums.Transmissio transmissio) {
        this.transmissio = transmissio;
    }

    public Integer getLimit_km() {
        return limit_km;
    }

    public void setLimit_km(Integer limit_km) {
        this.limit_km = limit_km;
    }

    public Integer getMin_dies() {
        return min_dies;
    }

    public void setMin_dies(Integer min_dies) {
        this.min_dies = min_dies;
    }

    public Integer getMax_dies() {
        return max_dies;
    }

    public void setMax_dies(Integer max_dies) {
        this.max_dies = max_dies;
    }

    public BigDecimal getFianca() {
        return fianca;
    }

    public void setFianca(BigDecimal fianca) {
        this.fianca = fianca;
    }

    public String getComentaris_privats() {
        return comentaris_privats;
    }

    public void setComentaris_privats(String comentaris_privats) {
        this.comentaris_privats = comentaris_privats;
    }
    
}
    
