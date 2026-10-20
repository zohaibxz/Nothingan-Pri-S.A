/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "reserves")
public class Reserva {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long idReserva;

    @Column(name = "data_inici", nullable = false)
    private LocalDate dataInici;

    @Column(name = "data_fi", nullable = false)
    private LocalDate dataFi;

    @Column(name = "import_total", nullable = false)
    private BigDecimal importTotal;

    @Column(name = "import_fianca", nullable = false)
    private BigDecimal fianca;

    @Enumerated(EnumType.STRING)
    @Column(name = "estat_reserva", nullable = false)
    private Enums.EstatReserva estat;

    public Reserva() {
    }

    public Reserva(LocalDate dataInici, LocalDate dataFi, BigDecimal importTotal, BigDecimal fianca, Enums.EstatReserva estat) {
        this.dataInici = dataInici;
        this.dataFi = dataFi;
        this.importTotal = importTotal;
        this.fianca = fianca;
        this.estat = estat;
    }

    public Reserva(Long idReserva, LocalDate dataInici, LocalDate dataFi, BigDecimal importTotal, BigDecimal fianca, Enums.EstatReserva estat) {
        this.idReserva = idReserva;
        this.dataInici = dataInici;
        this.dataFi = dataFi;
        this.importTotal = importTotal;
        this.fianca = fianca;
        this.estat = estat;
    }

    public Long getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
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

    public Enums.EstatReserva getEstat() {
        return estat;
    }

    public void setEstat(Enums.EstatReserva estat) {
        this.estat = estat;
    }
    
    
    
}
