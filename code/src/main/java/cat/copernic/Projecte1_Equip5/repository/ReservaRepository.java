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

/**
 * Repositori JPA de les reserves (MySQL). Defineix les consultes de negoci
 * sobre {@link Reserva}: llistats d'un client, control de solapaments de dates,
 * recomptes per estat i cerca filtrada per als agents. Les consultes derivades
 * del nom les genera Spring Data; les altres porten @Query.
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Reserves d'un client que estan en algun dels estats indicats (p. ex. les
     * actives o pendents), ordenades per data d'inici ascendent. Carrega el
     * vehicle amb la reserva per evitar consultes addicionals.
     *
     * @param dniClient DNI del client
     * @param estats estats que s'han d'incloure
     * @param pageable pàgina i mida del resultat
     * @return pàgina de reserves, de la més propera a la més llunyana
     */
    @EntityGraph(attributePaths = "vehicle")
    Page<Reserva> findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
            String dniClient, Collection<Enums.EstatReserva> estats, Pageable pageable);

    /**
     * Reserves d'un client que NO estan en cap dels estats indicats (p. ex. el
     * seu historial un cop excloses les actives), ordenades per data d'inici
     * descendent. Carrega el vehicle amb la reserva.
     *
     * @param dniClient DNI del client
     * @param estats estats que s'han d'excloure
     * @param pageable pàgina i mida del resultat
     * @return pàgina de reserves, de la més recent a la més antiga
     */
    @EntityGraph(attributePaths = "vehicle")
    Page<Reserva> findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(
            String dniClient, Collection<Enums.EstatReserva> estats, Pageable pageable);

    /**
     * Busca una reserva pel seu codi (p. ex. EX-2026-0001).
     *
     * @param codiReserva codi únic de la reserva
     * @return la reserva, o buit si no existeix cap amb aquest codi
     */
    @EntityGraph(attributePaths = "vehicle")
    Optional<Reserva> findByCodiReserva(String codiReserva);

    /**
     * Compta les reserves d'un vehicle, en els estats indicats, que se solapen
     * amb [inici, fi). El dia de devolució queda lliure: per això es compara
     * amb {@code <} i {@code >} estrictes (igual que Reserva.solapaAmb).
     *
     * @param matricula matrícula del vehicle
     * @param inici primer dia del període consultat
     * @param fi dia de devolució del període consultat (exclòs)
     * @param estats estats de reserva que bloquegen el vehicle
     * @return nombre de reserves solapades (0 = vehicle lliure)
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
     *
     * @param matricula matrícula del vehicle
     * @param estat estat de reserva que es vol comprovar
     * @return {@code true} si hi ha almenys una reserva en aquest estat
     */
    boolean existsByVehicle_MatriculaAndEstat(String matricula, Enums.EstatReserva estat);

    /**
     * Totes les reserves que estan en algun dels estats indicats, ordenades per
     * data d'inici ascendent. Carrega vehicle i client amb la reserva.
     *
     * @param estats estats que s'han d'incloure
     * @return llista de reserves, de la data d'inici més propera a la més
     * llunyana
     */
    @EntityGraph(attributePaths = {"vehicle", "client"})
    List<Reserva> findByEstatInOrderByDataIniciAsc(
            Collection<Enums.EstatReserva> estats);

    /**
     * Compta les reserves que estan en l'estat indicat (p. ex. per als
     * indicadors del tauler).
     *
     * @param estat estat de la reserva
     * @return nombre de reserves en aquest estat
     */
    long countByEstat(Enums.EstatReserva estat);

    /**
     * Compta les reserves d'un client que estan en l'estat indicat.
     *
     * @param dniClient DNI del client
     * @param estat estat de la reserva
     * @return nombre de reserves del client en aquest estat
     */
    long countByClient_DniUsuariAndEstat(String dniClient, Enums.EstatReserva estat);

    /**
     * Nombre de reserves en curs (en els estats indicats, p. ex. Activa i
     * Lliurada) de cada client. Només retorna els clients que en tenen almenys
     * una. Cada fila és {@code [dniClient, nombreReserves]}.
     *
     * @param estats estats que es compten
     * @return files {@code [String dni, Long total]}
     */
    @Query("select r.client.dniUsuari, count(r) from Reserva r "
            + "where r.estat in :estats group by r.client.dniUsuari")
    List<Object[]> comptarReservesPerClient(@Param("estats") Collection<Enums.EstatReserva> estats);

    /**
     * Busca una reserva per identificador carregant-hi també el client i el
     * vehicle, per poder mostrar-ne el detall sense consultes addicionals.
     *
     * @param idReserva identificador de la reserva
     * @return la reserva amb client i vehicle, o buit si no existeix
     */
    @EntityGraph(attributePaths = {"vehicle", "client"})
    Optional<Reserva> findWithClientAndVehicleByIdReserva(Long idReserva);

    /**
     * Reserves d'un client que estan en algun dels estats indicats. S'utilitza
     * en la baixa d'un client, per saber si té reserves pendents o en curs
     * abans d'esborrar-lo.
     *
     * @param dniClient DNI del client
     * @param estats estats que s'han de buscar
     * @return llista de reserves del client en aquests estats
     */
    List<Reserva> findByClient_DniUsuariAndEstatIn(String dniClient, Collection<Enums.EstatReserva> estats);

    /**
     * Cerca de reserves per a l'agent amb filtres opcionals combinables: cada
     * paràmetre {@code null} s'ignora. DNI, email, matrícula i codi
     * coincideixen parcialment i sense distingir majúscules; les dates filtren
     * per data de creació de la reserva (inici inclòs, fi exclòs).
     *
     * @param dni part del DNI del client (opcional)
     * @param email part de l'email del client (opcional)
     * @param matricula part de la matrícula del vehicle (opcional)
     * @param codi part del codi de reserva (opcional)
     * @param dataInici data i hora mínima de creació, inclosa (opcional)
     * @param dataFi data i hora màxima de creació, exclosa (opcional)
     * @param estat estat exacte de la reserva (opcional)
     * @return reserves que compleixen els filtres, de la més recent a la més
     * antiga
     */
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
     *
     * @param lliurada estat Lliurada (vehicle recollit i no tornat)
     * @param activa estat Activa (reserva confirmada en curs)
     * @param avui data d'avui, per comprovar si cau dins del període
     * @return matrícules sense repetir dels vehicles llogats ara mateix
     */
    @Query("select distinct r.vehicle.matricula from Reserva r "
            + "where r.estat = :lliurada "
            + "or (r.estat = :activa and r.dataInici <= :avui and r.dataFi > :avui)")
    List<String> matriculesLlogades(@Param("lliurada") Enums.EstatReserva lliurada,
            @Param("activa") Enums.EstatReserva activa,
            @Param("avui") LocalDate avui);
    
    /**
     * Indica si un vehicle té alguna reserva, sigui de l'estat que sigui
     * (activa, lliurada, finalitzada o anul·lada).
     *
     * @param matricula matrícula del vehicle
     * @return {@code true} si hi ha almenys una reserva del vehicle
     */
    boolean existsByVehicle_Matricula(String matricula);
}
