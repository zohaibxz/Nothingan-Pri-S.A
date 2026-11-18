package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Repositori JPA dels clients (entitat {@link Client}). La clau primària és el
 * DNI (String), heretat d'{@code Usuari}. Els agents també són clients (Agent
 * hereta de Client), per això algunes consultes distingeixen el tipus.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    /**
     * Clients filtrats per l'estat de validació (Pendent, Validat o Rebutjat).
     * Serveix perquè agents i administradors vegin la llista de clients
     * pendents i puguin validar-los o rebutjar-los.
     *
     * @param estatValidacio estat de validació que es vol consultar
     * @return llista de clients en aquest estat (buida si no n'hi ha cap)
     */
    List<Client> findByEstatValidacio(Enums.EstatValidacio estatValidacio);

    /**
     * Només els clients normals, excloent els agents i administradors (que
     * també són {@code Client} per herència). S'usa als llistats de clients
     * perquè no apareguin usuaris del personal.
     *
     * @return llista de clients el tipus exacte dels quals és {@code Client}
     */
    @Query("SELECT c FROM Client c WHERE TYPE(c) = Client")
    List<Client> findNomesClients();

    /**
     * Clients (no personal) actius amb l'estat de validació indicat, filtrats
     * per nom, cognoms, DNI o correu. Un text buit no filtra.
     *
     * @param estat estat de validació requerit
     * @param text text a cercar, ja sense espais als extrems
     * @param pageable límit de resultats
     * @return clients que coincideixen, ordenats per cognoms i nom
     */
    @Query("SELECT c FROM Client c WHERE TYPE(c) = Client AND c.actiu = true "
            + "AND c.estatValidacio = :estat AND (:text = '' "
            + "OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :text, '%')) "
            + "OR LOWER(c.cognoms) LIKE LOWER(CONCAT('%', :text, '%')) "
            + "OR LOWER(c.dniUsuari) LIKE LOWER(CONCAT('%', :text, '%')) "
            + "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :text, '%'))) "
            + "ORDER BY c.cognoms, c.nom")
    List<Client> cercarReservables(@Param("estat") Enums.EstatValidacio estat,
            @Param("text") String text, Pageable pageable);
}
