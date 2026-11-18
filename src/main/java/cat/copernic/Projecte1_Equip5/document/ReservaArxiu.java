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
        /**
         * Foto feta en lliurar el vehicle al client.
         */
        LLIURAMENT,
        /**
         * Foto feta quan el client torna el vehicle.
         */
        RETORN
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

    /**
     * Constructor buit, necessari per a Spring Data MongoDB.
     */
    public ReservaArxiu() {
    }

    /**
     * Crea una foto de reserva. L'identificador el genera MongoDB en desar-la i
     * la data de pujada s'inicialitza amb l'instant actual.
     *
     * @param idReserva identificador de la reserva de MySQL a què pertany la
     * foto
     * @param moment moment de la reserva en què es fa la foto (LLIURAMENT o
     * RETORN)
     * @param nom nom del fitxer
     * @param contentType tipus de contingut (MIME) del fitxer, p. ex.
     * image/jpeg
     * @param mida mida del fitxer en bytes
     * @param dades contingut de la foto, en bytes
     */
    public ReservaArxiu(Long idReserva, Moment moment, String nom,
            String contentType, long mida, byte[] dades) {
        this.idReserva = idReserva;
        this.moment = moment;
        this.nom = nom;
        this.contentType = contentType;
        this.mida = mida;
        this.dades = dades;
    }

    /**
     * Retorna l'identificador del fitxer a MongoDB.
     *
     * @return identificador del fitxer
     */
    public String getId() {
        return id;
    }

    /**
     * Estableix l'identificador del fitxer a MongoDB.
     *
     * @param id identificador del fitxer
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retorna l'identificador de la reserva de MySQL a què pertany la foto.
     *
     * @return identificador de la reserva
     */
    public Long getIdReserva() {
        return idReserva;
    }

    /**
     * Estableix l'identificador de la reserva de MySQL a què pertany la foto.
     *
     * @param idReserva identificador de la reserva
     */
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    /**
     * Retorna el moment de la reserva en què es va fer la foto: LLIURAMENT o
     * RETORN.
     *
     * @return moment de la foto
     */
    public Moment getMoment() {
        return moment;
    }

    /**
     * Estableix el moment de la reserva en què es va fer la foto: LLIURAMENT o
     * RETORN.
     *
     * @param moment moment de la foto
     */
    public void setMoment(Moment moment) {
        this.moment = moment;
    }

    /**
     * Retorna el nom del fitxer.
     *
     * @return nom del fitxer
     */
    public String getNom() {
        return nom;
    }

    /**
     * Estableix el nom del fitxer.
     *
     * @param nom nom del fitxer
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * Retorna el tipus de contingut (MIME) del fitxer, p. ex. image/jpeg.
     *
     * @return tipus de contingut
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Estableix el tipus de contingut (MIME) del fitxer, p. ex. image/jpeg.
     *
     * @param contentType tipus de contingut
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Retorna la mida del fitxer en bytes.
     *
     * @return mida en bytes
     */
    public long getMida() {
        return mida;
    }

    /**
     * Estableix la mida del fitxer en bytes.
     *
     * @param mida mida en bytes
     */
    public void setMida(long mida) {
        this.mida = mida;
    }

    /**
     * Retorna el contingut de la foto, en bytes.
     *
     * @return contingut en bytes
     */
    public byte[] getDades() {
        return dades;
    }

    /**
     * Estableix el contingut de la foto, en bytes.
     *
     * @param dades contingut en bytes
     */
    public void setDades(byte[] dades) {
        this.dades = dades;
    }

    /**
     * Retorna la data i hora en què es va pujar la foto.
     *
     * @return data i hora de pujada
     */
    public LocalDateTime getDataPujada() {
        return dataPujada;
    }

    /**
     * Estableix la data i hora en què es va pujar la foto.
     *
     * @param dataPujada data i hora de pujada
     */
    public void setDataPujada(LocalDateTime dataPujada) {
        this.dataPujada = dataPujada;
    }
}
