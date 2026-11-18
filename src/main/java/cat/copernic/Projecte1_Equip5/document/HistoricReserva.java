package cat.copernic.Projecte1_Equip5.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registre permanent d'una reserva a MongoDB. És una còpia de les dades, sense
 * cap relació amb MySQL: si la reserva, el client o el vehicle s'esborren, el
 * registre es manté. Hi ha un sol document per reserva (clau = idReserva) que
 * s'actualitza a cada canvi d'estat.
 */
@Document(collection = "historic_reserves")
public class HistoricReserva {

    @Id
    private Long idReserva;

    private String codiReserva;
    private String dniClient;
    private String nomClient;
    private String matricula;
    private String marca;
    private String model;
    private LocalDate dataInici;
    private LocalDate dataFi;
    private BigDecimal importTotal;
    private BigDecimal fianca;
    private String estat;               // text de l'enum EstatReserva
    private LocalDateTime dataAlta;
    private LocalDateTime dataAnullacio;
    private BigDecimal importRetornat;
    private String motiuAnulacio;
    private BigDecimal percentatgeDevolucio;
    private LocalDateTime dataLliurament;
    private String descripcioLliurament;
    private LocalDateTime dataRetorn;
    private String comentariRetorn;
    private String incidenciaRetorn;
    private Integer horesRetard;
    private BigDecimal descompteRetard;
    private Boolean desperfecteImputable;

    /**
     * Constructor buit, necessari per a Spring Data MongoDB.
     */
    public HistoricReserva() {
    }

    /**
     * Retorna l'identificador de la reserva de MySQL, que és la clau d'aquest
     * document.
     *
     * @return identificador de la reserva
     */
    public Long getIdReserva() {
        return idReserva;
    }

    /**
     * Estableix l'identificador de la reserva de MySQL, que és la clau d'aquest
     * document.
     *
     * @param idReserva identificador de la reserva
     */
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    /**
     * Retorna el codi únic de la reserva (p. ex. EX-2026-0001).
     *
     * @return codi de la reserva
     */
    public String getCodiReserva() {
        return codiReserva;
    }

    /**
     * Estableix el codi únic de la reserva (p. ex. EX-2026-0001).
     *
     * @param codiReserva codi de la reserva
     */
    public void setCodiReserva(String codiReserva) {
        this.codiReserva = codiReserva;
    }

    /**
     * Retorna el DNI del client en el moment de la reserva.
     *
     * @return DNI del client
     */
    public String getDniClient() {
        return dniClient;
    }

    /**
     * Estableix el DNI del client en el moment de la reserva.
     *
     * @param dniClient DNI del client
     */
    public void setDniClient(String dniClient) {
        this.dniClient = dniClient;
    }

    /**
     * Retorna el nom complet del client en el moment de la reserva.
     *
     * @return nom del client
     */
    public String getNomClient() {
        return nomClient;
    }

    /**
     * Estableix el nom complet del client en el moment de la reserva.
     *
     * @param nomClient nom del client
     */
    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    /**
     * Retorna la matrícula del vehicle reservat.
     *
     * @return matrícula del vehicle
     */
    public String getMatricula() {
        return matricula;
    }

    /**
     * Estableix la matrícula del vehicle reservat.
     *
     * @param matricula matrícula del vehicle
     */
    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    /**
     * Retorna la marca del vehicle reservat.
     *
     * @return marca del vehicle
     */
    public String getMarca() {
        return marca;
    }

    /**
     * Estableix la marca del vehicle reservat.
     *
     * @param marca marca del vehicle
     */
    public void setMarca(String marca) {
        this.marca = marca;
    }

    /**
     * Retorna el model del vehicle reservat.
     *
     * @return model del vehicle
     */
    public String getModel() {
        return model;
    }

    /**
     * Estableix el model del vehicle reservat.
     *
     * @param model model del vehicle
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Retorna el primer dia del lloguer.
     *
     * @return data d'inici
     */
    public LocalDate getDataInici() {
        return dataInici;
    }

    /**
     * Estableix el primer dia del lloguer.
     *
     * @param dataInici data d'inici
     */
    public void setDataInici(LocalDate dataInici) {
        this.dataInici = dataInici;
    }

    /**
     * Retorna el dia de devolució previst.
     *
     * @return data de devolució prevista
     */
    public LocalDate getDataFi() {
        return dataFi;
    }

    /**
     * Estableix el dia de devolució previst.
     *
     * @param dataFi data de devolució prevista
     */
    public void setDataFi(LocalDate dataFi) {
        this.dataFi = dataFi;
    }

    /**
     * Retorna l'import total del lloguer.
     *
     * @return import total
     */
    public BigDecimal getImportTotal() {
        return importTotal;
    }

    /**
     * Estableix l'import total del lloguer.
     *
     * @param importTotal import total
     */
    public void setImportTotal(BigDecimal importTotal) {
        this.importTotal = importTotal;
    }

    /**
     * Retorna la fiança de la reserva.
     *
     * @return fiança
     */
    public BigDecimal getFianca() {
        return fianca;
    }

    /**
     * Estableix la fiança de la reserva.
     *
     * @param fianca fiança
     */
    public void setFianca(BigDecimal fianca) {
        this.fianca = fianca;
    }

    /**
     * Retorna l'estat de la reserva en forma de text (valor de l'enum {@code
     * EstatReserva}).
     *
     * @return estat de la reserva, en text
     */
    public String getEstat() {
        return estat;
    }

    /**
     * Estableix l'estat de la reserva en forma de text (valor de l'enum {@code
     * EstatReserva}).
     *
     * @param estat estat de la reserva, en text
     */
    public void setEstat(String estat) {
        this.estat = estat;
    }

    /**
     * Retorna la data i hora de creació de la reserva.
     *
     * @return data i hora de creació
     */
    public LocalDateTime getDataAlta() {
        return dataAlta;
    }

    /**
     * Estableix la data i hora de creació de la reserva.
     *
     * @param dataAlta data i hora de creació
     */
    public void setDataAlta(LocalDateTime dataAlta) {
        this.dataAlta = dataAlta;
    }

    /**
     * Retorna la data i hora d'anul·lació de la reserva.
     *
     * @return data d'anul·lació, o {@code null} si no s'ha anul·lat
     */
    public LocalDateTime getDataAnullacio() {
        return dataAnullacio;
    }

    /**
     * Estableix la data i hora d'anul·lació de la reserva.
     *
     * @param dataAnullacio data d'anul·lació, o {@code null} si no s'ha
     * anul·lat
     */
    public void setDataAnullacio(LocalDateTime dataAnullacio) {
        this.dataAnullacio = dataAnullacio;
    }

    /**
     * Retorna l'import retornat al client (en anul·lar o en tornar el vehicle).
     *
     * @return import retornat, o {@code null} si no n'hi ha
     */
    public BigDecimal getImportRetornat() {
        return importRetornat;
    }

    /**
     * Estableix l'import retornat al client (en anul·lar o en tornar el
     * vehicle).
     *
     * @param importRetornat import retornat, o {@code null} si no n'hi ha
     */
    public void setImportRetornat(BigDecimal importRetornat) {
        this.importRetornat = importRetornat;
    }

    /**
     * Retorna el motiu d'anul·lació de la reserva.
     *
     * @return motiu d'anul·lació, o {@code null} si no s'ha anul·lat
     */
    public String getMotiuAnulacio() {
        return motiuAnulacio;
    }

    /**
     * Estableix el motiu d'anul·lació de la reserva.
     *
     * @param motiuAnulacio motiu d'anul·lació, o {@code null} si no s'ha
     * anul·lat
     */
    public void setMotiuAnulacio(String motiuAnulacio) {
        this.motiuAnulacio = motiuAnulacio;
    }

    /**
     * Retorna el percentatge de l'import que es va retornar en anul·lar la
     * reserva.
     *
     * @return percentatge de devolució
     */
    public BigDecimal getPercentatgeDevolucio() {
        return percentatgeDevolucio;
    }

    /**
     * Estableix el percentatge de l'import que es va retornar en anul·lar la
     * reserva.
     *
     * @param percentatgeDevolucio percentatge de devolució
     */
    public void setPercentatgeDevolucio(BigDecimal percentatgeDevolucio) {
        this.percentatgeDevolucio = percentatgeDevolucio;
    }

    /**
     * Retorna la data i hora en què es va lliurar el vehicle.
     *
     * @return data de lliurament, o {@code null} si no s'ha lliurat
     */
    public LocalDateTime getDataLliurament() {
        return dataLliurament;
    }

    /**
     * Estableix la data i hora en què es va lliurar el vehicle.
     *
     * @param dataLliurament data de lliurament, o {@code null} si no s'ha
     * lliurat
     */
    public void setDataLliurament(LocalDateTime dataLliurament) {
        this.dataLliurament = dataLliurament;
    }

    /**
     * Retorna la descripció de l'estat del vehicle en el lliurament.
     *
     * @return descripció del lliurament
     */
    public String getDescripcioLliurament() {
        return descripcioLliurament;
    }

    /**
     * Estableix la descripció de l'estat del vehicle en el lliurament.
     *
     * @param descripcioLliurament descripció del lliurament
     */
    public void setDescripcioLliurament(String descripcioLliurament) {
        this.descripcioLliurament = descripcioLliurament;
    }

    /**
     * Retorna la data i hora en què es va tornar el vehicle.
     *
     * @return data de devolució real, o {@code null} si no s'ha tornat
     */
    public LocalDateTime getDataRetorn() {
        return dataRetorn;
    }

    /**
     * Estableix la data i hora en què es va tornar el vehicle.
     *
     * @param dataRetorn data de devolució real, o {@code null} si no s'ha
     * tornat
     */
    public void setDataRetorn(LocalDateTime dataRetorn) {
        this.dataRetorn = dataRetorn;
    }

    /**
     * Retorna el comentari de l'agent en la devolució.
     *
     * @return comentari de la devolució
     */
    public String getComentariRetorn() {
        return comentariRetorn;
    }

    /**
     * Estableix el comentari de l'agent en la devolució.
     *
     * @param comentariRetorn comentari de la devolució
     */
    public void setComentariRetorn(String comentariRetorn) {
        this.comentariRetorn = comentariRetorn;
    }

    /**
     * Retorna la incidència registrada en la devolució.
     *
     * @return incidència de la devolució, o {@code null} si no n'hi ha
     */
    public String getIncidenciaRetorn() {
        return incidenciaRetorn;
    }

    /**
     * Estableix la incidència registrada en la devolució.
     *
     * @param incidenciaRetorn incidència de la devolució, o {@code null} si no
     * n'hi ha
     */
    public void setIncidenciaRetorn(String incidenciaRetorn) {
        this.incidenciaRetorn = incidenciaRetorn;
    }

    /**
     * Retorna les hores de retard en la devolució.
     *
     * @return hores de retard
     */
    public Integer getHoresRetard() {
        return horesRetard;
    }

    /**
     * Estableix les hores de retard en la devolució.
     *
     * @param horesRetard hores de retard
     */
    public void setHoresRetard(Integer horesRetard) {
        this.horesRetard = horesRetard;
    }

    /**
     * Retorna l'import descomptat de la fiança pel retard.
     *
     * @return import descomptat per retard
     */
    public BigDecimal getDescompteRetard() {
        return descompteRetard;
    }

    /**
     * Estableix l'import descomptat de la fiança pel retard.
     *
     * @param descompteRetard import descomptat per retard
     */
    public void setDescompteRetard(BigDecimal descompteRetard) {
        this.descompteRetard = descompteRetard;
    }

    /**
     * Retorna si els desperfectes s'han imputat al client.
     *
     * @return {@code true} si els desperfectes s'imputen al client
     */
    public Boolean getDesperfecteImputable() {
        return desperfecteImputable;
    }

    /**
     * Estableix si els desperfectes s'han imputat al client.
     *
     * @param desperfecteImputable {@code true} si els desperfectes s'imputen al
     * client
     */
    public void setDesperfecteImputable(Boolean desperfecteImputable) {
        this.desperfecteImputable = desperfecteImputable;
    }

}
