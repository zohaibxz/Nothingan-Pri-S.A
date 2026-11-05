package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Accés a la col·lecció historic_reserves de MongoDB. La clau és l'idReserva
 * (Long), així que save() crea el document la primera vegada i l'actualitza les
 * següents.
 */
@Repository
public interface HistoricReservaRepository extends MongoRepository<HistoricReserva, Long> {
}
