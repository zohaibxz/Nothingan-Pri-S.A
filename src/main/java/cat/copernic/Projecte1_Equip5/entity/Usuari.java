package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// añadido para el tema del campo actiu para poder dar de baja column default true..
import org.hibernate.annotations.ColumnDefault;

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
    
    @Column(name = "actiu", nullable = false)
    // @@ColumnDefault("true") si fuera la primera que ejecutaramso el proyecto y la base de datos estuviera vacia no haria falta eso ... 
    @ColumnDefault("true") /* Por qué = true y además @ColumnDefault("true"): 
    el = true cubre los objetos nuevos creados desde Java, y por eso no tienes 
    que tocar ningún constructor. Pero tu base de datos ya tiene filas y usas ddl-auto=update. 
    Si Hibernate añade una columna NOT NULL sin valor por defecto, MySQL pone 0 (false) a todas las filas existentes. 
    Entonces todos tus usuarios quedarían dados de baja y nadie podría entrar. El @ColumnDefault hace que las filas antiguas queden como activas.*/
    private boolean actiu = true;

    // no hace falta añadir el cmapo actiu en el cosntrcutor porque luego habrai que añadir para todoas las clases hijas
    // y ademas actiu seimpre es true asi que no hace añadir a los campos hijas etc..
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
    
    // set para campo actiu 
    public void setActiu(boolean actiu) { this.actiu = actiu; }
    
    // get para campo actiu 
    public boolean isActiu() { return actiu; }
}