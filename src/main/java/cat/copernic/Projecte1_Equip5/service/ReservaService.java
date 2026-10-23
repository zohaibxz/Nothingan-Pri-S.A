package cat.copernic.Projecte1_Equip5.service;

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
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lògica de negoci de les reserves (RF50, RF52). Les entrades incorrectes es
 * notifiquen amb IllegalArgumentException i els conflictes de disponibilitat
 * amb IllegalStateException; els missatges són en català perquè el controlador
 * els mostra tal qual a l'usuari.
 */
@Service
public class ReservaService {

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
     * POSSIBLEMENT INCORRECTE: fiança provisional fins que Vehicle tingui el
     * camp de fiança estàndard.
     */
    private static final BigDecimal FIANCA_BASE = new BigDecimal("150.00");

    private final ReservaRepository reservaRepository;
    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    public ReservaService(ReservaRepository reservaRepository,
            VehicleRepository vehicleRepository,
            ClientRepository clientRepository) {
        this.reservaRepository = reservaRepository;
        this.vehicleRepository = vehicleRepository;
        this.clientRepository = clientRepository;
    }

    // ---------- RF52: llistat ----------
    /**
     * Reserves actives o lliurades d'un client, ordenades per data d'inici.
     *
     * @param dniClient DNI del client
     * @return llista de properes reserves
     */
    @Transactional(readOnly = true)
    public List<Reserva> llistarProperes(String dniClient) {
        return reservaRepository.findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(dniClient, ESTATS_PROPERES);
    }

    /**
     * Reserves d'un client finalitzades o anul·lades, de la més recent a la més
     * antiga.
     *
     * @param dniClient DNI del client
     * @return llista de l'historial
     */
    @Transactional(readOnly = true)
    public List<Reserva> llistarHistorial(String dniClient) {
        return reservaRepository.findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(dniClient, ESTATS_PROPERES);
    }

    // ---------- RF50: alta de reserva ----------
    /**
     * Valida el període: les dues dates són obligatòries, l'inici no pot ser
     * anterior a avui i el mínim és 1 dia (el dia de devolució queda lliure).
     *
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

    
    public long calcularDies(LocalDate inici, LocalDate fi) {
        return ChronoUnit.DAYS.between(inici, fi);
    }

    
    public BigDecimal calcularImportTotal(Vehicle vehicle, LocalDate inici, LocalDate fi) {
        return BigDecimal.valueOf(vehicle.getPreu_hores())
                .multiply(BigDecimal.valueOf(calcularDies(inici, fi)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fiança del client: la base, amb descompte si és premium. POSSIBLEMENT
     * INCORRECTE: interpreto descomptePremium com a percentatge (25 = 25 %).
     */
    public BigDecimal calcularFianca(Client client) {
        BigDecimal fianca = FIANCA_BASE;
        if (Boolean.TRUE.equals(client.getPremium()) && client.getDescomptePremium() != null) {
            BigDecimal factor = BigDecimal.valueOf(100)
                    .subtract(client.getDescomptePremium())
                    .divide(BigDecimal.valueOf(100));
            fianca = fianca.multiply(factor);
        }
        return fianca.setScale(2, RoundingMode.HALF_UP);
    }

    
    @Transactional(readOnly = true)
    public boolean estaDisponible(Vehicle vehicle, LocalDate inici, LocalDate fi) {
        if (vehicle.getEstat() == Enums.EstatVehicle.Inactiu) {
            return false;
        }
        return reservaRepository.comptarSolapaments(
                vehicle.getMatricula(), inici, fi, ESTATS_QUE_BLOQUEJEN) == 0;
    }

    
    @Transactional(readOnly = true)
    public List<Vehicle> vehiclesDisponibles(LocalDate inici, LocalDate fi, Enums.CategoriaVehicle categoria) {
        validarDates(inici, fi);
        return vehicleRepository.findAll().stream()
                .filter(v -> categoria == null || v.getCategoria() == categoria)
                .filter(v -> estaDisponible(v, inici, fi))
                .toList();
    }
    
   
    @Transactional(readOnly = true)
    public Vehicle obtenirVehicle(String matricula) {
        return vehicleRepository.findById(matricula)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat el vehicle indicat."));
    }

    
    @Transactional(readOnly = true)
    public Client obtenirClient(String dniClient) {
        return clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat el client."));
    }

    
    public String generarCodiReserva() {
        return String.format("EX-%d-%04d", LocalDate.now().getYear(), reservaRepository.count() + 1);
    }

    
    @Transactional
    public Reserva crearReserva(String dniClient, String matricula, LocalDate inici, LocalDate fi) {
        validarDates(inici, fi);
        Client client = obtenirClient(dniClient);
        Vehicle vehicle = obtenirVehicle(matricula);

        if (!estaDisponible(vehicle, inici, fi)) {
            throw new IllegalStateException("El vehicle ja no està disponible per a les dates triades.");
        }

        Reserva reserva = new Reserva(generarCodiReserva(), vehicle, client, inici, fi,
                calcularImportTotal(vehicle, inici, fi), calcularFianca(client),
                Enums.EstatReserva.Activa);
        return reservaRepository.save(reserva);
    }

    
    @Transactional(readOnly = true)
    public Reserva obtenirReservaDelClient(String codi, String dniClient) {
        Reserva reserva = reservaRepository.findByCodiReserva(codi)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat la reserva."));
        if (!reserva.getClient().getDniUsuari().equals(dniClient)) {
            throw new IllegalArgumentException("No s'ha trobat la reserva.");
        }
        return reserva;
    }
}
