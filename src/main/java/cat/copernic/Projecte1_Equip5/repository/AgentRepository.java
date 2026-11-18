package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los agentes y administradores (entidad {@link Agent}); la
 * clave primaria es el DNI. Spring genera la implementación al arrancar, y las
 * consultas se derivan del nombre del método. Todas devuelven solo agentes
 * activos ({@code actiu = true}), para que los listados no muestren a los dados
 * de baja.
 *
 * @author ZOHAIB
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {

    /**
     * Agentes activos con un rol concreto. Por ejemplo,
     * {@code findByRolAndActiuTrue(Enums.Rol.Administrador)} devuelve solo los
     * administradores y {@code Enums.Rol.Agent} solo los agentes normales.
     *
     * @param rol rol a buscar (Agent o Administrador)
     * @return lista de agentes activos con ese rol (vacía si no hay ninguno)
     */
    List<Agent> findByRolAndActiuTrue(Enums.Rol rol);

    /**
     * Agentes activos con un rol concreto cuyo DNI contiene el texto indicado,
     * sin distinguir mayúsculas. Se usa en el buscador por DNI. Por ejemplo,
     * con rol Agent y "123" devuelve los agentes cuyo DNI contiene "123".
     *
     * @param rol rol a buscar (Agent o Administrador)
     * @param dni texto que debe contener el DNI
     * @return lista de agentes activos que cumplen ambas condiciones
     */
    List<Agent> findByRolAndActiuTrueAndDniUsuariContainingIgnoreCase(Enums.Rol rol, String dni);
}
