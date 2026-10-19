package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USUARI")
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuari {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuari")
    private Long idUsuari;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "data_creacio", nullable = false)
    private LocalDateTime dataCreacio;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "cognoms", nullable = false)
    private String cognoms;

    // Constructores
    public Usuari() {}

    public Usuari(String email, String passwordHash, LocalDateTime dataCreacio, String nom, String cognoms) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.dataCreacio = dataCreacio;
        this.nom = nom;
        this.cognoms = cognoms;
    }

    public Usuari(Long idUsuari, String email, String passwordHash, LocalDateTime dataCreacio, String nom, String cognoms) {
        this.idUsuari = idUsuari;
        this.email = email;
        this.passwordHash = passwordHash;
        this.dataCreacio = dataCreacio;
        this.nom = nom;
        this.cognoms = cognoms;
    }
    
  

    // Getters y Setters
    public Long getIdUsuari() { return idUsuari; }
    public void setIdUsuari(Long idUsuari) { this.idUsuari = idUsuari; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDateTime getDataCreacio() { return dataCreacio; }
    public void setDataCreacio(LocalDateTime dataCreacio) { this.dataCreacio = dataCreacio; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCognoms() { return cognoms; }
    public void setCognoms(String cognoms) { this.cognoms = cognoms; }
}