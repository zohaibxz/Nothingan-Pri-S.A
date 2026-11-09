package cat.copernic.Projecte1_Equip5.document;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Foto d'una reserva feta pel personal en lliurar o retornar el vehicle (RF49 i
 * RF48). Es desa a MongoDB; la unió amb la reserva de MySQL és l'idReserva.
 * Només hi poden accedir l'agent i l'administrador.
 */
@Document(collection = "reserva_arxius")
public class ReservaArxiu {

    /**
     * Moment de la reserva en què es fa la foto.
     */
    public enum Moment {
        LLIURAMENT, RETORN
    }

    @Id
    private String id;

    private Long idReserva;
    private Moment moment;
    private String nom;
    private String contentType;
    private long mida;
    private byte[] dades;
    private LocalDateTime dataPujada = LocalDateTime.now();

    public ReservaArxiu() {
    }

    public ReservaArxiu(Long idReserva, Moment moment, String nom,
            String contentType, long mida, byte[] dades) {
        this.idReserva = idReserva;
        this.moment = moment;
        this.nom = nom;
        this.contentType = contentType;
        this.mida = mida;
        this.dades = dades;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public Moment getMoment() {
        return moment;
    }

    public void setMoment(Moment moment) {
        this.moment = moment;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getMida() {
        return mida;
    }

    public void setMida(long mida) {
        this.mida = mida;
    }

    public byte[] getDades() {
        return dades;
    }

    public void setDades(byte[] dades) {
        this.dades = dades;
    }

    public LocalDateTime getDataPujada() {
        return dataPujada;
    }

    public void setDataPujada(LocalDateTime dataPujada) {
        this.dataPujada = dataPujada;
    }
}
