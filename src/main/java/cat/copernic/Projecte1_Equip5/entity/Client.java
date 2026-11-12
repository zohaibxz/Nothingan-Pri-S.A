package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cliente del servicio de alquiler de vehículos. Extiende {@link Usuari} (el
 * DNI, el email, la contraseña y el nombre están en la clase padre) y añade los
 * datos propios del cliente: caducidad del DNI, carnet de conducir, dirección,
 * nacionalidad, teléfono y fecha de nacimiento.
 *
 * Un cliente nuevo queda en estado de validación {@code Pendent} hasta que un
 * agente o administrador lo valida o lo rechaza; ese agente se guarda en
 * {@code agentValidador}. También guarda si es premium y el descuento que
 * tiene. {@link Agent} extiende esta clase, así que un agente también es un
 * cliente a nivel de datos.
 */
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
    /**
     * Constructor vacío, necesario para JPA y para rellenar el objeto desde un
     * formulario.
     */
    public Client() {
        super();
    }

    // 2. Amb tots els atributs (amb DNI)
    /**
     * Crea un cliente con todos sus atributos, incluido el DNI (clave
     * primaria). Las fechas de nacimiento y el teléfono no se pasan por
     * constructor; se establecen con sus setters.
     *
     * @param dniUsuari DNI del usuario (clave primaria)
     * @param email email del usuario (único; se usa para el login)
     * @param passwordHash contraseña ya codificada (hash), nunca en texto plano
     * @param dataCreacio fecha y hora de alta del usuario
     * @param nom nombre del usuario
     * @param cognoms apellidos del usuario
     * @param dataCaducitatDni fecha de caducidad del DNI
     * @param tipusLlicencia tipo de carnet de conducir (por ejemplo, B)
     * @param dataCaducitatLlicencia fecha de caducidad del carnet de conducir
     * @param adreca dirección postal del cliente
     * @param nacionalitat nacionalidad del cliente
     * @param estatValidacio estado de validación (Pendent, Validat o Rebutjat)
     * @param agentValidador agente que validó al cliente, o {@code null} si aún
     * no está validado
     * @param premium si el cliente es premium
     * @param descomptePremium descuento que tiene el cliente premium, o
     * {@code null} si no tiene
     */
    public Client(String dniUsuari, String email, String passwordHash, LocalDateTime dataCreacio,
            String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia,
            LocalDate dataCaducitatLlicencia, String adreca,
            String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador,
            Boolean premium, BigDecimal descomptePremium) {
        super(dniUsuari, email, passwordHash, dataCreacio, nom, cognoms);
        this.dataCaducitatDni = dataCaducitatDni;
        this.tipusLlicencia = tipusLlicencia;
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
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
    /**
     * Crea un cliente sin DNI, para cuando la clave primaria se asigna después.
     * Las fechas de nacimiento y el teléfono no se pasan por constructor; se
     * establecen con sus setters.
     *
     * @param email email del usuario (único; se usa para el login)
     * @param passwordHash contraseña ya codificada (hash), nunca en texto plano
     * @param dataCreacio fecha y hora de alta del usuario
     * @param nom nombre del usuario
     * @param cognoms apellidos del usuario
     * @param dataCaducitatDni fecha de caducidad del DNI
     * @param tipusLlicencia tipo de carnet de conducir (por ejemplo, B)
     * @param dataCaducitatLlicencia fecha de caducidad del carnet de conducir
     * @param adreca dirección postal del cliente
     * @param nacionalitat nacionalidad del cliente
     * @param estatValidacio estado de validación (Pendent, Validat o Rebutjat)
     * @param agentValidador agente que validó al cliente, o {@code null} si aún
     * no está validado
     * @param premium si el cliente es premium
     * @param descomptePremium descuento que tiene el cliente premium, o
     * {@code null} si no tiene
     */
    public Client(String email, String passwordHash, LocalDateTime dataCreacio,
            String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia,
            LocalDate dataCaducitatLlicencia, String adreca,
            String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador,
            Boolean premium, BigDecimal descomptePremium) {
        super(email, passwordHash, dataCreacio, nom, cognoms);
        this.dataCaducitatDni = dataCaducitatDni;
        this.tipusLlicencia = tipusLlicencia;
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
        this.adreca = adreca;
        this.nacionalitat = nacionalitat;
        this.estatValidacio = estatValidacio;
        this.agentValidador = agentValidador;
        this.premium = premium;
        this.descomptePremium = descomptePremium;
    }

    // Getters y Setters
    /**
     * Devuelve la fecha de caducidad del DNI del cliente.
     *
     * @return fecha de caducidad del DNI
     */
    public LocalDate getDataCaducitatDni() {
        return dataCaducitatDni;
    }

    /**
     * Establece la fecha de caducidad del DNI del cliente.
     *
     * @param dataCaducitatDni fecha de caducidad del DNI
     */
    public void setDataCaducitatDni(LocalDate dataCaducitatDni) {
        this.dataCaducitatDni = dataCaducitatDni;
    }

    /**
     * Devuelve el tipo de carnet de conducir del cliente (por ejemplo, B).
     *
     * @return tipo de carnet de conducir
     */
    public String getTipusLlicencia() {
        return tipusLlicencia;
    }

    /**
     * Establece el tipo de carnet de conducir del cliente (por ejemplo, B).
     *
     * @param tipusLlicencia tipo de carnet de conducir
     */
    public void setTipusLlicencia(String tipusLlicencia) {
        this.tipusLlicencia = tipusLlicencia;
    }

    /**
     * Devuelve la fecha de caducidad del carnet de conducir.
     *
     * @return fecha de caducidad del carnet
     */
    public LocalDate getDataCaducitatLlicencia() {
        return dataCaducitatLlicencia;
    }

    /**
     * Establece la fecha de caducidad del carnet de conducir.
     *
     * @param dataCaducitatLlicencia fecha de caducidad del carnet
     */
    public void setDataCaducitatLlicencia(LocalDate dataCaducitatLlicencia) {
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
    }

    /**
     * Devuelve la dirección postal del cliente.
     *
     * @return dirección del cliente
     */
    public String getAdreca() {
        return adreca;
    }

    /**
     * Establece la dirección postal del cliente.
     *
     * @param adreca dirección del cliente
     */
    public void setAdreca(String adreca) {
        this.adreca = adreca;
    }

    /**
     * Devuelve la nacionalidad del cliente.
     *
     * @return nacionalidad del cliente
     */
    public String getNacionalitat() {
        return nacionalitat;
    }

    /**
     * Establece la nacionalidad del cliente.
     *
     * @param nacionalitat nacionalidad del cliente
     */
    public void setNacionalitat(String nacionalitat) {
        this.nacionalitat = nacionalitat;
    }

    /**
     * Devuelve el estado de validación del cliente: Pendent (por defecto),
     * Validat o Rebutjat.
     *
     * @return estado de validación
     */
    public Enums.EstatValidacio getEstatValidacio() {
        return estatValidacio;
    }

    /**
     * Establece el estado de validación del cliente: Pendent (por defecto),
     * Validat o Rebutjat.
     *
     * @param estatValidacio estado de validación
     */
    public void setEstatValidacio(Enums.EstatValidacio estatValidacio) {
        this.estatValidacio = estatValidacio;
    }

    /**
     * Devuelve el agente que validó al cliente; es {@code null} mientras no se
     * haya validado.
     *
     * @return agente validador, o {@code null} si aún no está validado
     */
    public Agent getAgentValidador() {
        return agentValidador;
    }

    /**
     * Establece el agente que validó al cliente; es {@code null} mientras no se
     * haya validado.
     *
     * @param agentValidador agente validador, o {@code null} si aún no está
     * validado
     */
    public void setAgentValidador(Agent agentValidador) {
        this.agentValidador = agentValidador;
    }

    /**
     * Devuelve si el cliente es premium (por defecto, no lo es).
     *
     * @return {@code true} si el cliente es premium
     */
    public Boolean getPremium() {
        return premium;
    }

    /**
     * Establece si el cliente es premium (por defecto, no lo es).
     *
     * @param premium {@code true} si el cliente es premium
     */
    public void setPremium(Boolean premium) {
        this.premium = premium;
    }

    /**
     * Devuelve el descuento del cliente premium, con precisión decimal exacta.
     *
     * @return descuento premium, o {@code null} si no tiene
     */
    public BigDecimal getDescomptePremium() {
        return descomptePremium;
    }

    /**
     * Establece el descuento del cliente premium, con precisión decimal exacta.
     *
     * @param descomptePremium descuento premium, o {@code null} si no tiene
     */
    public void setDescomptePremium(BigDecimal descomptePremium) {
        this.descomptePremium = descomptePremium;
    }

    // añadidos para los campos del formulario campo adicional 
    /**
     * Devuelve la fecha de nacimiento del cliente (campo adicional del
     * formulario de alta).
     *
     * @return fecha de nacimiento
     */
    public LocalDate getDataNaixement() {
        return dataNaixement;
    }

    /**
     * Establece la fecha de nacimiento del cliente (campo adicional del
     * formulario de alta).
     *
     * @param dataNaixement fecha de nacimiento
     */
    public void setDataNaixement(LocalDate dataNaixement) {
        this.dataNaixement = dataNaixement;
    }

    // añadido para poder listar clients por telefon o agents por telefon 
    /**
     * Devuelve el teléfono del cliente, que permite buscar y listar clientes
     * por teléfono.
     *
     * @return teléfono del cliente
     */
    public String getTelefon() {
        return telefon;
    }

    /**
     * Establece el teléfono del cliente, que permite buscar y listar clientes
     * por teléfono.
     *
     * @param telefon teléfono del cliente
     */
    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

}
