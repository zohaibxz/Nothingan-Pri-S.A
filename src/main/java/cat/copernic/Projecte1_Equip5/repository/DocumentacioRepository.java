package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio MongoDB de la documentación (DNI y carnet) de los usuarios.
 *
 * Como son dos bases de datos distintas, no hay clave foránea que garantice la
 * coherencia: si se borra un cliente en MySQL, su documentación en Mongo no se
 * borra sola y hay que llamar a {@link #deleteByDniUsuari}. Conviene hacerlo en
 * el mismo método del servicio para que no queden documentos huérfanos.
 */
@Repository
public interface DocumentacioRepository extends MongoRepository<Documentacio, String> {

    /**
     * Busca la documentación de un usuario por su DNI. Devuelve un Optional
     * porque puede que ese usuario no haya subido nada todavía.
     *
     * @param dniUsuari DNI del usuario
     * @return la documentación, o vacío si no ha subido nada
     */
    Optional<Documentacio> findByDniUsuari(String dniUsuari);

    /**
     * Borra la documentación de un usuario. Spring genera la consulta a partir
     * del nombre, así que {@code dniUsuari} debe existir como atributo en la
     * clase {@link Documentacio}.
     *
     * @param dniUsuari DNI del usuario
     */
    void deleteByDniUsuari(String dniUsuari);
}
