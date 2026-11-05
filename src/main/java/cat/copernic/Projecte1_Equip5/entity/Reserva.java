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
    
    @Column (name = "motiu_anullacio")
    private String motiuAnulacio;
    
    public Reserva() {
    }

    // 2. Constructor sense la clau primària
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
    
    public long getNumDies() {
        if (dataInici == null || dataFi == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dataInici, dataFi);
    }
    
    public boolean solapaAmb(LocalDate inici, LocalDate fi) {
        return dataInici.isBefore(fi) && dataFi.isAfter(inici);
    }

    //Getters i setters
    public Long getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public String getCodiReserva() {
        return codiReserva;
    }

    public void setCodiReserva(String codiReserva) {
        this.codiReserva = codiReserva;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDate getDataInici() {
        return dataInici;
    }

    public void setDataInici(LocalDate dataInici) {
        this.dataInici = dataInici;
    }

    public LocalDate getDataFi() {
        return dataFi;
    }

    public void setDataFi(LocalDate dataFi) {
        this.dataFi = dataFi;
    }

    public BigDecimal getImportTotal() {
        return importTotal;
    }

    public void setImportTotal(BigDecimal importTotal) {
        this.importTotal = importTotal;
    }

    public BigDecimal getFianca() {
        return fianca;
    }

    public void setFianca(BigDecimal fianca) {
        this.fianca = fianca;
    }

    public EstatReserva getEstat() {
        return estat;
    }

    public void setEstat(EstatReserva estat) {
        this.estat = estat;
    }

    public LocalDateTime getDataCreacio() {
        return dataCreacio;
    }

    public void setDataCreacio(LocalDateTime dataCreacio) {
        this.dataCreacio = dataCreacio;
    }
    
    public LocalDateTime getDataAnulacio() {
        return dataAnulacio;
    }

    public void setDataAnulacio(LocalDateTime dataAnulacio) {
        this.dataAnulacio = dataAnulacio;
    }

    public BigDecimal getImportRetornat() {
        return importRetornat;
    }

    public void setImportRetornat(BigDecimal importRetornat) {
        this.importRetornat = importRetornat;
    }
    
    public String getMotiuAnulacio() {
        return motiuAnulacio;
    }

    public void setMotiuAnulacio(String motiuAnullacio) {
        this.motiuAnulacio = motiuAnullacio;
    }
    
    
    
}
