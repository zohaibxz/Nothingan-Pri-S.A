package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@PrimaryKeyJoinColumn(name = "id_agent")
public class Agent extends Client {


    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Enums.Rol rol;

    // 1. Buit
    public Agent() {
        super();
    }

    // 2. Amb tots els atributs (amb DNI)
    public Agent(String dniUsuari, String email, String passwordHash, LocalDateTime dataCreacio, 
                 String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia, 
                 LocalDate dataCaducitatLlicencia, String numTargetaCredit, String adreca, 
                 String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador, 
                 Boolean premium, BigDecimal descomptePremium, Enums.Rol rol) {
        super(dniUsuari, email, passwordHash, dataCreacio, nom, cognoms, dataCaducitatDni, 
              tipusLlicencia, dataCaducitatLlicencia, numTargetaCredit, adreca, nacionalitat, 
              estatValidacio, agentValidador, premium, descomptePremium);
        this.rol = rol;
    }

    // 3. Sense clau primària (sense DNI)
    public Agent(String email, String passwordHash, LocalDateTime dataCreacio, 
                 String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia, 
                 LocalDate dataCaducitatLlicencia, String numTargetaCredit, String adreca, 
                 String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador, 
                 Boolean premium, BigDecimal descomptePremium, Enums.Rol rol) {
        super(email, passwordHash, dataCreacio, nom, cognoms, dataCaducitatDni, 
              tipusLlicencia, dataCaducitatLlicencia, numTargetaCredit, adreca, nacionalitat, 
              estatValidacio, agentValidador, premium, descomptePremium);
        this.rol = rol;
    }

    // Getters y Setters

    public Enums.Rol getRol() {
        return rol;
    }

    public void setRol(Enums.Rol rol) {
        this.rol = rol;
    }

    
    
}
