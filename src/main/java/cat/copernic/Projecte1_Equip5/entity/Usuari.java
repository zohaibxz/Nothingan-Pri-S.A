package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuaris")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuari {

    @Id
    @Column(name = "dni_usuari", length = 10)
    private String dniUsuari;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "data_creacio", nullable = false, updatable = false)
    private LocalDateTime dataCreacio;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "cognoms", nullable = false,length = 100)
    private String cognoms;

    // Constructores

    public Usuari(String dniUsuari, String email, String passwordHash, LocalDateTime dataCreacio, String nom, String cognoms) {
        this.dniUsuari = dniUsuari;
        this.email = email;
        this.passwordHash = passwordHash;
        this.dataCreacio = dataCreacio;
        this.nom = nom;
        this.cognoms = cognoms;
    }

    public Usuari(String email, String passwordHash, LocalDateTime dataCreacio, String nom, String cognoms) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.dataCreacio = dataCreacio;
        this.nom = nom;
        this.cognoms = cognoms;
    }

    public Usuari() {
    }
    
    // Getters y Setters

    public String getDniUsuari() {
        return dniUsuari;
    }

    public void setDniUsuari(String dniUsuari) {
        this.dniUsuari = dniUsuari;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getDataCreacio() {
        return dataCreacio;
    }

    public void setDataCreacio(LocalDateTime dataCreacio) {
        this.dataCreacio = dataCreacio;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCognoms() {
        return cognoms;
    }

    public void setCognoms(String cognoms) {
        this.cognoms = cognoms;
    }
    
}