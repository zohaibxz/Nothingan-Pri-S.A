package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Usuari;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/* Es un repositorio de Spring Data JPA: una interfaz que te da acceso a la tabla de usuarios 
sin que tengas que escribir nada de SQL ni la implementación. Spring la genera automáticamente al arrancar. */
@Repository // marca la interfaz como componente de acceso a datos, para que Spring la registre y puedas inyectarla en otras clases.
public interface UsuariRepository extends JpaRepository<Usuari, String>{ // hereda metode de JpaRepository como findByEmail o save en lugar de escribir SQL a mano select * where email...
    // Qué es Optional Es un contenedor que puede tener un usuario o estar vacío. Obliga a gestionar el caso de “no existe”:
    Optional<Usuari> findByEmail(String email);
    //findByEmail: lo usarás en el login. Spring Security recibe el email, tú buscas el usuario y comparas la contraseña. Es justo el Usuari u que luego pasa a AuthorityType.of(u)
    
    boolean existsByEmail(String email);

    // Usuarios dados de baja (actiu = false): clientes, agentes y administradores
    List<Usuari> findByActiuFalse();
    /* lo usarás en el registro, para comprobar que el email no esté ya cogido antes de guardar: */
}