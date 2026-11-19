package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Usuari;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los usuarios (entidad {@link Usuari}, clave primaria el
 * DNI). Da acceso a la tabla de usuarios sin escribir SQL: Spring genera la
 * implementación al arrancar, con métodos heredados como {@code save} o
 * {@code findById} y las consultas derivadas del nombre que se declaran aquí.
 * Trabaja con la clase padre, así que encuentra clientes, agentes y
 * administradores.
 */
@Repository
public interface UsuariRepository extends JpaRepository<Usuari, String> {

    /**
     * Busca un usuario por su email. Se usa en el login: Spring Security recibe
     * el email, se busca el usuario y se compara la contraseña. Devuelve un
     * {@link Optional} porque puede que no exista ninguno con ese email.
     *
     * @param email email del usuario
     * @return el usuario, o vacío si no existe
     */
    Optional<Usuari> findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario con ese email. Se usa en el registro,
     * para evitar que el email esté ya cogido antes de guardar.
     *
     * @param email email a comprobar
     * @return {@code true} si ya hay un usuario con ese email
     */
    boolean existsByEmail(String email);

    /**
     * Usuarios dados de baja ({@code actiu = false}): clientes, agentes y
     * administradores.
     *
     * @return lista de usuarios inactivos (vacía si no hay ninguno)
     */
    List<Usuari> findByActiuFalse();
}
