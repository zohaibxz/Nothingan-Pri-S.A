package cat.copernic.Projecte1_Equip5.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documentación de identidad de un usuario guardada en MongoDB: el documento de
 * identidad (DNI/NIE) y el carnet de conducir, cada uno con el nombre del
 * fichero, su tipo de contenido y sus bytes, más un estado de revisión.
 *
 * La ficha se une al usuario de MySQL por el DNI ({@code dniUsuari}, único).
 * Como son dos bases de datos distintas no hay clave foránea, así que al borrar
 * un usuario hay que borrar también su ficha con
 * {@code DocumentacioRepository.deleteByDniUsuari}.
 */
@Document(collection = "documentacio")
public class Documentacio {

    /**
     * Estado de la documentación. Es parecido a {@code Enums.EstatValidacio},
     * pero es solo de Mongo. Mongo guarda los enums como texto, así que no hace
     * falta {@code @Enumerated}.
     */
    public enum Estat {
        /**
         * Aún no revisada; es el valor con el que nace la ficha.
         */
        PENDENT,
        /**
         * Revisada y aceptada.
         */
        APROVADA,
        /**
         * Revisada y rechazada.
         */
        REBUTJADA
    }
    /* Un enum anidado con los estados de la documentación. Es parecido a Enums.EstatValidacio, pero este es solo de Mongo.
    En Mongo los enums se guardan como texto por defecto, así que no necesitas @Enumerated. */

    @Id
    /* El identificador del documento. Si lo dejas en null, Mongo genera un ObjectId solo y lo convierte a String.
    Por eso aquí no hace falta @GeneratedValue: en Mongo es automático.*/
    private String id;

    @Indexed(unique = true) // unique solo puedeo haber un dniUsuari
    /* crea un índice sobre ese campo, para que findByDniUsuari sea rápido. */
    private String dniUsuari;          // une esta ficha con el usuario de MySQL

    private String nomIdentitat; // : el nombre del fichero (dni_frontal.jpg)
    private String tipusIdentitat; //el tipo de contenido (image/jpeg, application/pdf)
    private byte[] identitat; // el contenido del fichero en bytes
    // Un array de bytes: el fichero entero guardado dentro del propio documento. Para subirlo desde un formulario sería algo así:

    private String nomCarnet;
    private String tipusCarnet;
    private byte[] carnet;

    private Estat estat = Estat.PENDENT; // Estado de la documentación, que por defecto nace en PENDENT, igual que el estatValidacio del cliente.

    /**
     * Crea una ficha con todos los datos, incluido el identificador. El estado
     * queda en PENDENT, que es el valor por defecto.
     *
     * @param id identificador del documento (si es {@code null}, Mongo lo
     * genera)
     * @param dniUsuari DNI del usuario al que pertenece la documentación
     * @param nomIdentitat nombre del fichero del documento de identidad
     * @param tipusIdentitat tipo de contenido del documento de identidad (por
     * ejemplo, image/jpeg)
     * @param identitat contenido del documento de identidad, en bytes
     * @param nomCarnet nombre del fichero del carnet de conducir
     * @param tipusCarnet tipo de contenido del carnet de conducir
     * @param carnet contenido del carnet de conducir, en bytes
     */
    public Documentacio(String id, String dniUsuari, String nomIdentitat, String tipusIdentitat, byte[] identitat, String nomCarnet, String tipusCarnet, byte[] carnet) {
        this.id = id;
        this.dniUsuari = dniUsuari;
        this.nomIdentitat = nomIdentitat;
        this.tipusIdentitat = tipusIdentitat;
        this.identitat = identitat;
        this.nomCarnet = nomCarnet;
        this.tipusCarnet = tipusCarnet;
        this.carnet = carnet;
    }

    /**
     * Constructor vacío, necesario para Spring Data MongoDB.
     */
    public Documentacio() {
    }

    /**
     * Crea una ficha sin identificador (Mongo lo genera al guardar). El estado
     * queda en PENDENT, que es el valor por defecto.
     *
     * @param dniUsuari DNI del usuario al que pertenece la documentación
     * @param nomIdentitat nombre del fichero del documento de identidad
     * @param tipusIdentitat tipo de contenido del documento de identidad (por
     * ejemplo, image/jpeg)
     * @param identitat contenido del documento de identidad, en bytes
     * @param nomCarnet nombre del fichero del carnet de conducir
     * @param tipusCarnet tipo de contenido del carnet de conducir
     * @param carnet contenido del carnet de conducir, en bytes
     */
    public Documentacio(String dniUsuari, String nomIdentitat, String tipusIdentitat, byte[] identitat, String nomCarnet, String tipusCarnet, byte[] carnet) {
        this.dniUsuari = dniUsuari;
        this.nomIdentitat = nomIdentitat;
        this.tipusIdentitat = tipusIdentitat;
        this.identitat = identitat;
        this.nomCarnet = nomCarnet;
        this.tipusCarnet = tipusCarnet;
        this.carnet = carnet;
    }

    // Getters y setters de todos los campos
    /**
     * Devuelve el identificador del documento en MongoDB; si es {@code null} al
     * guardar, Mongo genera uno.
     *
     * @return identificador del documento
     */
    public String getId() {
        return id;
    }

    /**
     * Establece el identificador del documento en MongoDB; si es {@code null}
     * al guardar, Mongo genera uno.
     *
     * @param id identificador del documento
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Devuelve el DNI del usuario al que pertenece la documentación; une esta
     * ficha con el usuario de MySQL y es único.
     *
     * @return DNI del usuario
     */
    public String getDniUsuari() {
        return dniUsuari;
    }

    /**
     * Establece el DNI del usuario al que pertenece la documentación; une esta
     * ficha con el usuario de MySQL y es único.
     *
     * @param dniUsuari DNI del usuario
     */
    public void setDniUsuari(String dniUsuari) {
        this.dniUsuari = dniUsuari;
    }

    /**
     * Devuelve el nombre del fichero del documento de identidad (por ejemplo,
     * dni_frontal.jpg).
     *
     * @return nombre del fichero de identidad
     */
    public String getNomIdentitat() {
        return nomIdentitat;
    }

    /**
     * Establece el nombre del fichero del documento de identidad (por ejemplo,
     * dni_frontal.jpg).
     *
     * @param nomIdentitat nombre del fichero de identidad
     */
    public void setNomIdentitat(String nomIdentitat) {
        this.nomIdentitat = nomIdentitat;
    }

    /**
     * Devuelve el tipo de contenido del documento de identidad (image/jpeg,
     * application/pdf...).
     *
     * @return tipo de contenido (MIME) del documento de identidad
     */
    public String getTipusIdentitat() {
        return tipusIdentitat;
    }

    /**
     * Establece el tipo de contenido del documento de identidad (image/jpeg,
     * application/pdf...).
     *
     * @param tipusIdentitat tipo de contenido (MIME) del documento de identidad
     */
    public void setTipusIdentitat(String tipusIdentitat) {
        this.tipusIdentitat = tipusIdentitat;
    }

    /**
     * Devuelve el contenido del documento de identidad, guardado como array de
     * bytes dentro de la propia ficha.
     *
     * @return contenido del documento en bytes
     */
    public byte[] getIdentitat() {
        return identitat;
    }

    /**
     * Establece el contenido del documento de identidad, guardado como array de
     * bytes dentro de la propia ficha.
     *
     * @param identitat contenido del documento en bytes
     */
    public void setIdentitat(byte[] identitat) {
        this.identitat = identitat;
    }

    /**
     * Devuelve el nombre del fichero del carnet de conducir.
     *
     * @return nombre del fichero del carnet
     */
    public String getNomCarnet() {
        return nomCarnet;
    }

    /**
     * Establece el nombre del fichero del carnet de conducir.
     *
     * @param nomCarnet nombre del fichero del carnet
     */
    public void setNomCarnet(String nomCarnet) {
        this.nomCarnet = nomCarnet;
    }

    /**
     * Devuelve el tipo de contenido del carnet de conducir (image/jpeg,
     * application/pdf...).
     *
     * @return tipo de contenido (MIME) del carnet
     */
    public String getTipusCarnet() {
        return tipusCarnet;
    }

    /**
     * Establece el tipo de contenido del carnet de conducir (image/jpeg,
     * application/pdf...).
     *
     * @param tipusCarnet tipo de contenido (MIME) del carnet
     */
    public void setTipusCarnet(String tipusCarnet) {
        this.tipusCarnet = tipusCarnet;
    }

    /**
     * Devuelve el contenido del carnet de conducir, guardado como array de
     * bytes.
     *
     * @return contenido del carnet en bytes
     */
    public byte[] getCarnet() {
        return carnet;
    }

    /**
     * Establece el contenido del carnet de conducir, guardado como array de
     * bytes.
     *
     * @param carnet contenido del carnet en bytes
     */
    public void setCarnet(byte[] carnet) {
        this.carnet = carnet;
    }

    /**
     * Devuelve el estado de la documentación: PENDENT (por defecto), APROVADA o
     * REBUTJADA.
     *
     * @return estado de la documentación
     */
    public Estat getEstat() {
        return estat;
    }

    /**
     * Establece el estado de la documentación: PENDENT (por defecto), APROVADA
     * o REBUTJADA.
     *
     * @param estat estado de la documentación
     */
    public void setEstat(Estat estat) {
        this.estat = estat;
    }

}
