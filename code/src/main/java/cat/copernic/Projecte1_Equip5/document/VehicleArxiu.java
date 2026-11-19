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

    /**
     * Tipus de fitxer que es pot desar d'un vehicle.
     */
    public enum Tipus {
        
        /**
         * Foto del vehicle (jpg o png).
         */
        FOTO,
        
        /**
         * Document del vehicle (pdf o text).
         */
        DOCUMENT
    }

    @Id
    private String id;

    // ajuda a fer mes eficients les cerques ja que es crea un index sobre la matricula
    @Indexed
    private String matricula;   // uneix el fitxer amb el vehicle de MySQL

    private Tipus tipus;
    private boolean principal = false;
    private String nom;
    private String contentType;
    private long mida;
    private byte[] dades;
    private LocalDateTime dataPujada = LocalDateTime.now();

    /**
     * Constructor buit, necessari per a Spring Data MongoDB.
     */
    public VehicleArxiu() {
    }

    /**
     * Crea un fitxer de vehicle amb tots els atributs, inclòs l'identificador.
     * No és principal i la data de pujada és l'instant actual.
     *
     * @param id identificador del fitxer a MongoDB
     * @param matricula matrícula del vehicle de MySQL a què pertany el fitxer
     * @param tipus tipus de fitxer (FOTO o DOCUMENT)
     * @param nom nom del fitxer
     * @param contentType tipus de contingut (MIME) del fitxer, p. ex.
     * image/jpeg
     * @param mida mida del fitxer en bytes
     * @param dades contingut del fitxer, en bytes
     */
    public VehicleArxiu(String id, String matricula, Tipus tipus, String nom, String contentType, long mida, byte[] dades) {
        this.id = id;
        this.matricula = matricula;
        this.tipus = tipus;
        this.nom = nom;
        this.contentType = contentType;
        this.mida = mida;
        this.dades = dades;
    }

    /**
     * Crea un fitxer de vehicle sense identificador (MongoDB el genera en
     * desar- lo). No és principal i la data de pujada és l'instant actual.
     *
     * @param matricula matrícula del vehicle de MySQL a què pertany el fitxer
     * @param tipus tipus de fitxer (FOTO o DOCUMENT)
     * @param nom nom del fitxer
     * @param contentType tipus de contingut (MIME) del fitxer, p. ex.
     * image/jpeg
     * @param mida mida del fitxer en bytes
     * @param dades contingut del fitxer, en bytes
     */
    public VehicleArxiu(String matricula, Tipus tipus, String nom, String contentType, long mida, byte[] dades) {
        this.matricula = matricula;
        this.tipus = tipus;
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
     * Retorna la matrícula del vehicle a què pertany el fitxer; l'uneix amb el
     * vehicle de MySQL.
     *
     * @return matrícula del vehicle
     */
    public String getMatricula() {
        return matricula;
    }

    /**
     * Estableix la matrícula del vehicle a què pertany el fitxer; l'uneix amb
     * el vehicle de MySQL.
     *
     * @param matricula matrícula del vehicle
     */
    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    /**
     * Retorna el tipus de fitxer: FOTO o DOCUMENT.
     *
     * @return tipus de fitxer
     */
    public Tipus getTipus() {
        return tipus;
    }

    /**
     * Estableix el tipus de fitxer: FOTO o DOCUMENT.
     *
     * @param tipus tipus de fitxer
     */
    public void setTipus(Tipus tipus) {
        this.tipus = tipus;
    }

    /**
     * Retorna si és el fitxer principal del vehicle (p. ex. la foto que es
     * mostra al catàleg); per defecte no ho és.
     *
     * @return {@code true} si és el fitxer principal
     */
    public boolean isPrincipal() {
        return principal;
    }

    /**
     * Estableix si és el fitxer principal del vehicle (p. ex. la foto que es
     * mostra al catàleg); per defecte no ho és.
     *
     * @param principal {@code true} si és el fitxer principal
     */
    public void setPrincipal(boolean principal) {
        this.principal = principal;
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
     * Retorna el contingut del fitxer, en bytes.
     *
     * @return contingut en bytes
     */
    public byte[] getDades() {
        return dades;
    }

    /**
     * Estableix el contingut del fitxer, en bytes.
     *
     * @param dades contingut en bytes
     */
    public void setDades(byte[] dades) {
        this.dades = dades;
    }

    /**
     * Retorna la data i hora en què es va pujar el fitxer.
     *
     * @return data i hora de pujada
     */
    public LocalDateTime getDataPujada() {
        return dataPujada;
    }

    /**
     * Estableix la data i hora en què es va pujar el fitxer.
     *
     * @param dataPujada data i hora de pujada
     */
    public void setDataPujada(LocalDateTime dataPujada) {
        this.dataPujada = dataPujada;
    }

}
