package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lògica de negoci de les reserves (RF49, RF50, RF52, RF53, RF54 i RF55). Les
 * entrades incorrectes es notifiquen amb IllegalArgumentException i els
 * conflictes de disponibilitat amb IllegalStateException; els missatges són en
 * català perquè el controlador els mostra tal qual a l'usuari.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    /**
     * Estats que es consideren "properes reserves".
     */
    private static final Collection<Enums.EstatReserva> ESTATS_PROPERES
            = List.of(Enums.EstatReserva.Activa, Enums.EstatReserva.Lliurada);

    /**
     * Estats que bloquegen un vehicle (el mateix criteri que "properes").
     */
    private static final Collection<Enums.EstatReserva> ESTATS_QUE_BLOQUEJEN = ESTATS_PROPERES;

    /**
     * Hores que té un dia de lloguer.
     */
    private static final int HORES_PER_DIA = 24;

    private final ReservaRepository reservaRepository;
    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;
    private final HistoricReservaService historicReservaService;
    private final ReservaArxiuService reservaArxiuService;

    /**
     * Dies d'antelació: si falten més d'aquests dies per a la data d'inici, el
     * client recupera l'import de la reserva en anul·lar-la. Millora prevista:
     * llegir-lo d'application.properties amb @Value (l'enunciat el tracta com a
     * paràmetre de sistema).
     */
    private static final int DIES_ANTELACIO = 2;

    /**
     * Missatge quan una reserva no existeix o no és del client que la demana
     * (és el mateix en tots dos casos perquè no es pugui saber si existeix).
     */
    private static final String RESERVA_NO_TROBADA = "No s'ha trobat la reserva.";

    /**
     * Nombre de reserves que es mostren per pàgina als llistats del client.
     */
    private static final int RESERVES_PER_PAGINA = 5;

    /**
     * Constructor amb injecció de dependències.
     *
     * @param reservaRepository repositori de reserves
     * @param vehicleRepository repositori de vehicles
     * @param clientRepository repositori de clients
     * @param historicReservaService servei que registra l'històric a MongoDB
     * @param reservaArxiuService servei de les fotos de lliurament i devolució
     */
    public ReservaService(ReservaRepository reservaRepository,
            VehicleRepository vehicleRepository,
            ClientRepository clientRepository,
            HistoricReservaService historicReservaService,
            ReservaArxiuService reservaArxiuService) {
        this.reservaRepository = reservaRepository;
        this.vehicleRepository = vehicleRepository;
        this.clientRepository = clientRepository;
        this.historicReservaService = historicReservaService;
        this.reservaArxiuService = reservaArxiuService;
    }

    // ---------- RF52: llistat ----------
    /**
     * Reserves actives o lliurades d'un client, ordenades per data d'inici i
     * repartides en pàgines.
     *
     * @param dniClient DNI del client
     * @param pagina número de pàgina (comença a 0; un valor negatiu es tracta
     * com 0)
     * @return pàgina de properes reserves
     */
    @Transactional(readOnly = true)
    public Page<Reserva> llistarProperes(String dniClient, int pagina) {
        return reservaRepository.findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
                dniClient, ESTATS_PROPERES, PageRequest.of(Math.max(pagina, 0), RESERVES_PER_PAGINA));
    }

    /**
     * Reserves d'un client finalitzades o anul·lades, de la més recent a la més
     * antiga i repartides en pàgines.
     *
     * @param dniClient DNI del client
     * @param pagina número de pàgina (comença a 0; un valor negatiu es tracta
     * com 0)
     * @return pàgina de l'historial
     */
    @Transactional(readOnly = true)
    public Page<Reserva> llistarHistorial(String dniClient, int pagina) {
        return reservaRepository.findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(
                dniClient, ESTATS_PROPERES, PageRequest.of(Math.max(pagina, 0), RESERVES_PER_PAGINA));
    }

    // ---------- RF50: alta de reserva ----------
    /**
     * Valida el període: les dues dates són obligatòries, l'inici no pot ser
     * anterior a avui i el mínim és 1 dia (el dia de devolució queda lliure).
     *
     * @param inici primer dia del lloguer
     * @param fi dia de devolució (el dia queda lliure)
     * @throws IllegalArgumentException si el període no és vàlid
     */
    public void validarDates(LocalDate inici, LocalDate fi) {
        if (inici == null || fi == null) {
            throw new IllegalArgumentException("Cal indicar la data d'inici i la data de fi.");
        }
        if (inici.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La data d'inici no pot ser anterior a avui.");
        }
        if (!fi.isAfter(inici)) {
            throw new IllegalArgumentException("La data de fi ha de ser posterior a la d'inici (mínim 1 dia).");
        }
    }

    /**
     * Nombre de dies de lloguer: diferència entre les dues dates, sense sumar 1
     * (del 22 al 25 són 3 dies).
     *
     * @param inici data de recollida
     * @param fi data de devolució
     * @return nombre de dies
     */
    public long calcularDies(LocalDate inici, LocalDate fi) {
        return ChronoUnit.DAYS.between(inici, fi);
    }

    /**
     * Indica si la durada de la reserva compleix el mínim i el màxim de dies de
     * lloguer del vehicle.
     *
     * @param vehicle vehicle reservat
     * @param inici data d'inici
     * @param fi data de fi
     * @return true si els dies estan dins del rang permès
     */
    public boolean diesPermesos(Vehicle vehicle, LocalDate inici, LocalDate fi) {
        long dies = calcularDies(inici, fi);
        return dies >= vehicle.getMin_dies() && dies <= vehicle.getMax_dies();
    }

    /**
     * Com diesPermesos, però llança un error amb el motiu si la durada no és
     * permesa (RN20, RN21).
     *
     * @param vehicle vehicle reservat
     * @param inici data d'inici
     * @param fi data de fi
     * @throws IllegalArgumentException si hi ha menys dies que el mínim o més
     * que el màxim
     */
    public void validarDiesPermesos(Vehicle vehicle, LocalDate inici, LocalDate fi) {
        if (!diesPermesos(vehicle, inici, fi)) {
            long dies = calcularDies(inici, fi);
            if (dies < vehicle.getMin_dies()) {
                throw new IllegalArgumentException("Aquest vehicle es lloga com a mínim "
                        + vehicle.getMin_dies() + " dies.");
            }
            throw new IllegalArgumentException("Aquest vehicle es lloga com a màxim "
                    + vehicle.getMax_dies() + " dies.");
        }
    }

    /**
     * Hores de lloguer: els dies de lloguer per 24.
     *
     * @param inici data de recollida
     * @param fi data de devolució
     * @return nombre d'hores
     */
    public long calcularHores(LocalDate inici, LocalDate fi) {
        return calcularDies(inici, fi) * HORES_PER_DIA;
    }

    /**
     * Import del lloguer: preu per hora del vehicle per les hores de lloguer,
     * amb 2 decimals (arrodonit HALF_UP). Es fa servir BigDecimal perquè amb
     * double es perdrien cèntims.
     *
     * @param vehicle vehicle reservat
     * @param inici data de recollida
     * @param fi data de devolució
     * @return import del lloguer (sense la fiança)
     */
    public BigDecimal calcularImportTotal(Vehicle vehicle, LocalDate inici, LocalDate fi) {
        return BigDecimal.valueOf(vehicle.getPreu_hores())
                .multiply(BigDecimal.valueOf(calcularHores(inici, fi)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fiança del client: la fiança estàndard del vehicle; si el client és
     * premium en paga el 75 % (percentatge fix al codi).
     *
     * @param vehicle vehicle reservat
     * @param client client que reserva
     * @return fiança amb 2 decimals
     * @throws IllegalArgumentException si el vehicle no té fiança configurada
     */
    public BigDecimal calcularFianca(Vehicle vehicle, Client client) {
        if (vehicle.getFianca() == null) {
            throw new IllegalArgumentException("El vehicle no té una fiança configurada.");
        }

        BigDecimal fianca = vehicle.getFianca();

        if (Boolean.TRUE.equals(client.getPremium())) {
            fianca = fianca.multiply(new BigDecimal("0.75"));
        }

        return fianca.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Un vehicle està disponible si no és Inactiu i no té cap reserva Activa o
     * Lliurada que se solapi amb el període.
     *
     * @param vehicle vehicle a comprovar
     * @param inici data de recollida
     * @param fi data de devolució
     * @return true si es pot reservar en aquestes dates
     */
    @Transactional(readOnly = true)
    public boolean estaDisponible(Vehicle vehicle, LocalDate inici, LocalDate fi) {
        if (vehicle.getEstat() == Enums.EstatVehicle.Inactiu) {
            return false;
        }
        return reservaRepository.comptarSolapaments(
                vehicle.getMatricula(), inici, fi, ESTATS_QUE_BLOQUEJEN) == 0;
    }

    /**
     * Vehicles disponibles en el període, opcionalment d'una sola categoria. Es
     * carreguen tots els vehicles i es filtren aquí, així no cal tocar
     * VehicleRepository.
     *
     * @param inici data de recollida
     * @param fi data de devolució
     * @param categoria categoria a filtrar, o null per a totes
     * @return vehicles disponibles
     * @throws IllegalArgumentException si el període no és vàlid
     */
    @Transactional(readOnly = true)
    public List<Vehicle> vehiclesDisponibles(LocalDate inici, LocalDate fi, Enums.CategoriaVehicle categoria) {
        validarDates(inici, fi);
        return vehicleRepository.findAll().stream()
                .filter(v -> categoria == null || v.getCategoria() == categoria)
                .filter(v -> diesPermesos(v, inici, fi))
                .filter(v -> estaDisponible(v, inici, fi))
                .toList();
    }

    /**
     * Cerca un vehicle per la seva matrícula.
     *
     * @param matricula matrícula del vehicle
     * @return el vehicle
     * @throws IllegalArgumentException si no existeix
     */
    @Transactional(readOnly = true)
    public Vehicle obtenirVehicle(String matricula) {
        return vehicleRepository.findById(matricula)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat el vehicle indicat."));
    }

    /**
     * Cerca un client pel seu DNI.
     *
     * @param dniClient DNI del client
     * @return el client
     * @throws IllegalArgumentException si no existeix
     */
    @Transactional(readOnly = true)
    public Client obtenirClient(String dniClient) {
        return clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat el client."));
    }

    /**
     * Genera el codi de reserva amb el format EX-2026-0001 a partir del
     * recompte. POSSIBLEMENT INCORRECTE: si s'esborren reserves el codi es
     * podria repetir.
     *
     * @return codi de reserva nou
     */
    public String generarCodiReserva() {
        return String.format("EX-%d-%04d", LocalDate.now().getYear(), reservaRepository.count() + 1);
    }

    /**
     * Crea la reserva (RF50): valida, comprova la disponibilitat, calcula
     * import i fiança, la desa en estat Activa a MySQL i la registra a
     * l'històric de MongoDB. El pagament no s'implementa. Si Mongo falla, la
     * transacció desfà el desat a MySQL (però no al revés).
     *
     * @param dniClient DNI del client que reserva
     * @param matricula matrícula del vehicle
     * @param inici data de recollida
     * @param fi data de devolució
     * @return la reserva creada, amb el seu codi
     * @throws IllegalArgumentException si les dades no són vàlides
     * @throws IllegalStateException si el vehicle ja no està disponible
     */
    // RN01: aplicació multiusuari. SERIALIZABLE evita que dos clients reservin el mateix vehicle a la vegada
    // (o que dues reserves rebin el mateix codi): si coincideixen, una acaba amb ConcurrencyFailureException,
    // que GestorErrorsGlobal explica a l'usuari i li demana que ho torni a provar.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Reserva crearReserva(String dniClient, String matricula, LocalDate inici, LocalDate fi) {
        validarDates(inici, fi);
        Client client = obtenirClient(dniClient);
        Vehicle vehicle = obtenirVehicle(matricula);
        validarDiesPermesos(vehicle, inici, fi);

        if (!estaDisponible(vehicle, inici, fi)) {
            log.warn("No s'ha creat la reserva: el vehicle {} ja no està disponible del {} al {}",
                    matricula, inici, fi);
            throw new IllegalStateException("El vehicle ja no està disponible per a les dates triades.");
        }

        Reserva reserva = new Reserva(
                generarCodiReserva(),
                vehicle,
                client,
                inici,
                fi,
                calcularImportTotal(vehicle, inici, fi),
                calcularFianca(vehicle, client),
                Enums.EstatReserva.Activa
        );
        Reserva guardada = reservaRepository.save(reserva);
        historicReservaService.registrarCanvi(guardada);
        log.info("Reserva {} creada (vehicle {}, del {} al {})",
                guardada.getCodiReserva(), matricula, inici, fi);
        return guardada;
    }

    /**
     * Retorna una reserva pel seu codi, només si pertany al client indicat.
     *
     * @param codi codi de la reserva
     * @param dniClient DNI del client que la demana
     * @return la reserva, amb el vehicle carregat
     * @throws IllegalArgumentException si no existeix o no és del client
     */
    @Transactional(readOnly = true)
    public Reserva obtenirReservaDelClient(String codi, String dniClient) {
        Reserva reserva = reservaRepository.findByCodiReserva(codi)
                .orElseThrow(() -> new IllegalArgumentException(RESERVA_NO_TROBADA));
        comprovarPropietari(reserva, dniClient);
        return reserva;
    }

    /**
     * Retorna una reserva pel seu identificador, només si pertany al client
     * indicat (RF53).
     *
     * @param id identificador de la reserva
     * @param dniClient DNI del client que la demana
     * @return la reserva
     * @throws IllegalArgumentException si no existeix o no és del client
     */
    @Transactional(readOnly = true)
    public Reserva obtenirDetallsReserva(Long id, String dniClient) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(RESERVA_NO_TROBADA));
        comprovarPropietari(reserva, dniClient);
        return reserva;
    }

    /**
     * Comprova que la reserva és del client indicat. Si no ho és, llança el
     * mateix error que si no existís, perquè un client no pugui saber si una
     * reserva d'un altre client existeix. S'ha de cridar dins d'una transacció
     * (el client de la reserva és LAZY).
     *
     * @param reserva reserva a comprovar
     * @param dniClient DNI del client que la demana
     * @throws IllegalArgumentException si la reserva no és del client
     */
    private void comprovarPropietari(Reserva reserva, String dniClient) {
        if (!reserva.getClient().getDniUsuari().equals(dniClient)) {
            throw new IllegalArgumentException(RESERVA_NO_TROBADA);
        }
    }

    /**
     * Import que es retorna al client quan anul·la ell mateix (RF55): tot
     * l'import del lloguer si falten més de DIES_ANTELACIO dies per a la data
     * d'inici, i 0 si no. No inclou la fiança. (L'agent fa servir un
     * percentatge sobre import + fiança: vegeu anullarReservaAgent.)
     *
     * @param reserva reserva que s'anul·laria
     * @return import a retornar (l'import total o zero)
     */
    public BigDecimal calcularDevolucio(Reserva reserva) {
        // Calculem els dies d'antelació entre avui i la data d'inici
        long diesAntelacio = ChronoUnit.DAYS.between(LocalDate.now(), reserva.getDataInici());

        // Interpretació: "més de n dies" significa estrictament major (>)
        if (diesAntelacio > DIES_ANTELACIO) {
            return reserva.getImportTotal();
        }

        return BigDecimal.ZERO;
    }

    /**
     * Anul·la una reserva Activa des del perfil del client (RF55): calcula la
     * devolució, desa l'estat Anul_lada i actualitza l'històric de MongoDB.
     *
     * @param id identificador de la reserva
     * @param dniClient DNI del client que l'anul·la
     * @return import que es retorna al client
     * @throws IllegalArgumentException si no existeix o no és del client
     * @throws IllegalStateException si la reserva no està Activa
     */
    @Transactional
    public BigDecimal anullarReservaClient(Long id, String dniClient) {
        Reserva reserva = obtenirDetallsReserva(id, dniClient);

        if (reserva.getEstat() != Enums.EstatReserva.Activa) {
            throw new IllegalStateException("Només es poden anul·lar reserves Actives.");
        }

        BigDecimal devolucio = calcularDevolucio(reserva);

        reserva.setEstat(Enums.EstatReserva.Anul_lada);
        reserva.setDataAnulacio(LocalDateTime.now());
        reserva.setImportRetornat(devolucio);

        reservaRepository.save(reserva);
        historicReservaService.registrarCanvi(reserva);
        log.info("Reserva {} anul·lada pel client (devolució {} €)",
                reserva.getCodiReserva(), devolucio);

        return devolucio;
    }

    // Reserves de l'agent
    /**
     * Anul·la una reserva des del perfil d'agent i calcula la devolució segons
     * el percentatge indicat sobre l'import pagat (lloguer + fiança).
     *
     * @param idReserva identificador de la reserva
     * @param motiu motiu de l'anul·lació
     * @param percentatge percentatge de devolució, entre 0 i 100
     * @return import que es retorna al client
     */
    @Transactional
    public BigDecimal anullarReservaAgent(
            Long idReserva, String motiu, BigDecimal percentatge) {

        Reserva reserva = reservaRepository
                .findWithClientAndVehicleByIdReserva(idReserva)
                .orElseThrow(() -> new IllegalArgumentException(
                "No s'ha trobat la reserva indicada."));

        if (reserva.getEstat() != Enums.EstatReserva.Activa) {
            throw new IllegalStateException(
                    "Només es poden anul·lar reserves Actives.");
        }

        if (motiu == null || motiu.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cal indicar el motiu de l'anul·lació.");
        }

        if (motiu.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "El motiu no pot superar els 500 caràcters.");
        }

        if (percentatge == null
                || percentatge.compareTo(BigDecimal.ZERO) < 0
                || percentatge.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "El percentatge de devolució ha d'estar entre 0 i 100.");
        }

        // L'import pagat inclou el preu de la reserva i la fiança.
        BigDecimal importPagat = reserva.getImportTotal().add(reserva.getFianca());

        BigDecimal devolucio = importPagat
                .multiply(percentatge)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        reserva.setEstat(Enums.EstatReserva.Anul_lada);
        reserva.setDataAnulacio(LocalDateTime.now());
        reserva.setImportRetornat(devolucio);
        reserva.setMotiuAnulacio(motiu.trim());
        reserva.setPercentatgeDevolucio(
                percentatge.setScale(2, RoundingMode.HALF_UP));

        reservaRepository.save(reserva);
        historicReservaService.registrarCanvi(reserva);
        log.info("Reserva {} anul·lada per un agent (devolució {} €, {} %)",
                reserva.getCodiReserva(), devolucio, reserva.getPercentatgeDevolucio());

        return devolucio;
    }

    /**
     * Llistat de reserves per a l'agent amb filtres (RF54). Tots els filtres
     * són opcionals i es poden combinar: null o text buit vol dir "sense
     * filtre". DNI, email, matrícula i codi cerquen per coincidència parcial
     * sense distingir majúscules. La data filtra per data de creació de la
     * reserva (tot el dia, sense mirar l'hora).
     *
     * @param dni DNI del client (o una part)
     * @param email email del client (o una part)
     * @param data dia de creació de la reserva
     * @param matricula matrícula del vehicle (o una part)
     * @param codi codi de reserva (o una part)
     * @param estat estat de la reserva
     * @return reserves que compleixen tots els filtres, de la més recent a la
     * més antiga
     */
    public List<Reserva> llistarReservesAgentFiltrades(
            String dni,
            String email,
            LocalDate data,
            String matricula,
            String codi,
            Enums.EstatReserva estat) {

        // Si un filtre està buit, no s'aplicarà
        dni = (dni == null || dni.isBlank()) ? null : dni.trim();
        email = (email == null || email.isBlank()) ? null : email.trim();
        matricula = (matricula == null || matricula.isBlank()) ? null : matricula.trim();
        codi = (codi == null || codi.isBlank()) ? null : codi.trim();

        // Si s'ha seleccionat una data, busquem totes les reserves
        // creades durant aquell dia, independentment de l'hora.
        LocalDateTime dataInici = data != null ? data.atStartOfDay() : null;
        LocalDateTime dataFi = data != null ? data.plusDays(1).atStartOfDay() : null;

        return reservaRepository.llistarReservesAgentFiltrades(
                dni,
                email,
                matricula,
                codi,
                dataInici,
                dataFi,
                estat
        );
    }

    /**
     * Nombre de reserves en estat Activa (indicador de la pantalla de reserves
     * de l'agent).
     *
     * @return recompte de reserves Actives
     */
    public long comptarReservesActives() {
        return reservaRepository.countByEstat(Enums.EstatReserva.Activa);
    }

    /**
     * Nombre de reserves en estat Lliurada, és a dir, amb el vehicle recollit i
     * encara no retornat (indicador de la pantalla de reserves de l'agent).
     *
     * @return recompte de reserves Lliurades
     */
    public long comptarReservesLliurades() {
        return reservaRepository.countByEstat(Enums.EstatReserva.Lliurada);
    }

    /**
     * Reserva amb el client i el vehicle carregats, per al detall de l'agent
     * (RF53). No comprova el propietari: l'agent pot veure qualsevol reserva.
     *
     * @param idReserva identificador de la reserva
     * @return la reserva
     * @throws IllegalArgumentException si no existeix
     */
    @Transactional(readOnly = true)
    public Reserva obtenirDetallsReservaAgent(Long idReserva) {
        return reservaRepository.findWithClientAndVehicleByIdReserva(idReserva)
                .orElseThrow(() -> new IllegalArgumentException(
                "No s'ha trobat la reserva indicada."));
    }

    // ---------- RF49: lliurar vehicle ----------
    /**
     * Format de data per als missatges que es mostren a l'usuari.
     */
    private static final DateTimeFormatter FORMAT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Format de data i hora per als missatges que es mostren a l'usuari.
     */
    private static final DateTimeFormatter FORMAT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Comprova si es pot lliurar el vehicle d'una reserva ara mateix. Serveix
     * tant per mostrar el motiu a la pantalla (i amagar el formulari) com per
     * validar l'operació, així la regla només està escrita en un lloc.
     *
     * @param reserva reserva amb el vehicle carregat
     * @return null si es pot lliurar; si no, el motiu en català
     */
    public String motiuNoLliurable(Reserva reserva) {
        if (reserva.getEstat() != Enums.EstatReserva.Activa) {
            return "Només es pot lliurar el vehicle d'una reserva Activa.";
        }

        LocalDate avui = LocalDate.now();
        if (avui.isBefore(reserva.getDataInici())) {
            return "El vehicle es podrà lliurar a partir del "
                    + reserva.getDataInici().format(FORMAT_DATA) + ".";
        }
        if (avui.isAfter(reserva.getDataFi())) {
            return "La data de devolució prevista d'aquesta reserva ("
                    + reserva.getDataFi().format(FORMAT_DATA) + ") ja ha passat.";
        }

        Enums.EstatVehicle estatVehicle = reserva.getVehicle().getEstat();
        if (estatVehicle == Enums.EstatVehicle.Inactiu) {
            return "El vehicle està inactiu i no es pot lliurar.";
        }
        if (estatVehicle == Enums.EstatVehicle.Llogat) {
            return "El vehicle encara no s'ha retornat de la reserva anterior.";
        }
        return null;
    }

    /**
     * Lliura el vehicle d'una reserva Activa al client: desa la data, l'hora i
     * la descripció de l'estat, passa la reserva a Lliurada i el vehicle a
     * Llogat, ho registra a l'històric i desa les fotos opcionals a MongoDB.
     * Primer es valida tot (inclosos els arxius) i després es modifica res, de
     * manera que un error no deixa l'operació a mitges.
     *
     * @param idReserva id de la reserva
     * @param dataHora data i hora en què es lliura (no pot ser futura i ha
     * d'estar dins del període de la reserva)
     * @param descripcio estat del vehicle en lliurar-lo (obligatori, màx. 500)
     * @param fotos fotos opcionals (JPG o PNG, màx. 5 MB cadascuna)
     * @throws IllegalArgumentException si les dades o les fotos no són vàlides
     * @throws IllegalStateException si la reserva no es pot lliurar o MongoDB
     * falla
     */
    @Transactional
    public void lliurarVehicle(Long idReserva, LocalDateTime dataHora,
            String descripcio, List<MultipartFile> fotos) {

        Reserva reserva = reservaRepository
                .findWithClientAndVehicleByIdReserva(idReserva)
                .orElseThrow(() -> new IllegalArgumentException(
                "No s'ha trobat la reserva indicada."));

        String motiu = motiuNoLliurable(reserva);
        if (motiu != null) {
            throw new IllegalStateException(motiu);
        }

        if (dataHora == null) {
            throw new IllegalArgumentException(
                    "Cal indicar la data i l'hora de lliurament.");
        }
        if (dataHora.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La data i l'hora de lliurament no poden ser futures.");
        }
        if (dataHora.toLocalDate().isBefore(reserva.getDataInici())
                || dataHora.toLocalDate().isAfter(reserva.getDataFi())) {
            throw new IllegalArgumentException(
                    "La data de lliurament ha d'estar dins del període de la reserva ("
                    + reserva.getDataInici().format(FORMAT_DATA) + " - "
                    + reserva.getDataFi().format(FORMAT_DATA) + ").");
        }
        if (descripcio == null || descripcio.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cal descriure l'estat en què es lliura el vehicle.");
        }
        if (descripcio.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "La descripció no pot superar els 500 caràcters.");
        }

        // Valida les fotos abans de tocar la base de dades
        List<ReservaArxiu> arxius = reservaArxiuService.prepararFotos(
                idReserva, ReservaArxiu.Moment.LLIURAMENT, fotos);

        Vehicle vehicle = reserva.getVehicle();
        reserva.setDataLliurament(dataHora);
        reserva.setDescripcioLliurament(descripcio.trim());
        reserva.setEstat(Enums.EstatReserva.Lliurada);
        vehicle.setEstat(Enums.EstatVehicle.Llogat);

        vehicleRepository.save(vehicle);
        reservaRepository.save(reserva);
        historicReservaService.registrarCanvi(reserva);
        reservaArxiuService.guardar(arxius);

        log.info("Vehicle {} lliurat a la reserva {} ({} fotos)",
                vehicle.getMatricula(), reserva.getCodiReserva(), arxius.size());
    }

    // ---------- RF48: retornar vehicle ----------
    /**
     * Moment límit per retornar el vehicle: la data de fi de la reserva a la
     * mateixa hora a la qual es va lliurar (el lloguer es cobra per hores, dies
     * x 24, així que el retorn s'espera a la mateixa hora del lliurament).
     *
     * @param reserva reserva Lliurada
     * @return data i hora límit de devolució
     */
    public LocalDateTime calcularLimitRetorn(Reserva reserva) {
        if (reserva.getDataLliurament() == null) {
            return reserva.getDataFi().atStartOfDay();
        }
        return reserva.getDataFi().atTime(reserva.getDataLliurament().toLocalTime());
    }

    /**
     * Hores de retard, arrodonides cap amunt: qualsevol fracció d'hora compta
     * com una hora sencera (1 h 10 min de retard = 2 hores). Si es retorna a
     * temps, 0.
     *
     * @param reserva reserva Lliurada
     * @param dataRetorn moment real de la devolució
     * @return hores de retard (0 si no n'hi ha)
     */
    public int calcularHoresRetard(Reserva reserva, LocalDateTime dataRetorn) {
        long minuts = Duration.between(calcularLimitRetorn(reserva), dataRetorn).toMinutes();
        if (minuts <= 0) {
            return 0;
        }
        return (int) ((minuts + 59) / 60);
    }

    /**
     * Descompte de la fiança pel retard: hores de retard per l'import d'una
     * hora de reserva (preu per hora del vehicle). Mai pot superar la fiança.
     *
     * @param vehicle vehicle de la reserva
     * @param horesRetard hores de retard
     * @param fianca fiança de la reserva
     * @return import a descomptar, amb 2 decimals
     */
    public BigDecimal calcularDescompteRetard(Vehicle vehicle, int horesRetard, BigDecimal fianca) {
        if (horesRetard <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal descompte = BigDecimal.valueOf(vehicle.getPreu_hores())
                .multiply(BigDecimal.valueOf(horesRetard))
                .setScale(2, RoundingMode.HALF_UP);
        return descompte.min(fianca);
    }

    /**
     * Registra la devolució d'un vehicle d'una reserva Lliurada. L'agent indica
     * la data i l'hora reals de la devolució; per cada hora de retard es
     * descompta de la fiança l'import d'una hora de reserva. Si hi ha
     * desperfectes imputables al client la fiança es reté sencera; si no ho són
     * (o no n'hi ha) es retorna, descomptant el retard. El resultat queda a
     * MySQL (import retornat de la reserva), a l'històric de MongoDB i, si n'hi
     * ha, a les fotos del retorn. En finalitzar, el vehicle torna a estar
     * Disponible.
     *
     * @param idReserva id de la reserva
     * @param dataHoraRetorn data i hora de devolució (no futura i posterior al
     * lliurament)
     * @param comentari comentari opcional (màx. 500)
     * @param incidencia desperfectes o incidència (obligatòria si és imputable)
     * @param desperfecteImputable true si els desperfectes són imputables al
     * client
     * @param fotos fotos opcionals del retorn
     * @return la reserva finalitzada, amb la decisió sobre la fiança
     * @throws IllegalArgumentException si les dades no són vàlides
     * @throws IllegalStateException si la reserva no està Lliurada o MongoDB
     * falla
     */
    @Transactional
    public Reserva retornarVehicle(Long idReserva, LocalDateTime dataHoraRetorn,
            String comentari, String incidencia, boolean desperfecteImputable,
            List<MultipartFile> fotos) {

        Reserva reserva = reservaRepository
                .findWithClientAndVehicleByIdReserva(idReserva)
                .orElseThrow(() -> new IllegalArgumentException(
                "No s'ha trobat la reserva indicada."));

        if (reserva.getEstat() != Enums.EstatReserva.Lliurada) {
            throw new IllegalStateException(
                    "Només es pot retornar un vehicle d'una reserva Lliurada.");
        }

        if (dataHoraRetorn == null) {
            throw new IllegalArgumentException(
                    "Cal indicar la data i l'hora de devolució.");
        }
        if (dataHoraRetorn.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La data i l'hora de devolució no poden ser futures.");
        }
        if (reserva.getDataLliurament() != null
                && dataHoraRetorn.isBefore(reserva.getDataLliurament())) {
            throw new IllegalArgumentException(
                    "La devolució no pot ser anterior al lliurament ("
                    + reserva.getDataLliurament().format(FORMAT_DATA_HORA) + ").");
        }

        if (comentari != null && comentari.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "El comentari no pot superar els 500 caràcters.");
        }
        if (incidencia != null && incidencia.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "La incidència no pot superar els 500 caràcters.");
        }
        boolean hiHaIncidencia = incidencia != null && !incidencia.isBlank();
        if (desperfecteImputable && !hiHaIncidencia) {
            throw new IllegalArgumentException(
                    "Per marcar un desperfecte com a imputable al client cal descriure'l a la incidència.");
        }

        // Primer es validen totes les fotos; si alguna és incorrecta no es canvia res.
        List<ReservaArxiu> arxius = reservaArxiuService.prepararFotos(
                idReserva, ReservaArxiu.Moment.RETORN, fotos);

        Vehicle vehicle = reserva.getVehicle();
        int horesRetard = calcularHoresRetard(reserva, dataHoraRetorn);

        // Decisió sobre la fiança
        BigDecimal descompte;
        BigDecimal fiancaRetornada;
        if (desperfecteImputable) {
            // La fiança es reté sencera; el retard ja no canvia res.
            descompte = BigDecimal.ZERO.setScale(2);
            fiancaRetornada = BigDecimal.ZERO.setScale(2);
        } else {
            descompte = calcularDescompteRetard(vehicle, horesRetard, reserva.getFianca());
            fiancaRetornada = reserva.getFianca().subtract(descompte).setScale(2, RoundingMode.HALF_UP);
        }

        reserva.setDataRetorn(dataHoraRetorn);
        reserva.setComentariRetorn(comentari == null || comentari.isBlank()
                ? null : comentari.trim());
        reserva.setIncidenciaRetorn(hiHaIncidencia ? incidencia.trim() : null);
        reserva.setDesperfecteImputable(desperfecteImputable);
        reserva.setHoresRetard(horesRetard);
        reserva.setDescompteRetard(descompte);
        reserva.setImportRetornat(fiancaRetornada);
        reserva.setEstat(Enums.EstatReserva.Finalitzada);
        vehicle.setEstat(Enums.EstatVehicle.Disponible);

        vehicleRepository.save(vehicle);
        reservaRepository.save(reserva);
        historicReservaService.registrarCanvi(reserva);
        reservaArxiuService.guardar(arxius);

        log.info("Vehicle {} retornat per la reserva {} ({} h de retard, imputable: {}, fiança retornada {} €, {} fotos)",
                vehicle.getMatricula(), reserva.getCodiReserva(), horesRetard,
                desperfecteImputable, fiancaRetornada, arxius.size());
        return reserva;
    }

    // ---------- Reserves del client (pantalla d'inici) ----------
    /**
     * Pròxima reserva Activa o Lliurada d'un client (la de data d'inici més
     * propera), si en té.
     *
     * @param dniClient DNI del client
     * @return la reserva amb el vehicle carregat, o null si no en té cap
     */
    @Transactional(readOnly = true)
    public Reserva obtenirProperaReserva(String dniClient) {
        List<Reserva> properes = llistarProperes(dniClient, 0).getContent();
        return properes.isEmpty() ? null : properes.get(0);
    }

    /**
     * Nombre de lloguers completats (reserves Finalitzades) d'un client.
     *
     * @param dniClient DNI del client
     * @return recompte de reserves Finalitzades
     */
    public long comptarLloguersCompletats(String dniClient) {
        return reservaRepository.countByClient_DniUsuariAndEstat(
                dniClient, Enums.EstatReserva.Finalitzada);
    }

}
