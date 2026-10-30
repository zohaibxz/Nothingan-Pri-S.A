package cat.copernic.Projecte1_Equip5.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "documentacio")
public class Documentacio {

    public enum Estat { PENDENT, APROVADA, REBUTJADA }
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

    public Documentacio() {
    }

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDniUsuari() {
        return dniUsuari;
    }

    public void setDniUsuari(String dniUsuari) {
        this.dniUsuari = dniUsuari;
    }

    public String getNomIdentitat() {
        return nomIdentitat;
    }

    public void setNomIdentitat(String nomIdentitat) {
        this.nomIdentitat = nomIdentitat;
    }

    public String getTipusIdentitat() {
        return tipusIdentitat;
    }

    public void setTipusIdentitat(String tipusIdentitat) {
        this.tipusIdentitat = tipusIdentitat;
    }

    public byte[] getIdentitat() {
        return identitat;
    }

    public void setIdentitat(byte[] identitat) {
        this.identitat = identitat;
    }

    public String getNomCarnet() {
        return nomCarnet;
    }

    public void setNomCarnet(String nomCarnet) {
        this.nomCarnet = nomCarnet;
    }

    public String getTipusCarnet() {
        return tipusCarnet;
    }

    public void setTipusCarnet(String tipusCarnet) {
        this.tipusCarnet = tipusCarnet;
    }

    public byte[] getCarnet() {
        return carnet;
    }

    public void setCarnet(byte[] carnet) {
        this.carnet = carnet;
    }

    public Estat getEstat() {
        return estat;
    }

    public void setEstat(Estat estat) {
        this.estat = estat;
    }
    
    
}
