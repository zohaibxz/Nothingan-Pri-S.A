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
 * Vehicle del catàleg de lloguer. Guarda les dades comercials públiques (marca,
 * model, característiques tècniques i condicions de lloguer: preu per hora,
 * límit de quilometratge, mínim i màxim de dies, fiança) i unes dades privades
 * que només pot veure el personal (comentaris de l'agent).
 *
 * La clau primària és la matrícula. L'estat inicial és Disponible i el mínim de
 * dies de lloguer és 1. Les fotos i els documents del vehicle no es guarden
 * aquí sinó a MongoDB, a {@code VehicleArxiu}, enllaçats per la matrícula.
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

    @Column(name = "preu_per_hores", nullable = false)
    private Double preu_hores;

    @Enumerated(EnumType.STRING)
    @Column(name = "estat_vehicle", nullable = false, updatable = true)
    private Enums.EstatVehicle estat = Enums.EstatVehicle.Disponible;

    @Column(name = "data_alts_vehicle", nullable = false)
    private LocalDate data_alta;

    // ---------- Dades comercials (públiques): característiques tècniques ----------
    @Enumerated(EnumType.STRING)
    @Column(name = "combustible_vehicle", nullable = false)
    private Enums.Combustible combustible;

    @Column(name = "potencia_cv_vehicle", nullable = false)
    private Integer potencia;

    @Column(name = "color_vehicle", length = 30, nullable = false)
    private String color;

    @Column(name = "places_vehicle", nullable = false)
    private Integer places;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmissio_vehicle", nullable = false)
    private Enums.Transmissio transmissio;

    // ---------- Dades comercials (públiques): condicions de lloguer ----------
    // Límit de quilometratge (km). null = sense límit.
    @Column(name = "limit_km_vehicle")
    private Integer limit_km;

    // Mínim de dies de lloguer (mai inferior a 1)
    @Column(name = "min_dies_lloguer", nullable = false)
    private Integer min_dies = 1;

    @Column(name = "max_dies_lloguer", nullable = false)
    private Integer max_dies;

    // Fiança estàndard (€)
    @Column(name = "fianca_estandard", nullable = false, precision = 10, scale = 2)
    private BigDecimal fianca;

    // ---------- Dades privades: NOMÉS les pot veure personal (agent/administrador) ----------
    // Les fotos i els documents (jpg, png, pdf, text) es desen a MongoDB (VehicleArxiu).
    @Column(name = "comentaris_privats_agent", length = 2000)
    private String comentaris_privats;

    //Constructors
    /**
     * Crea un vehicle amb totes les seves dades, inclosa la matrícula (clau
     * primària). L'estat queda Disponible i el mínim de dies, 1, que són els
     * valors per defecte.
     *
     * @param matricula matrícula del vehicle (clau primària)
     * @param marca marca del vehicle
     * @param categoria categoria (Turisme, SUV, Furgoneta...)
     * @param model model del vehicle
     * @param any_matriculacio any de matriculació
     * @param preu_hores preu de lloguer per hora
     * @param data_alta data d'alta del vehicle al sistema
     * @param combustible tipus de combustible
     * @param potencia potència en cavalls (CV)
     * @param color color del vehicle
     * @param places nombre de places
     * @param transmissio tipus de transmissió (manual o automàtic)
     * @param limit_km límit de quilometratge en km, o {@code null} si no n'hi
     * ha
     * @param max_dies màxim de dies de lloguer
     * @param fianca fiança estàndard en euros
     * @param comentaris_privats comentaris privats de l'agent (només els veu el
     * personal)
     */
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

    /**
     * Crea un vehicle sense matrícula, per a quan es tria o es genera més tard.
     * L'estat queda Disponible i el mínim de dies, 1.
     *
     * @param marca marca del vehicle
     * @param categoria categoria (Turisme, SUV, Furgoneta...)
     * @param model model del vehicle
     * @param any_matriculacio any de matriculació
     * @param preu_hores preu de lloguer per hora
     * @param data_alta data d'alta del vehicle al sistema
     * @param combustible tipus de combustible
     * @param potencia potència en cavalls (CV)
     * @param color color del vehicle
     * @param places nombre de places
     * @param transmissio tipus de transmissió (manual o automàtic)
     * @param limit_km límit de quilometratge en km, o {@code null} si no n'hi
     * ha
     * @param max_dies màxim de dies de lloguer
     * @param fianca fiança estàndard en euros
     * @param comentaris_privats comentaris privats de l'agent (només els veu el
     * personal)
     */
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

    /**
     * Constructor buit, necessari per a JPA i per omplir l'objecte des d'un
     * formulari.
     */
    public Vehicle() {
    }

    //Getters i setters
    /**
     * Retorna la matrícula del vehicle, que n'és la clau primària.
     *
     * @return matrícula del vehicle
     */
    public String getMatricula() {
        return matricula;
    }

    /**
     * Estableix la matrícula del vehicle, que n'és la clau primària.
     *
     * @param matricula matrícula del vehicle
     */
    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    /**
     * Retorna la marca del vehicle.
     *
     * @return marca del vehicle
     */
    public String getMarca() {
        return marca;
    }

    /**
     * Estableix la marca del vehicle.
     *
     * @param marca marca del vehicle
     */
    public void setMarca(String marca) {
        this.marca = marca;
    }

    /**
     * Retorna la categoria del vehicle (Turisme, SUV, Furgoneta, Esportiu o
     * Elèctric).
     *
     * @return categoria del vehicle
     */
    public Enums.CategoriaVehicle getCategoria() {
        return categoria;
    }

    /**
     * Estableix la categoria del vehicle (Turisme, SUV, Furgoneta, Esportiu o
     * Elèctric).
     *
     * @param categoria categoria del vehicle
     */
    public void setCategoria(Enums.CategoriaVehicle categoria) {
        this.categoria = categoria;
    }

    /**
     * Retorna el model del vehicle.
     *
     * @return model del vehicle
     */
    public String getModel() {
        return model;
    }

    /**
     * Estableix el model del vehicle.
     *
     * @param model model del vehicle
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Retorna l'any de matriculació del vehicle.
     *
     * @return any de matriculació
     */
    public Integer getAny_matriculacio() {
        return any_matriculacio;
    }

    /**
     * Estableix l'any de matriculació del vehicle.
     *
     * @param any_matriculacio any de matriculació
     */
    public void setAny_matriculacio(Integer any_matriculacio) {
        this.any_matriculacio = any_matriculacio;
    }

    /**
     * Retorna el preu de lloguer per hora.
     *
     * @return preu per hora
     */
    public Double getPreu_hores() {
        return preu_hores;
    }

    /**
     * Estableix el preu de lloguer per hora.
     *
     * @param preu_hores preu per hora
     */
    public void setPreu_hores(Double preu_hores) {
        this.preu_hores = preu_hores;
    }

    /**
     * Retorna l'estat del vehicle: Disponible (per defecte), Llogat o Inactiu.
     *
     * @return estat del vehicle
     */
    public Enums.EstatVehicle getEstat() {
        return estat;
    }

    /**
     * Estableix l'estat del vehicle: Disponible (per defecte), Llogat o
     * Inactiu.
     *
     * @param estat estat del vehicle
     */
    public void setEstat(Enums.EstatVehicle estat) {
        this.estat = estat;
    }

    /**
     * Retorna la data d'alta del vehicle al sistema.
     *
     * @return data d'alta
     */
    public LocalDate getData_alta() {
        return data_alta;
    }

    /**
     * Estableix la data d'alta del vehicle al sistema.
     *
     * @param data_alta data d'alta
     */
    public void setData_alta(LocalDate data_alta) {
        this.data_alta = data_alta;
    }

    /**
     * Retorna el tipus de combustible del vehicle.
     *
     * @return tipus de combustible
     */
    public Enums.Combustible getCombustible() {
        return combustible;
    }

    /**
     * Estableix el tipus de combustible del vehicle.
     *
     * @param combustible tipus de combustible
     */
    public void setCombustible(Enums.Combustible combustible) {
        this.combustible = combustible;
    }

    /**
     * Retorna la potència del vehicle en cavalls (CV).
     *
     * @return potència en CV
     */
    public Integer getPotencia() {
        return potencia;
    }

    /**
     * Estableix la potència del vehicle en cavalls (CV).
     *
     * @param potencia potència en CV
     */
    public void setPotencia(Integer potencia) {
        this.potencia = potencia;
    }

    /**
     * Retorna el color del vehicle.
     *
     * @return color del vehicle
     */
    public String getColor() {
        return color;
    }

    /**
     * Estableix el color del vehicle.
     *
     * @param color color del vehicle
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Retorna el nombre de places del vehicle.
     *
     * @return nombre de places
     */
    public Integer getPlaces() {
        return places;
    }

    /**
     * Estableix el nombre de places del vehicle.
     *
     * @param places nombre de places
     */
    public void setPlaces(Integer places) {
        this.places = places;
    }

    /**
     * Retorna el tipus de transmissió del vehicle (manual o automàtic).
     *
     * @return tipus de transmissió
     */
    public Enums.Transmissio getTransmissio() {
        return transmissio;
    }

    /**
     * Estableix el tipus de transmissió del vehicle (manual o automàtic).
     *
     * @param transmissio tipus de transmissió
     */
    public void setTransmissio(Enums.Transmissio transmissio) {
        this.transmissio = transmissio;
    }

    /**
     * Retorna el límit de quilometratge en km; és {@code null} quan no hi ha
     * límit.
     *
     * @return límit de km, o {@code null} si no n'hi ha
     */
    public Integer getLimit_km() {
        return limit_km;
    }

    /**
     * Estableix el límit de quilometratge en km; és {@code null} quan no hi ha
     * límit.
     *
     * @param limit_km límit de km, o {@code null} si no n'hi ha
     */
    public void setLimit_km(Integer limit_km) {
        this.limit_km = limit_km;
    }

    /**
     * Retorna el mínim de dies de lloguer; mai és inferior a 1.
     *
     * @return mínim de dies de lloguer
     */
    public Integer getMin_dies() {
        return min_dies;
    }

    /**
     * Estableix el mínim de dies de lloguer; mai és inferior a 1.
     *
     * @param min_dies mínim de dies de lloguer
     */
    public void setMin_dies(Integer min_dies) {
        this.min_dies = min_dies;
    }

    /**
     * Retorna el màxim de dies de lloguer.
     *
     * @return màxim de dies de lloguer
     */
    public Integer getMax_dies() {
        return max_dies;
    }

    /**
     * Estableix el màxim de dies de lloguer.
     *
     * @param max_dies màxim de dies de lloguer
     */
    public void setMax_dies(Integer max_dies) {
        this.max_dies = max_dies;
    }

    /**
     * Retorna la fiança estàndard del vehicle, en euros.
     *
     * @return fiança estàndard
     */
    public BigDecimal getFianca() {
        return fianca;
    }

    /**
     * Estableix la fiança estàndard del vehicle, en euros.
     *
     * @param fianca fiança estàndard
     */
    public void setFianca(BigDecimal fianca) {
        this.fianca = fianca;
    }

    /**
     * Retorna els comentaris privats de l'agent sobre el vehicle; només els pot
     * veure el personal (agent o administrador).
     *
     * @return comentaris privats de l'agent
     */
    public String getComentaris_privats() {
        return comentaris_privats;
    }

    /**
     * Estableix els comentaris privats de l'agent sobre el vehicle; només els
     * pot veure el personal (agent o administrador).
     *
     * @param comentaris_privats comentaris privats de l'agent
     */
    public void setComentaris_privats(String comentaris_privats) {
        this.comentaris_privats = comentaris_privats;
    }

}
