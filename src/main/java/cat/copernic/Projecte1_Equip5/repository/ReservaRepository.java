package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @EntityGraph(attributePaths = "vehicle")
    List<Reserva> findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
            String dniClient, Collection<Enums.EstatReserva> estats);

    @EntityGraph(attributePaths = "vehicle")
    List<Reserva> findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(
            String dniClient, Collection<Enums.EstatReserva> estats);

    /**
     * Busca una reserva pel seu codi (p. ex. EX-2026-0001).
     */
    @EntityGraph(attributePaths = "vehicle")
    Optional<Reserva> findByCodiReserva(String codiReserva);

    /**
     * Compta les reserves d'un vehicle, en els estats indicats, que se solapen
     * amb [inici, fi). El dia de devolució queda lliure: per això es compara
     * amb < i > estrictes (igual que Reserva.solapaAmb).
     */
    @Query("select count(r) from Reserva r "
            + "where r.vehicle.matricula = :matricula "
            + "and r.estat in :estats "
            + "and r.dataInici < :fi and r.dataFi > :inici")
    long comptarSolapaments(@Param("matricula") String matricula,
            @Param("inici") LocalDate inici,
            @Param("fi") LocalDate fi,
            @Param("estats") Collection<Enums.EstatReserva> estats);
    
     /**
     * Indica si un vehicle té alguna reserva en l'estat indicat
     * (p. ex. Lliurada = el client l'ha recollit i encara no l'ha tornat).
     */
    boolean existsByVehicle_MatriculaAndEstat(String matricula, Enums.EstatReserva estat);
}
