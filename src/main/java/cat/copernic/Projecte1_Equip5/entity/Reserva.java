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
@Table(name = "reserva")
public class Reserva {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @Column(nullable = false)
    private LocalDate dataInici;

    @Column(nullable = false)
    private LocalDate dataFi;

    @Column(nullable = false)
    private BigDecimal importTotal;

    @Column(nullable = false)
    private BigDecimal fianca;

    private enum EstatReserva {ACTIVA, LLIURADA, FINALITZADA, ANUL_LADA}
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstatReserva estat;

    public Reserva() {
    }

    public Reserva(LocalDate dataInici, LocalDate dataFi, BigDecimal importTotal, BigDecimal fianca, EstatReserva estat) {
        this.dataInici = dataInici;
        this.dataFi = dataFi;
        this.importTotal = importTotal;
        this.fianca = fianca;
        this.estat = estat;
    }

    public Reserva(Long idReserva, LocalDate dataInici, LocalDate dataFi, BigDecimal importTotal, BigDecimal fianca, EstatReserva estat) {
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

    public EstatReserva getEstat() {
        return estat;
    }

    public void setEstat(EstatReserva estat) {
        this.estat = estat;
    }
    
    
    
}
