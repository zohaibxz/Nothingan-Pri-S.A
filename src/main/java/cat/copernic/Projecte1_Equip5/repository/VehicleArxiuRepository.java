/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori MongoDB dels fitxers privats dels vehicles ({@link VehicleArxiu}:
 * fotos i documents). Els fitxers s'enllacen amb el vehicle de MySQL per la
 * matrícula; com que són dues bases de dades diferents, no hi ha clau forana i
 * cal esborrar-los amb {@link #deleteByMatricula} quan s'elimina el vehicle.
 */
@Repository
public interface VehicleArxiuRepository extends MongoRepository<VehicleArxiu, String> {

    /**
     * Tots els fitxers (fotos i documents) d'un vehicle, dels més antics als
     * més recents.
     *
     * @param matricula matrícula del vehicle
     * @return llista de fitxers ordenats per data de pujada
     */
    List<VehicleArxiu> findByMatriculaOrderByDataPujadaAsc(String matricula);

    /**
     * Esborra tots els fitxers d'un vehicle.
     *
     * @param matricula matrícula del vehicle
     */
    void deleteByMatricula(String matricula);

    /**
     * Compta els fitxers d'un vehicle d'un tipus concret (foto o document), per
     * exemple per comprovar un màxim de fotos.
     *
     * @param matricula matrícula del vehicle
     * @param tipus tipus de fitxer
     * @return nombre de fitxers d'aquest tipus
     */
    long countByMatriculaAndTipus(String matricula, VehicleArxiu.Tipus tipus);

    /**
     * Fitxer marcat com a principal d'un vehicle per a un tipus (p. ex. la foto
     * principal que es mostra al catàleg).
     *
     * @param matricula matrícula del vehicle
     * @param tipus tipus de fitxer
     * @return el fitxer principal, o buit si no n'hi ha cap
     */
    java.util.Optional<VehicleArxiu> findFirstByMatriculaAndTipusAndPrincipalTrue(
            String matricula, VehicleArxiu.Tipus tipus);

    /**
     * Fitxers d'un vehicle d'un tipus concret, dels més antics als més recents.
     *
     * @param matricula matrícula del vehicle
     * @param tipus tipus de fitxer (foto o document)
     * @return llista de fitxers ordenats per data de pujada
     */
    List<VehicleArxiu> findByMatriculaAndTipusOrderByDataPujadaAsc(
            String matricula, VehicleArxiu.Tipus tipus);
}
