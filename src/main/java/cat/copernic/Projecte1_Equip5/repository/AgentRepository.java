package cat.copernic.Projecte1_Equip5.repository;


import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ZOHAIB
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, String>{
    
    // se declaran aqui para usarlo mas adelante en otras clases ejemplo abajo 
    // ejemplo agentRepository.findByRol(Enums.Rol.Administrador);  // solo los administradores
    //agentRepository.findByRol(Enums.Rol.Agent);          // solo los agentes normales
    List<Agent> findByRol(Enums.Rol rol); 
    List<Agent> findByRolAndDniUsuariContainingIgnoreCase(Enums.Rol rol, String dni);
    /* agentRepository.findByRolAndDniUsuariContainingIgnoreCase(Enums.Rol.Agent, "123");
// Agentes con rol Agent cuyo DNI contenga "123" */
}

  
 
