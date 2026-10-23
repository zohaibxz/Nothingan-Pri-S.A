package cat.copernic.Projecte1_Equip5.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "documentacio")
public class Documentacio {

    public enum Estat { PENDENT, APROVADA, REBUTJADA }

    @Id
    private String id;

    @Indexed(unique = true)
    private String dniUsuari;          // une esta ficha con el usuario de MySQL

    private String nomIdentitat;
    private String tipusIdentitat;
    private byte[] identitat;

    private String nomCarnet;
    private String tipusCarnet;
    private byte[] carnet;

    private Estat estat = Estat.PENDENT;

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
