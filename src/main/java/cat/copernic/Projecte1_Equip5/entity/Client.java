package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "CLIENT")
@PrimaryKeyJoinColumn(name = "id_client")
public class Client extends Usuari {

    public enum EstatValidacio {
        pendent,
        validat,
        rebutjat
    }

    @Column(name = "dni", nullable = false, unique = true)
    private String dni;

    @Column(name = "data_caducitat_dni")
    private LocalDate dataCaducitatDni;

    @Column(name = "tipus_llicencia")
    private String tipusLlicencia;

    @Column(name = "data_caducitat_llicencia")
    private LocalDate dataCaducitatLlicencia;

    @Column(name = "num_targeta_credit")
    private String numTargetaCredit;

    @Column(name = "adreca")
    private String adreca;

    @Column(name = "nacionalitat")
    private String nacionalitat;

    @Enumerated(EnumType.STRING)
    @Column(name = "estat_validacio")
    private EstatValidacio estatValidacio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agent_validador")
    private Agent agentValidador;

    @Column(name = "premium")
    private Boolean premium;

    @Column(name = "descompte_premium", precision = 10, scale = 2)
    private BigDecimal descomptePremium;

    // Constructores
    public Client() {
        super();
    }

    public Client(String dni, LocalDate dataCaducitatDni, String tipusLlicencia, LocalDate dataCaducitatLlicencia, String numTargetaCredit, String adreca, String nacionalitat, EstatValidacio estatValidacio, Agent agentValidador, Boolean premium, BigDecimal descomptePremium) {
        this.dni = dni;
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
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public LocalDate getDataCaducitatDni() { return dataCaducitatDni; }
    public void setDataCaducitatDni(LocalDate dataCaducitatDni) { this.dataCaducitatDni = dataCaducitatDni; }

    public String getTipusLlicencia() { return tipusLlicencia; }
    public void setTipusLlicencia(String tipusLlicencia) { this.tipusLlicencia = tipusLlicencia; }

    public LocalDate getDataCaducitatLlicencia() { return dataCaducitatLlicencia; }
    public void setDataCaducitatLlicencia(LocalDate dataCaducitatLlicencia) { this.dataCaducitatLlicencia = dataCaducitatLlicencia; }

    public String getNumTargetaCredit() { return numTargetaCredit; }
    public void setNumTargetaCredit(String numTargetaCredit) { this.numTargetaCredit = numTargetaCredit; }

    public String getAdreca() { return adreca; }
    public void setAdreca(String adreca) { this.adreca = adreca; }

    public String getNacionalitat() { return nacionalitat; }
    public void setNacionalitat(String nacionalitat) { this.nacionalitat = nacionalitat; }

    public EstatValidacio getEstatValidacio() { return estatValidacio; }
    public void setEstatValidacio(EstatValidacio estatValidacio) { this.estatValidacio = estatValidacio; }

    public Agent getAgentValidador() { return agentValidador; }
    public void setAgentValidador(Agent agentValidador) { this.agentValidador = agentValidador; }

    public Boolean getPremium() { return premium; }
    public void setPremium(Boolean premium) { this.premium = premium; }

    public BigDecimal getDescomptePremium() { return descomptePremium; }
    public void setDescomptePremium(BigDecimal descomptePremium) { this.descomptePremium = descomptePremium; }
}
