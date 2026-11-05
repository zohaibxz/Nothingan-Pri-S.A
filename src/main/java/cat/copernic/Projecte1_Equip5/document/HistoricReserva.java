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

    public HistoricReserva() {
    }

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

    public String getDniClient() {
        return dniClient;
    }

    public void setDniClient(String dniClient) {
        this.dniClient = dniClient;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    public String getEstat() {
        return estat;
    }

    public void setEstat(String estat) {
        this.estat = estat;
    }

    public LocalDateTime getDataAlta() {
        return dataAlta;
    }

    public void setDataAlta(LocalDateTime dataAlta) {
        this.dataAlta = dataAlta;
    }

    public LocalDateTime getDataAnullacio() {
        return dataAnullacio;
    }

    public void setDataAnullacio(LocalDateTime dataAnullacio) {
        this.dataAnullacio = dataAnullacio;
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

    public void setMotiuAnulacio(String motiuAnulacio) {
        this.motiuAnulacio = motiuAnulacio;
    }
}
