/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;

import cat.copernic.Projecte1_Equip5.entity.Enums.EstatReserva;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reserva d'un vehicle per part d'un client en un període de dates. Guarda el
 * codi únic (p. ex. EX-2026-0001), el vehicle i el client, els imports (total i
 * fiança) i l'estat (Activa, Lliurada, Finalitzada o Anul·lada).
 *
 * També recull les dades de cada fase del lloguer: l'anul·lació i l'import
 * retornat, el lliurament del vehicle i la devolució (comentari, incidència,
 * hores de retard, descompte per retard i si els desperfectes són imputables al
 * client).
 *
 * El període és semiobert: el dia d'inici compta i el dia de devolució queda
 * lliure per a una altra reserva (vegeu {@link #solapaAmb}).
 */
@Entity
@Table(name = "reserves")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long idReserva;

    @Column(name = "codi_reserva", nullable = false, unique = true, length = 20)
    private String codiReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_vehicle", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @Column(name = "data_inici", nullable = false)
    private LocalDate dataInici;

    @Column(name = "data_fi", nullable = false)
    private LocalDate dataFi;

    @Column(name = "import_total", nullable = false)
    private BigDecimal importTotal;

    @Column(name = "import_fianca", nullable = false)
    private BigDecimal fianca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Enums.EstatReserva estat = Enums.EstatReserva.Activa;

    @Column(name = "data_creacio", nullable = false, updatable = false)
    private LocalDateTime dataCreacio = LocalDateTime.now();

    @Column(name = "data_anulacio")
    private LocalDateTime dataAnulacio;

    @Column(name = "import_retornat")
    private BigDecimal importRetornat;

    @Column(name = "motiu_anulacio", length = 500)
    private String motiuAnulacio;

    @Column(name = "percentatge_devolucio", precision = 5, scale = 2)
    private BigDecimal percentatgeDevolucio;

    @Column(name = "data_lliurament")
    private LocalDateTime dataLliurament;

    @Column(name = "descripcio_lliurament", length = 500)
    private String descripcioLliurament;

    @Column(name = "data_retorn")
    private LocalDateTime dataRetorn;

    @Column(name = "comentari_retorn", length = 500)
    private String comentariRetorn;

    @Column(name = "incidencia_retorn", length = 500)
    private String incidenciaRetorn;

    /**
     * Hores de retard (arrodonides cap amunt) en la devolució; 0 si va a temps.
     */
    @Column(name = "hores_retard")
    private Integer horesRetard;

    /**
     * Import descomptat de la fiança pel retard (hores x preu d'una hora).
     */
    @Column(name = "descompte_retard", precision = 10, scale = 2)
    private BigDecimal descompteRetard;

    /**
     * true si l'agent ha marcat els desperfectes com a imputables al client.
     */
    @Column(name = "desperfecte_imputable")
    private Boolean desperfecteImputable;

    /**
     * Constructor buit, necessari per a JPA.
     */
    public Reserva() {
    }

    // 2. Constructor sense la clau primària
    /**
     * Crea una reserva sense la clau primària (l'identificador l'assigna la
     * base de dades en desar-la). La data de creació s'inicialitza amb
     * l'instant actual.
     *
     * @param codiReserva codi únic de la reserva (p. ex. EX-2026-0001)
     * @param vehicle vehicle reservat
     * @param client client que fa la reserva
     * @param dataInici primer dia del lloguer
     * @param dataFi dia de devolució (queda lliure per a altres reserves)
     * @param importTotal import total del lloguer
     * @param fianca fiança de la reserva
     * @param estat estat de la reserva
     */
    public Reserva(String codiReserva, Vehicle vehicle, Client client,
            LocalDate dataInici, LocalDate dataFi, BigDecimal importTotal,
            BigDecimal fianca, EstatReserva estat) {
        this.codiReserva = codiReserva;
        this.vehicle = vehicle;
        this.client = client;
        this.dataInici = dataInici;
        this.dataFi = dataFi;
        this.importTotal = importTotal;
        this.fianca = fianca;
        this.estat = estat;
    }

    // 3. Constructor amb tots els atributs
    /**
     * Crea una reserva amb tots els atributs bàsics, inclosos l'identificador i
     * la data de creació.
     *
     * @param idReserva identificador de la reserva (clau primària)
     * @param codiReserva codi únic de la reserva (p. ex. EX-2026-0001)
     * @param vehicle vehicle reservat
     * @param client client que fa la reserva
     * @param dataInici primer dia del lloguer
     * @param dataFi dia de devolució (queda lliure per a altres reserves)
     * @param importTotal import total del lloguer
     * @param fianca fiança de la reserva
     * @param estat estat de la reserva
     * @param dataCreacio data i hora de creació de la reserva
     */
    public Reserva(Long idReserva, String codiReserva, Vehicle vehicle, Client client,
            LocalDate dataInici, LocalDate dataFi, BigDecimal importTotal,
            BigDecimal fianca, EstatReserva estat, LocalDateTime dataCreacio) {
        this.idReserva = idReserva;
        this.codiReserva = codiReserva;
        this.vehicle = vehicle;
        this.client = client;
        this.dataInici = dataInici;
        this.dataFi = dataFi;
        this.importTotal = importTotal;
        this.fianca = fianca;
        this.estat = estat;
        this.dataCreacio = dataCreacio;
    }

    /**
     * Calcula el nombre de dies del lloguer, comptats entre la data d'inici i
     * la de devolució (el dia de devolució no compta).
     *
     * @return nombre de dies del lloguer, o 0 si falta alguna de les dues dates
     */
    public long getNumDies() {
        if (dataInici == null || dataFi == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dataInici, dataFi);
    }

    /**
     * Indica si aquesta reserva se solapa amb el període indicat. La comparació
     * és estricta ({@code <} i {@code >}): el dia de devolució queda lliure, de
     * manera que una reserva que acaba el dia en què en comença una altra no hi
     * té conflicte. És la mateixa regla que usa la consulta {@code
     * comptarSolapaments} del repositori.
     *
     * @param inici primer dia del període que es vol comprovar
     * @param fi dia de devolució del període que es vol comprovar (exclòs)
     * @return {@code true} si els dos períodes es creuen
     */
    public boolean solapaAmb(LocalDate inici, LocalDate fi) {
        return dataInici.isBefore(fi) && dataFi.isAfter(inici);
    }

    //Getters i setters
    /**
     * Retorna l'identificador numèric de la reserva (clau primària,
     * autogenerat).
     *
     * @return identificador de la reserva
     */
    public Long getIdReserva() {
        return idReserva;
    }

    /**
     * Estableix l'identificador numèric de la reserva (clau primària,
     * autogenerat).
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
     * Retorna el vehicle reservat.
     *
     * @return vehicle reservat
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Estableix el vehicle reservat.
     *
     * @param vehicle vehicle reservat
     */
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    /**
     * Retorna el client que ha fet la reserva.
     *
     * @return client de la reserva
     */
    public Client getClient() {
        return client;
    }

    /**
     * Estableix el client que ha fet la reserva.
     *
     * @param client client de la reserva
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Retorna el primer dia del lloguer.
     *
     * @return data d'inici del lloguer
     */
    public LocalDate getDataInici() {
        return dataInici;
    }

    /**
     * Estableix el primer dia del lloguer.
     *
     * @param dataInici data d'inici del lloguer
     */
    public void setDataInici(LocalDate dataInici) {
        this.dataInici = dataInici;
    }

    /**
     * Retorna el dia de devolució previst; aquest dia el vehicle queda lliure
     * per a altres reserves.
     *
     * @return data de devolució prevista
     */
    public LocalDate getDataFi() {
        return dataFi;
    }

    /**
     * Estableix el dia de devolució previst; aquest dia el vehicle queda lliure
     * per a altres reserves.
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
     * @return fiança de la reserva
     */
    public BigDecimal getFianca() {
        return fianca;
    }

    /**
     * Estableix la fiança de la reserva.
     *
     * @param fianca fiança de la reserva
     */
    public void setFianca(BigDecimal fianca) {
        this.fianca = fianca;
    }

    /**
     * Retorna l'estat de la reserva: Activa (per defecte), Lliurada,
     * Finalitzada o Anul·lada.
     *
     * @return estat de la reserva
     */
    public EstatReserva getEstat() {
        return estat;
    }

    /**
     * Estableix l'estat de la reserva: Activa (per defecte), Lliurada,
     * Finalitzada o Anul·lada.
     *
     * @param estat estat de la reserva
     */
    public void setEstat(EstatReserva estat) {
        this.estat = estat;
    }

    /**
     * Retorna la data i hora en què es va crear la reserva.
     *
     * @return data i hora de creació
     */
    public LocalDateTime getDataCreacio() {
        return dataCreacio;
    }

    /**
     * Estableix la data i hora en què es va crear la reserva.
     *
     * @param dataCreacio data i hora de creació
     */
    public void setDataCreacio(LocalDateTime dataCreacio) {
        this.dataCreacio = dataCreacio;
    }

    /**
     * Retorna la data i hora d'anul·lació de la reserva.
     *
     * @return data d'anul·lació, o {@code null} si no s'ha anul·lat
     */
    public LocalDateTime getDataAnulacio() {
        return dataAnulacio;
    }

    /**
     * Estableix la data i hora d'anul·lació de la reserva.
     *
     * @param dataAnulacio data d'anul·lació, o {@code null} si no s'ha anul·lat
     */
    public void setDataAnulacio(LocalDateTime dataAnulacio) {
        this.dataAnulacio = dataAnulacio;
    }

    /**
     * Retorna l'import retornat al client: en anul·lar la reserva, o la part de
     * la fiança que es retorna en tornar el vehicle.
     *
     * @return import retornat, o {@code null} si encara no n'hi ha
     */
    public BigDecimal getImportRetornat() {
        return importRetornat;
    }

    /**
     * Estableix l'import retornat al client: en anul·lar la reserva, o la part
     * de la fiança que es retorna en tornar el vehicle.
     *
     * @param importRetornat import retornat, o {@code null} si encara no n'hi
     * ha
     */
    public void setImportRetornat(BigDecimal importRetornat) {
        this.importRetornat = importRetornat;
    }

    /**
     * Retorna el motiu pel qual s'ha anul·lat la reserva.
     *
     * @return motiu d'anul·lació, o {@code null} si no s'ha anul·lat
     */
    public String getMotiuAnulacio() {
        return motiuAnulacio;
    }

    /**
     * Estableix el motiu pel qual s'ha anul·lat la reserva.
     *
     * @param motiuAnulacio motiu d'anul·lació, o {@code null} si no s'ha
     * anul·lat
     */
    public void setMotiuAnulacio(String motiuAnulacio) {
        this.motiuAnulacio = motiuAnulacio;
    }

    /**
     * Retorna el percentatge de l'import que es retorna al client en anul·lar
     * la reserva.
     *
     * @return percentatge de devolució aplicat en l'anul·lació
     */
    public BigDecimal getPercentatgeDevolucio() {
        return percentatgeDevolucio;
    }

    /**
     * Estableix el percentatge de l'import que es retorna al client en anul·lar
     * la reserva.
     *
     * @param percentatgeDevolucio percentatge de devolució aplicat en
     * l'anul·lació
     */
    public void setPercentatgeDevolucio(BigDecimal percentatgeDevolucio) {
        this.percentatgeDevolucio = percentatgeDevolucio;
    }

    /**
     * Retorna la data i hora en què es va lliurar el vehicle al client.
     *
     * @return data de lliurament, o {@code null} si encara no s'ha lliurat
     */
    public LocalDateTime getDataLliurament() {
        return dataLliurament;
    }

    /**
     * Estableix la data i hora en què es va lliurar el vehicle al client.
     *
     * @param dataLliurament data de lliurament, o {@code null} si encara no
     * s'ha lliurat
     */
    public void setDataLliurament(LocalDateTime dataLliurament) {
        this.dataLliurament = dataLliurament;
    }

    /**
     * Retorna la descripció de l'estat del vehicle en el moment del lliurament.
     *
     * @return descripció del lliurament
     */
    public String getDescripcioLliurament() {
        return descripcioLliurament;
    }

    /**
     * Estableix la descripció de l'estat del vehicle en el moment del
     * lliurament.
     *
     * @param descripcioLliurament descripció del lliurament
     */
    public void setDescripcioLliurament(String descripcioLliurament) {
        this.descripcioLliurament = descripcioLliurament;
    }

    /**
     * Retorna la data i hora en què el client va tornar el vehicle.
     *
     * @return data de devolució real, o {@code null} si encara no s'ha tornat
     */
    public LocalDateTime getDataRetorn() {
        return dataRetorn;
    }

    /**
     * Estableix la data i hora en què el client va tornar el vehicle.
     *
     * @param dataRetorn data de devolució real, o {@code null} si encara no
     * s'ha tornat
     */
    public void setDataRetorn(LocalDateTime dataRetorn) {
        this.dataRetorn = dataRetorn;
    }

    /**
     * Retorna el comentari de l'agent en rebre el vehicle de tornada.
     *
     * @return comentari de la devolució
     */
    public String getComentariRetorn() {
        return comentariRetorn;
    }

    /**
     * Estableix el comentari de l'agent en rebre el vehicle de tornada.
     *
     * @param comentariRetorn comentari de la devolució
     */
    public void setComentariRetorn(String comentariRetorn) {
        this.comentariRetorn = comentariRetorn;
    }

    /**
     * Retorna la incidència (desperfectes, etc.) registrada en la devolució.
     *
     * @return incidència de la devolució, o {@code null} si no n'hi ha
     */
    public String getIncidenciaRetorn() {
        return incidenciaRetorn;
    }

    /**
     * Estableix la incidència (desperfectes, etc.) registrada en la devolució.
     *
     * @param incidenciaRetorn incidència de la devolució, o {@code null} si no
     * n'hi ha
     */
    public void setIncidenciaRetorn(String incidenciaRetorn) {
        this.incidenciaRetorn = incidenciaRetorn;
    }

    /**
     * Retorna les hores de retard (arrodonides cap amunt) en la devolució; 0 si
     * ha anat a temps.
     *
     * @return hores de retard en la devolució
     */
    public Integer getHoresRetard() {
        return horesRetard;
    }

    /**
     * Estableix les hores de retard (arrodonides cap amunt) en la devolució; 0
     * si ha anat a temps.
     *
     * @param horesRetard hores de retard en la devolució
     */
    public void setHoresRetard(Integer horesRetard) {
        this.horesRetard = horesRetard;
    }

    /**
     * Retorna l'import descomptat de la fiança pel retard (hores x preu d'una
     * hora).
     *
     * @return import descomptat per retard
     */
    public BigDecimal getDescompteRetard() {
        return descompteRetard;
    }

    /**
     * Estableix l'import descomptat de la fiança pel retard (hores x preu d'una
     * hora).
     *
     * @param descompteRetard import descomptat per retard
     */
    public void setDescompteRetard(BigDecimal descompteRetard) {
        this.descompteRetard = descompteRetard;
    }

    /**
     * Retorna si l'agent ha marcat els desperfectes com a imputables al client.
     *
     * @return {@code true} si els desperfectes s'imputen al client
     */
    public Boolean getDesperfecteImputable() {
        return desperfecteImputable;
    }

    /**
     * Estableix si l'agent ha marcat els desperfectes com a imputables al
     * client.
     *
     * @param desperfecteImputable {@code true} si els desperfectes s'imputen al
     * client
     */
    public void setDesperfecteImputable(Boolean desperfecteImputable) {
        this.desperfecteImputable = desperfecteImputable;
    }

    /**
     * Fiança que es queda l'empresa (fiança - import retornat); null si no s'ha
     * retornat.
     * @return 
     * 
     */
    public BigDecimal getFiancaRetinguda() {
        if (fianca == null || importRetornat == null || dataRetorn == null) {
            return null;
        }
        return fianca.subtract(importRetornat);
    }

}
