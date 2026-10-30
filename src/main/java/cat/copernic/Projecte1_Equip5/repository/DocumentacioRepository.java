package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentacioRepository extends MongoRepository<Documentacio, String> {
    Optional<Documentacio> findByDniUsuari(String dniUsuari);
    /*Busca la documentación de un usuario por su DNI. Devuelve un Optional, 
    porque puede que ese usuario no haya subido nada todavía.  */
    void deleteByDniUsuari(String dniUsuari);
    /* Borra la documentación de ese usuario. No devuelve nada (void).Igual que antes, 
    Spring genera la consulta a partir del nombre, así que dniUsuari debe existir como atributo en la clase Documentacio. */
    
    /* Como son dos bases de datos distintas, no hay clave foránea que garantice la coherencia. 
    Si borras un cliente en SQL, su documentación en Mongo no se borra sola: tienes que llamar tú a deleteByDniUsuari(dni).
    Lo mismo al revés. Conviene hacerlo en el mismo método del servicio para que no queden documentos huérfanos.*/
}
