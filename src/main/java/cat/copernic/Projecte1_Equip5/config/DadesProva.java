package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleArxiuRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import cat.copernic.Projecte1_Equip5.service.HistoricReservaService;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carrega dades de prova en arrencar l'aplicació: clients i un agent (amb la
 * seva documentació a MongoDB), vehicles (amb foto) i reserves en tots els
 * estats (amb el seu històric a MongoDB). Cada bloc només es carrega si encara
 * no hi és, de manera que es pot arrencar l'aplicació diverses vegades. Es pot
 * desactivar amb {@code app.dades-prova=false}. Si MongoDB no està disponible,
 * es carrega només la part de MySQL i s'avisa pel log.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.dades-prova", havingValue = "true", matchIfMissing = true)
public class DadesProva implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DadesProva.class);

    /**
     * Contrasenya de tots els usuaris de prova creats per aquesta classe.
     */
    private static final String CONTRASENYA = "Prova1234";
    private static final String DNI_PRIMER_CLIENT = "44556677L";

    private final UsuariRepository usuariRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final ReservaRepository reservaRepository;
    private final DocumentacioRepository documentacioRepository;
    private final VehicleArxiuRepository vehicleArxiuRepository;
    private final ReservaService reservaService;
    private final HistoricReservaService historicReservaService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor amb injecció de dependències.
     *
     * @param usuariRepository repositori d'usuaris
     * @param clientRepository repositori de clients
     * @param vehicleRepository repositori de vehicles
     * @param reservaRepository repositori de reserves
     * @param documentacioRepository repositori de documentació (MongoDB)
     * @param vehicleArxiuRepository repositori de fitxers de vehicles (MongoDB)
     * @param reservaService servei de reserves (càlcul d'imports i codis)
     * @param historicReservaService servei de l'històric de reserves (MongoDB)
     * @param passwordEncoder codificador de contrasenyes
     */
    public DadesProva(UsuariRepository usuariRepository, ClientRepository clientRepository,
            VehicleRepository vehicleRepository, ReservaRepository reservaRepository,
            DocumentacioRepository documentacioRepository, VehicleArxiuRepository vehicleArxiuRepository,
            ReservaService reservaService, HistoricReservaService historicReservaService,
            PasswordEncoder passwordEncoder) {
        this.usuariRepository = usuariRepository;
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.reservaRepository = reservaRepository;
        this.documentacioRepository = documentacioRepository;
        this.vehicleArxiuRepository = vehicleArxiuRepository;
        this.reservaService = reservaService;
        this.historicReservaService = historicReservaService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Carrega clients, vehicles i reserves de prova, en aquest ordre. Un error
     * es registra al log però no impedeix que l'aplicació arrenqui.
     *
     * @param args arguments de línia d'ordres (no s'utilitzen)
     */
    @Override
    public void run(String... args) {
        try {
            byte[] imatgeDocument = llegirImatge("static/img/logo.jpg");
            crearClients(imatgeDocument);
            crearDocumentacioUsuarisInicials(imatgeDocument);
            crearVehicles(llegirImatge("static/img/cotxe.jpg"));
            crearReserves();
        } catch (RuntimeException e) {
            LOG.error("No s'han pogut carregar les dades de prova", e);
        }
    }

    // ------------------------------------------------------------------ clients
    private void crearClients(byte[] imatge) {
        if (usuariRepository.existsById(DNI_PRIMER_CLIENT)) {
            return;
        }
        Agent admin = usuariRepository.findByEmail("admin@empresa.com")
                .filter(Agent.class::isInstance).map(Agent.class::cast).orElse(null);
        String hash = passwordEncoder.encode(CONTRASENYA);
        Enums.EstatValidacio validat = Enums.EstatValidacio.Validat;
        Enums.EstatValidacio pendent = Enums.EstatValidacio.Pendent;

        List<Client> clients = new ArrayList<>(List.of(
                client("44556677L", "anna.puig@exemple.cat", "Anna", "Puig Ferrer", "Espanyola",
                        "Carrer de Balmes 45, 08007 Barcelona", "+34 612345678", validat, true, hash, admin),
                client("45678912S", "marc.vidal@exemple.cat", "Marc", "Vidal Soler", "Espanyola",
                        "Carrer Major 12, 08221 Terrassa", "+34 623456789", validat, false, hash, admin),
                client("46789123Q", "sophie.martin@exemple.fr", "Sophie", "Martin Dubois", "Francesa",
                        "Rue de Rivoli 20, 75001 Paris", "+33 612345678", validat, false, hash, admin),
                client("47891234J", "john.smith@exemple.co.uk", "John", "Smith Brown", "Britànica",
                        "Passeig de Gràcia 10, 08007 Barcelona", "+44 7123456789", validat, false, hash, admin),
                client("48912345Q", "laia.roca@exemple.cat", "Laia", "Roca Mas", "Espanyola",
                        "Carrer del Mar 3, 17001 Girona", "+34 634567890", pendent, false, hash, null),
                client("49123456X", "pere.sala@exemple.cat", "Pere", "Sala Vila", "Espanyola",
                        "Avinguda Diagonal 200, 08018 Barcelona", "+34 645678901", pendent, false, hash, null),
                client("50123456Q", "carla.mas@exemple.cat", "Carla", "Mas Rius", "Espanyola",
                        "Carrer Nou 8, 08202 Sabadell", "+34 656789012",
                        Enums.EstatValidacio.Rebutjat, false, hash, admin)));

        // Client donat de baixa (apareix a la pantalla de baixes)
        Client baixa = client("51234567C", "oriol.pons@exemple.cat", "Oriol", "Pons Gil", "Espanyola",
                "Carrer Ample 5, 43001 Tarragona", "+34 667890123", validat, false, hash, admin);
        baixa.setActiu(false);
        clients.add(baixa);

        // Segon agent
        clients.add(new Agent("52345678R", "nuria.ribas@empresa.com", hash, LocalDateTime.now(),
                "Núria", "Ribas Costa", LocalDate.now().plusYears(5), "B", LocalDate.now().plusYears(5),
                "Carrer Agent 4, 08001 Barcelona", "Espanyola", validat, admin, false, null,
                Enums.Rol.Agent));
        
        // Tercer agent
        clients.add(new Agent("53456789M", "jordi.camps@empresa.com", hash, LocalDateTime.now(),
                "Jordi", "Camps Pons", LocalDate.now().plusYears(5), "B", LocalDate.now().plusYears(5),
                "Carrer Agent 6, 08001 Barcelona", "Espanyola", validat, admin, false, null,
                Enums.Rol.Agent));

        usuariRepository.saveAll(clients);

        ambMongo("la documentació dels clients", () -> {
            if (imatge.length == 0) {
                return;
            }
            for (Client c : clients) {
                Documentacio d = new Documentacio(c.getDniUsuari(), "identitat.jpg", "image/jpeg",
                        imatge, "carnet.jpg", "image/jpeg", imatge);
                d.setEstat(estatDocumentacio(c.getEstatValidacio()));
                documentacioRepository.save(d);
            }
        });
        LOG.info("Carregats {} usuaris de prova (contrasenya: {})", clients.size(), CONTRASENYA);
    }
    
        /** Documentació (DNI i carnet) dels usuaris que crea initDatabase, si encara no en tenen. */
    private void crearDocumentacioUsuarisInicials(byte[] imatge) {
        ambMongo("la documentació dels usuaris inicials", () -> {
            if (imatge.length == 0) {
                return;
            }
            for (String dni : List.of("11111111A", "22222222B", "33333333C")) {
                if (usuariRepository.existsById(dni) && documentacioRepository.findByDniUsuari(dni).isEmpty()) {
                    Documentacio d = new Documentacio(dni, "identitat.jpg", "image/jpeg", imatge,
                            "carnet.jpg", "image/jpeg", imatge);
                    d.setEstat(Documentacio.Estat.APROVADA);
                    documentacioRepository.save(d);
                }
            }
        });
    }

    private static Client client(String dni, String email, String nom, String cognoms,
            String nacionalitat, String adreca, String telefon, Enums.EstatValidacio estat,
            boolean premium, String hash, Agent validador) {
        Client c = new Client(dni, email, hash, LocalDateTime.now(), nom, cognoms,
                LocalDate.now().plusYears(3), "B", LocalDate.now().plusYears(2), adreca, nacionalitat,
                estat, estat == Enums.EstatValidacio.Pendent ? null : validador, premium, null);
        c.setTelefon(telefon);
        c.setDataNaixement(LocalDate.now().minusYears(30));
        return c;
    }

    private static Documentacio.Estat estatDocumentacio(Enums.EstatValidacio estat) {
        return switch (estat) {
            case Validat ->
                Documentacio.Estat.APROVADA;
            case Rebutjat ->
                Documentacio.Estat.REBUTJADA;
            case Pendent ->
                Documentacio.Estat.PENDENT;
        };
    }

    // ----------------------------------------------------------------- vehicles
        private void crearVehicles(byte[] foto) {
        if (vehicleRepository.count() > 0) {
            return;
        }
        Vehicle inactiu = vehicle("2468HJK", "Peugeot", Enums.CategoriaVehicle.Turisme, "208", 2019,
                7.0, Enums.Combustible.Gasolina, 100, "Vermell", 5, Enums.Transmissio.Manual,
                200, 20, "200.00", "Pendent de passar l'ITV.");
        inactiu.setEstat(Enums.EstatVehicle.Inactiu);

        List<Vehicle> vehicles = List.of(
                vehicle("1234BCD", "Seat", Enums.CategoriaVehicle.Turisme, "Ibiza", 2022,
                        8.5, Enums.Combustible.Gasolina, 95, "Blanc", 5, Enums.Transmissio.Manual,
                        300, 30, "250.00", "Vehicle de prova."),
                vehicle("3456JKL", "Toyota", Enums.CategoriaVehicle.SUV, "RAV4", 2023,
                        14.0, Enums.Combustible.Híbrid, 222, "Negre", 5, Enums.Transmissio.Automàtic,
                        null, 30, "500.00", null),
                vehicle("4567MNP", "Tesla", Enums.CategoriaVehicle.Elèctric, "Model 3", 2024,
                        18.0, Enums.Combustible.Elèctric, 283, "Blanc", 5, Enums.Transmissio.Automàtic,
                        null, 15, "600.00", "Càrrega al garatge de l'oficina."),
                vehicle("5678RST", "Ford", Enums.CategoriaVehicle.Furgoneta, "Transit", 2020,
                        12.0, Enums.Combustible.Dièsel, 130, "Blanc", 3, Enums.Transmissio.Manual,
                        400, 30, "400.00", null),
                vehicle("6789VWX", "Porsche", Enums.CategoriaVehicle.Esportiu, "911", 2022,
                        45.0, Enums.Combustible.Gasolina, 385, "Vermell", 2, Enums.Transmissio.Automàtic,
                        150, 7, "2000.00", "Només clients premium."),
                inactiu);
        vehicleRepository.saveAll(vehicles);

        ambMongo("les fotos dels vehicles", () -> {
            if (foto.length == 0) {
                return;
            }
            for (Vehicle v : vehicles) {
                VehicleArxiu arxiu = new VehicleArxiu(v.getMatricula(), VehicleArxiu.Tipus.FOTO,
                        "principal.jpg", "image/jpeg", foto.length, foto);
                arxiu.setPrincipal(true);
                vehicleArxiuRepository.save(arxiu);
            }
        });
        LOG.info("Carregats {} vehicles de prova", vehicles.size());
    }

    private static Vehicle vehicle(String matricula, String marca, Enums.CategoriaVehicle categoria,
            String model, int anyMatriculacio, double preuHores, Enums.Combustible combustible,
            int potencia, String color, int places, Enums.Transmissio transmissio,
            Integer limitKm, int maxDies, String fianca, String comentaris) {
        return new Vehicle(matricula, marca, categoria, model, anyMatriculacio, preuHores,
                LocalDate.now().minusDays(30), combustible, potencia, color, places, transmissio,
                limitKm, maxDies, new BigDecimal(fianca), comentaris);
    }

    // ------------------------------------------------------------------ reserves
        private void crearReserves() {
        if (reservaRepository.count() > 0) {
            return;
        }
        LocalDate avui = LocalDate.now();
        List<Reserva> reserves = new ArrayList<>();

        // Seat Ibiza: finalitzada amb retard + activa que es pot lliurar avui
        desar(reserves, finalitzada(nova("45678912S", "1234BCD", avui.minusDays(20), avui.minusDays(17),
                Enums.EstatReserva.Finalitzada, 25), 3, null, false));
        desar(reserves, nova("46789123Q", "1234BCD", avui, avui.plusDays(3),
                Enums.EstatReserva.Activa, 5));

        // Toyota RAV4: finalitzada, anul·lada pel client i activa
        desar(reserves, finalitzada(nova("44556677L", "3456JKL", avui.minusDays(30), avui.minusDays(27),
                Enums.EstatReserva.Finalitzada, 35), 0, null, false));
        desar(reserves, anulada(nova("47891234J", "3456JKL", avui.plusDays(20), avui.plusDays(23),
                Enums.EstatReserva.Anul_lada, 3), null, null));
        desar(reserves, nova("46789123Q", "3456JKL", avui.plusDays(10), avui.plusDays(14),
                Enums.EstatReserva.Activa, 2));

        // Tesla Model 3: finalitzada + lliurada (vehicle llogat ara mateix)
        desar(reserves, finalitzada(nova("47891234J", "4567MNP", avui.minusDays(25), avui.minusDays(22),
                Enums.EstatReserva.Finalitzada, 30), 0, null, false));
        desar(reserves, lliurada(nova("45678912S", "4567MNP", avui.minusDays(1), avui.plusDays(2),
                Enums.EstatReserva.Lliurada, 6)));

        // Ford Transit: finalitzada amb desperfecte + activa d'una clienta premium
        desar(reserves, finalitzada(nova("46789123Q", "5678RST", avui.minusDays(15), avui.minusDays(12),
                Enums.EstatReserva.Finalitzada, 20), 0, "Esgarrapada al para-xocs posterior.", true));
        desar(reserves, nova("44556677L", "5678RST", avui.plusDays(5), avui.plusDays(7),
                Enums.EstatReserva.Activa, 2));

        // Porsche 911: activa + anul·lada per l'agent (50 %)
        desar(reserves, nova("47891234J", "6789VWX", avui.plusDays(15), avui.plusDays(17),
                Enums.EstatReserva.Activa, 1));
        desar(reserves, anulada(nova("44556677L", "6789VWX", avui.plusDays(30), avui.plusDays(32),
                Enums.EstatReserva.Anul_lada, 4), "El client no pot viatjar.", new BigDecimal("50")));

        // Peugeot 208 (inactiu): només finalitzada i anul·lada, mai activa
        desar(reserves, finalitzada(nova("45678912S", "2468HJK", avui.minusDays(10), avui.minusDays(8),
                Enums.EstatReserva.Finalitzada, 14), 0, null, false));
        desar(reserves, anulada(nova("46789123Q", "2468HJK", avui.plusDays(25), avui.plusDays(27),
                Enums.EstatReserva.Anul_lada, 2), null, null));

        ambMongo("l'històric de reserves", () -> reserves.forEach(historicReservaService::registrarCanvi));
        LOG.info("Carregades {} reserves de prova", reserves.size());
    }

    /**
     * Crea (sense desar) una reserva amb import, fiança i codi calculats pel
     * servei.
     */
    private Reserva nova(String dni, String matricula, LocalDate inici, LocalDate fi,
            Enums.EstatReserva estat, int creadaFaDies) {
        Client client = clientRepository.findById(dni).orElseThrow(
                () -> new IllegalStateException("Falta el client de prova " + dni));
        Vehicle vehicle = vehicleRepository.findById(matricula).orElseThrow(
                () -> new IllegalStateException("Falta el vehicle de prova " + matricula));
        Reserva r = new Reserva(reservaService.generarCodiReserva(), vehicle, client, inici, fi,
                reservaService.calcularImportTotal(vehicle, inici, fi),
                reservaService.calcularFianca(vehicle, client), estat);
        r.setDataCreacio(LocalDateTime.now().minusDays(creadaFaDies));
        return r;
    }

    private Reserva finalitzada(Reserva r, int horesRetard, String incidencia, boolean desperfecte) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        BigDecimal descompte = desperfecte ? zero
                : reservaService.calcularDescompteRetard(r.getVehicle(), horesRetard, r.getFianca());
        r.setDataLliurament(r.getDataInici().atTime(10, 0));
        r.setDescripcioLliurament("Vehicle en bon estat.");
        r.setDataRetorn(r.getDataFi().atTime(10, 0).plusHours(horesRetard));
        r.setComentariRetorn("Retorn correcte.");
        r.setIncidenciaRetorn(incidencia);
        r.setDesperfecteImputable(desperfecte);
        r.setHoresRetard(horesRetard);
        r.setDescompteRetard(descompte);
        r.setImportRetornat(desperfecte ? zero
                : r.getFianca().subtract(descompte).setScale(2, RoundingMode.HALF_UP));
        return r;
    }

    /**
     * Anul·lació del client si el percentatge és null; de l'agent si no ho és.
     */
    private Reserva anulada(Reserva r, String motiu, BigDecimal percentatge) {
        r.setDataAnulacio(LocalDateTime.now().minusHours(6));
        if (percentatge == null) {
            r.setImportRetornat(r.getImportTotal());
        } else {
            r.setMotiuAnulacio(motiu);
            r.setPercentatgeDevolucio(percentatge.setScale(2, RoundingMode.HALF_UP));
            r.setImportRetornat(r.getImportTotal().add(r.getFianca()).multiply(percentatge)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        return r;
    }

    private Reserva lliurada(Reserva r) {
        r.setDataLliurament(r.getDataInici().atTime(10, 0));
        r.setDescripcioLliurament("Vehicle en bon estat, bateria al 80 %.");
        Vehicle v = r.getVehicle();
        v.setEstat(Enums.EstatVehicle.Llogat);
        vehicleRepository.save(v);
        return r;
    }

    private void desar(List<Reserva> llista, Reserva r) {
        llista.add(reservaRepository.save(r));
    }

    // ------------------------------------------------------------------- suport
    /**
     * Executa una càrrega a MongoDB; si falla, només s'avisa pel log.
     */
    private void ambMongo(String que, Runnable accio) {
        try {
            accio.run();
        } catch (RuntimeException e) {
            LOG.warn("MongoDB no disponible: no s'han carregat {}", que, e);
        }
    }

    private static byte[] llegirImatge(String ruta) {
        try {
            return new ClassPathResource(ruta).getContentAsByteArray();
        } catch (IOException e) {
            LOG.warn("No s'ha pogut llegir la imatge {}", ruta, e);
            return new byte[0];
        }
    }
}
