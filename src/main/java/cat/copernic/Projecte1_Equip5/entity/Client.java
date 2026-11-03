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

    @Enumerated(EnumType.STRING) // es para guardar el valor del Enum (Pendent, validat) como texto en la base de datos y no el indice del enum 1, 2 y 3 
    @Column(name = "estat_validacio", nullable = false)
    private Enums.EstatValidacio estatValidacio = Enums.EstatValidacio.Pendent;
    // = Enums.EstatValidacio.Pendent; por defecto es Pendent 
    // no hace falta import de Enums porque esta en el mismo package entities que Client 

    // FetchType.LAZY significa que si consulta datos de client no consulta datos de agents solo clients auqnue tenga apunte su clave primaria a agents 
    @ManyToOne(fetch = FetchType.LAZY) // muchos clientes pueden ser validados por un solo agente 
    // (fetch = FetchType.LAZY) el agente no se carga de la base de datos hasta que lo uses (client.getAgentValidador()). Mejora el rendimiento al evitar consultas innecesarias.
    @JoinColumn(name = "id_agent_validador", nullable = true) // el join column es para indicar que es una clave foranea de la tabla clients que apunta a la tabla agents 
    private Agent agentValidador; // nullable = true es porque mientras no se haya validado el cliente por administrador no tendra administrador entonces que lo guarde igualmente 
    // porque este campo estara vacio si no ha sido validado entonces que guarde los datos en la base de datos aunque no este validadod 
    
    // Para cargar un Client, Hibernate hace un JOIN entre usuaris y clients. De ahí el nombre.
    // Para cargar un Client, Hibernate hace un JOIN entre usuaris y clients. De ahí el nombre. 

    @Column(name = "premium", nullable = false) // nullable siempre tiene que tener valor le tenemso que por si o no 
    private Boolean premium = false; // por defecto no es premium

    @Column(name = "descompte_premium", precision = 10, scale = 2)
    private BigDecimal descomptePremium; // BigDecimal es una clase de Java para representar números decimales con precisión exacta, sobre todo útil para dinero. 
    
    // añadidos para los campos del formulario campo adicional 
    @Column(name = "data_naixement")
    private LocalDate dataNaixement;
    // añadido para poder listar por clients o agents por telefon 
    @Column(name = "telefon", length = 20)
    private String telefon;

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
    // no hay cmapos telefon y data de naixment porque no hace falta para no romper el constructor... 
    }
    // 
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
    
    // añadidos para los campos del formulario campo adicional 
    public LocalDate getDataNaixement() {
        return dataNaixement;
    }

    public void setDataNaixement(LocalDate dataNaixement) {
        this.dataNaixement = dataNaixement;
    }
    
    // añadido para poder listar clients por telefon o agents por telefon 
    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

}
