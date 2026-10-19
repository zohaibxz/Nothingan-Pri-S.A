package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import cat.copernic.Projecte1_Equip5.entity.Enums;

@Entity
@Table(name = "AGENT")
@PrimaryKeyJoinColumn(name = "id_agent")
public class Agent extends Usuari {


    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Enums.Rol rol;

    // Constructores
    public Agent() {
        super();
    }

    public Agent(String email, String passwordHash, java.time.LocalDateTime dataCreacio, String nom, String cognoms) {
        super(email, passwordHash, dataCreacio, nom, cognoms);
    }

    // Getters y Setters

    public Enums.Rol getRol() {
        return rol;
    }

    public void setRol(Enums.Rol rol) {
        this.rol = rol;
    }
    
}
