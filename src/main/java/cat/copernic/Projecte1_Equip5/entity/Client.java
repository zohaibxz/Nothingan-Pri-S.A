package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
//Serveix per a verificar que el numero de la tarjeta compleix el algrorisme
import org.hibernate.validator.constraints.LuhnCheck;

@Entity
@Table(name = "clients")
@PrimaryKeyJoinColumn(name = "id_client")
public class Client extends Usuari {



    @Column(name = "data_caducitat_dni", nullable = false)
    private LocalDate dataCaducitatDni;

    @Column(name = "tipus_llicencia", nullable = false, length = 30)
    private String tipusLlicencia;

    @Column(name = "data_caducitat_llicencia", nullable = false)
    private LocalDate dataCaducitatLlicencia;

    @LuhnCheck(message = "El número de targeta no és vàlid segons l'algorisme de Luhn")
    @Column(name = "num_targeta_credit", nullable = false, length = 30)
    private String numTargetaCredit;

    @Column(name = "adreca", nullable = false, length = 255)
    private String adreca;

    @Column(name = "nacionalitat", nullable = false, length = 50)
    private String nacionalitat;

    @Enumerated(EnumType.STRING)
    @Column(name = "estat_validacio", nullable = false)
    private Enums.EstatValidacio estatValidacio = Enums.EstatValidacio.Pendent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agent_validador", nullable = false)
    private Agent agentValidador;

    @Column(name = "premium", nullable = false)
    private Boolean premium = false;

    @Column(name = "descompte_premium", precision = 10, scale = 2)
    private BigDecimal descomptePremium;

    // Constructores

    // 1. Buit
    public Client() {
        super();
    }

    // 2. Amb tots els atributs (amb DNI)
    public Client(String dniUsuari, String email, String passwordHash, LocalDateTime dataCreacio, 
                  String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia, 
                  LocalDate dataCaducitatLlicencia, String numTargetaCredit, String adreca, 
                  String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador, 
                  Boolean premium, BigDecimal descomptePremium) {
        super(dniUsuari, email, passwordHash, dataCreacio, nom, cognoms);
        this.dataCaducitatDni = dataCaducitatDni;
        this.tipusLlicencia = tipusLlicencia;
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
        this.numTargetaCredit = numTargetaCredit;
        this.adreca = adreca;
        this.nacionalitat = nacionalitat;
        this.estatValidacio = estatValidacio;
        this.agentValidador = agentValidador;
        this.premium = premium;
        this.descomptePremium = descomptePremium;
    }

    // 3. Sense clau primària (sense DNI)
    public Client(String email, String passwordHash, LocalDateTime dataCreacio, 
                  String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia, 
                  LocalDate dataCaducitatLlicencia, String numTargetaCredit, String adreca, 
                  String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador, 
                  Boolean premium, BigDecimal descomptePremium) {
        super(email, passwordHash, dataCreacio, nom, cognoms);
        this.dataCaducitatDni = dataCaducitatDni;
        this.tipusLlicencia = tipusLlicencia;
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
        this.numTargetaCredit = numTargetaCredit;
        this.adreca = adreca;
        this.nacionalitat = nacionalitat;
        this.estatValidacio = estatValidacio;
        this.agentValidador = agentValidador;
        this.premium = premium;
        this.descomptePremium = descomptePremium;
    }
    
    
    // Getters y Setters

    public LocalDate getDataCaducitatDni() {
        return dataCaducitatDni;
    }

    public void setDataCaducitatDni(LocalDate dataCaducitatDni) {
        this.dataCaducitatDni = dataCaducitatDni;
    }

    public String getTipusLlicencia() {
        return tipusLlicencia;
    }

    public void setTipusLlicencia(String tipusLlicencia) {
        this.tipusLlicencia = tipusLlicencia;
    }

    public LocalDate getDataCaducitatLlicencia() {
        return dataCaducitatLlicencia;
    }

    public void setDataCaducitatLlicencia(LocalDate dataCaducitatLlicencia) {
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
    }

    public String getNumTargetaCredit() {
        return numTargetaCredit;
    }

    public void setNumTargetaCredit(String numTargetaCredit) {
        this.numTargetaCredit = numTargetaCredit;
    }

    public String getAdreca() {
        return adreca;
    }

    public void setAdreca(String adreca) {
        this.adreca = adreca;
    }

    public String getNacionalitat() {
        return nacionalitat;
    }

    public void setNacionalitat(String nacionalitat) {
        this.nacionalitat = nacionalitat;
    }

    public Enums.EstatValidacio getEstatValidacio() {
        return estatValidacio;
    }

    public void setEstatValidacio(Enums.EstatValidacio estatValidacio) {
        this.estatValidacio = estatValidacio;
    }

    public Agent getAgentValidador() {
        return agentValidador;
    }

    public void setAgentValidador(Agent agentValidador) {
        this.agentValidador = agentValidador;
    }

    public Boolean getPremium() {
        return premium;
    }

    public void setPremium(Boolean premium) {
        this.premium = premium;
    }

    public BigDecimal getDescomptePremium() {
        return descomptePremium;
    }

    public void setDescomptePremium(BigDecimal descomptePremium) {
        this.descomptePremium = descomptePremium;
    }

}
