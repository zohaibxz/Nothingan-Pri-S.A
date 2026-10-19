package cat.copernic.Projecte1_Equip5.entity;
import jakarta.persistence.*;


import jakarta.persistence.*;

@Entity
@Table(name = "ADMINISTRADOR")
@PrimaryKeyJoinColumn(name = "id_administrador")
public class Administrador extends Agent {

    public Administrador() {
        super();
        setRol(Rol.administrador);
    }

    public Administrador(String email, String passwordHash, java.time.LocalDateTime dataCreacio, String nom, String cognoms) {
        super(email, passwordHash, dataCreacio, nom, cognoms, Rol.administrador);
    }
}
