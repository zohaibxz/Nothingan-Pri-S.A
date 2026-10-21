package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    
    @EntityGraph(attributePaths = "vehicle")
    List<Reserva> findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
            String dniClient, Collection<Enums.EstatReserva> estats);

    
    @EntityGraph(attributePaths = "vehicle")
    List<Reserva> findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(
            String dniClient, Collection<Enums.EstatReserva> estats);
}
