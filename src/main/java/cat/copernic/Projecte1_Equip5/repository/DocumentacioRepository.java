package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentacioRepository extends MongoRepository<Documentacio, String> {
    Optional<Documentacio> findByDniUsuari(String dniUsuari);
}
