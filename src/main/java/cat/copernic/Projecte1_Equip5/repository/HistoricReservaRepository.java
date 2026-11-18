package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Accés a la col·lecció historic_reserves de MongoDB. La clau és l'idReserva
 * (Long), així que save() crea el document la primera vegada i l'actualitza les
 * següents.
 */
@Repository
public interface HistoricReservaRepository extends MongoRepository<HistoricReserva, Long> {

    /**
     * Reserves de l'històric d'un client (pel DNI), amb l'ordre indicat. Per a
     * tots els clients s'usa findAll(Sort), que ja ve heretat.
     *
     * @param dniClient DNI del client
     * @param sort ordre del resultat
     * @return reserves del client
     */
    List<HistoricReserva> findByDniClient(String dniClient, Sort sort);
}
