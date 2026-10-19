package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "AGENT")
@PrimaryKeyJoinColumn(name = "id_agent")
public class Agent extends Usuari {

    public enum Rol {
        agent,
        administrador
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    // Constructores
    public Agent() {
        super();
    }

    public Agent(String email, String passwordHash, java.time.LocalDateTime dataCreacio, String nom, String cognoms, Rol rol) {
        super(email, passwordHash, dataCreacio, nom, cognoms);
        this.rol = rol;
    }

    // Getters y Setters
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
