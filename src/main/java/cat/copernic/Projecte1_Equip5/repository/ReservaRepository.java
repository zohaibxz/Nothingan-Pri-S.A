package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @EntityGraph(attributePaths = "vehicle")
    Page<Reserva> findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
            String dniClient, Collection<Enums.EstatReserva> estats, Pageable pageable);

    @EntityGraph(attributePaths = "vehicle")
    Page<Reserva> findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(
            String dniClient, Collection<Enums.EstatReserva> estats, Pageable pageable);

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
     * Indica si un vehicle té alguna reserva en l'estat indicat (p. ex.
     * Lliurada = el client l'ha recollit i encara no l'ha tornat).
     */
    boolean existsByVehicle_MatriculaAndEstat(String matricula, Enums.EstatReserva estat);

    @EntityGraph(attributePaths = {"vehicle", "client"})
    List<Reserva> findByEstatInOrderByDataIniciAsc(
            Collection<Enums.EstatReserva> estats);

    long countByEstat(Enums.EstatReserva estat);

    @EntityGraph(attributePaths = {"vehicle", "client"})
    Optional<Reserva> findWithClientAndVehicleByIdReserva(Long idReserva);

    // para tema de baja client que me ha una lsit de las reservas disponibles antes de borrar 
    List<Reserva> findByClient_DniUsuariAndEstatIn(String dniClient, Collection<Enums.EstatReserva> estats);

    @EntityGraph(attributePaths = {"vehicle", "client"})
    @Query("SELECT r FROM Reserva r WHERE "
            + "(:dni IS NULL OR LOWER(r.client.dniUsuari) LIKE LOWER(CONCAT('%', :dni, '%'))) "
            + "AND (:email IS NULL OR LOWER(r.client.email) LIKE LOWER(CONCAT('%', :email, '%'))) "
            + "AND (:matricula IS NULL OR LOWER(r.vehicle.matricula) LIKE LOWER(CONCAT('%', :matricula, '%'))) "
            + "AND (:codi IS NULL OR LOWER(r.codiReserva) LIKE LOWER(CONCAT('%', :codi, '%'))) "
            + "AND (:dataInici IS NULL OR r.dataCreacio >= :dataInici) "
            + "AND (:dataFi IS NULL OR r.dataCreacio < :dataFi) "
            + "AND (:estat IS NULL OR r.estat = :estat) "
            + "ORDER BY r.dataCreacio DESC")
    List<Reserva> llistarReservesAgentFiltrades(
            @Param("dni") String dni,
            @Param("email") String email,
            @Param("matricula") String matricula,
            @Param("codi") String codi,
            @Param("dataInici") LocalDateTime dataInici,
            @Param("dataFi") LocalDateTime dataFi,
            @Param("estat") Enums.EstatReserva estat);

    
     /**
     * Matrícules dels vehicles llogats ara mateix: reserves Lliurada (recollit
     * i no tornat) o Activa amb avui dins del període. El dia de devolució
     * queda lliure (dataFi exclusiva), igual que a comptarSolapaments.
     */
    @Query("select distinct r.vehicle.matricula from Reserva r "
            + "where r.estat = :lliurada "
            + "or (r.estat = :activa and r.dataInici <= :avui and r.dataFi > :avui)")
    List<String> matriculesLlogades(@Param("lliurada") Enums.EstatReserva lliurada,
            @Param("activa") Enums.EstatReserva activa,
            @Param("avui") LocalDate avui);
}
