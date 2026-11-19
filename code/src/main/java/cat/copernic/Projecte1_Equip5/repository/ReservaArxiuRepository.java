package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Accés a la col·lecció reserva_arxius de MongoDB, on es guarden les fotos
 * d'una reserva (lliurament i devolució del vehicle).
 */
@Repository
public interface ReservaArxiuRepository extends MongoRepository<ReservaArxiu, String> {

    /**
     * Fotos d'una reserva en un moment concret, SENSE els bytes (fields amb
     * 'dades': 0), perquè el llistat només necessita l'id i el nom i no cal
     * carregar fins a 50 MB de la base de dades. Els bytes es llegeixen amb
     * findById quan es mostra una foto. L'ordre el decideix qui crida amb el
     * paràmetre Sort.
     *
     * @param idReserva identificador de la reserva
     * @param moment moment de la foto dins la reserva (p. ex. lliurament o
     * devolució)
     * @param sort ordre del resultat
     * @return fotos de la reserva sense el contingut binari
     */
    @Query(value = "{ 'idReserva': ?0, 'moment': ?1 }", fields = "{ 'dades': 0 }")
    List<ReservaArxiu> findMetadades(Long idReserva, ReservaArxiu.Moment moment, Sort sort);
}
