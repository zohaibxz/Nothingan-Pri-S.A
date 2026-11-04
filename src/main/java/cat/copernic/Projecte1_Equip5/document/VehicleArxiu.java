/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.document;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Fitxer PRIVAT d'un vehicle (foto jpg/png o document pdf/text). Només el
 * personal (agent/administrador) hi pot accedir.
 */
@Document(collection = "vehicle_arxius")
public class VehicleArxiu {

    public enum Tipus {
        FOTO, DOCUMENT
    }

    @Id
    private String id;

    @Indexed
    private String matricula;   // uneix el fitxer amb el vehicle de MySQL

    private Tipus tipus;
    private boolean principal = false;
    private String nom;
    private String contentType;
    private long mida;
    private byte[] dades;
    private LocalDateTime dataPujada = LocalDateTime.now();

    public VehicleArxiu() {
    }

    public VehicleArxiu(String id, String matricula, Tipus tipus, String nom, String contentType, long mida, byte[] dades) {
        this.id = id;
        this.matricula = matricula;
        this.tipus = tipus;
        this.nom = nom;
        this.contentType = contentType;
        this.mida = mida;
        this.dades = dades;
    }

    public VehicleArxiu(String matricula, Tipus tipus, String nom, String contentType, long mida, byte[] dades) {
        this.matricula = matricula;
        this.tipus = tipus;
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

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Tipus getTipus() {
        return tipus;
    }

    public void setTipus(Tipus tipus) {
        this.tipus = tipus;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public void setPrincipal(boolean principal) {
        this.principal = principal;
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
