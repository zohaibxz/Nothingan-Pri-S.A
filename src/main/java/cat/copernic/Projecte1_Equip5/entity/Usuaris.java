package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 *
 * @author ZOHAIB
 */
@Entity
@Table(name = "usuaris")
@Inheritance(strategy = InheritanceType.JOINED) // para que hereden 
@DiscriminatorColumn(name = "tipus_usuari", 
        discriminatorType = DiscriminatorType.STRING) // Esa anotación le indica 
// a JPA/Hibernate que debe añadir una columna especial identificadora 
// (o columna discriminadora) en la base de datos para saber qué tipo exacto 
// de objeto representa cada fila cuando lea los datos.
        
public abstract class Usuaris implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "nom")
    private String nom;

    @Column(name = "cognoms")
    private String cognoms;

    public Usuaris() {
    }

        public Usuaris(Long id, String username, String password, String email, String nom, String cognoms) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.email = email;
            this.nom = nom;
            this.cognoms = cognoms;
        }

        public Usuaris(String username, String password, String email, String nom, String cognoms) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.nom = nom;
            this.cognoms = cognoms;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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
