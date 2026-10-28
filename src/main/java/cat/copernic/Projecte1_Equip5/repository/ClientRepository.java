package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori de clients. La clau primària és el DNI (String), heretat d'Usuari.
 * POSSIBLEMENT INCORRECTE: pot coincidir amb el de RF80/CRUD de clients.
 * Comprova develop abans de fer commit; si ja existeix, esborra aquest.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, String> {
    List<Client> findByEstatValidacio(Enums.EstatValidacio estatValidacio);
    // Razón: Para agregar una consulta que devuelva los clientes filtrados por su estado:
    /* Razón: Debe exponer los endpoints para que los Agentes/Administradores visualicen la 
    lista de clientes pendientes y ejecuten la acción de validar o rechazar.*/
}
