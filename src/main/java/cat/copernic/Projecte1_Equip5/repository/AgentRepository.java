package cat.copernic.Projecte1_Equip5.repository;


import cat.copernic.Projecte1_Equip5.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ZOHAIB
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long>{

}

  

