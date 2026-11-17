package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import cat.copernic.Projecte1_Equip5.repository.AgentRepository;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador del tauler d'inici (/dashboard). Segons el rol de l'usuari
 * autenticat mostra el tauler de l'administrador, el de l'agent (amb mètriques
 * reals de lliuraments, devolucions, flota i alertes) o la pantalla d'inici del
 * client.
 */
@Controller
public class DashBoardController {

    private final UsuariRepository usuariRepository;
    private final ClientService clientService;
    private final ReservaService reservaService;
    private final ReservaRepository reservaRepository;
    private final VehicleLogic vehicleLogic;
    private final AgentRepository agentRepository;

    /**
     * Constructor amb injecció de dependències.
     *
     * @param usuariRepository repositori d'usuaris
     * @param clientService servei de clients
     * @param reservaService servei de reserves (propera reserva i lloguers del
     * client)
     * @param reservaRepository repositori de reserves
     * @param vehicleLogic lògica de vehicles (estat i disponibilitat)
     * @param agentRepository repositori d'agents (agents actius del tauler de
     * l'administrador)
     */
    public DashBoardController(UsuariRepository usuariRepository,
            ClientService clientService,
            ReservaService reservaService,
            ReservaRepository reservaRepository,
            VehicleLogic vehicleLogic,
            AgentRepository agentRepository) {
        this.usuariRepository = usuariRepository;
        this.clientService = clientService;
        this.reservaService = reservaService;
        this.reservaRepository = reservaRepository;
        this.vehicleLogic = vehicleLogic;
        this.agentRepository = agentRepository;
    }

    /**
     * Atén les peticions GET a /dashboard. Carrega el nom i les inicials de
     * l'usuari i, segons el rol, les dades que necessita cada tauler. Si no hi
     * ha sessió iniciada redirigeix al login.
     *
     * @param authentication dades de l'usuari autenticat (correu i rols)
     * @param model model per enviar dades a la plantilla
     * @return plantilla del tauler que correspon al rol, o redirecció al login
     */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Authentication authentication, Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);

        String nomMostrar = "Usuari";
        Client clientLogat = null;

        if (usuariOpt.isPresent()) {
            Usuari usuari = usuariOpt.get();
            if (usuari.getNom() != null && !usuari.getNom().trim().isEmpty()) {
                nomMostrar = usuari.getNom();
            }
            model.addAttribute("inicials", calcularInicials(usuari));

            if (usuari instanceof Client client) {
                model.addAttribute("client", client);
                clientLogat = client;
            }
        } else {
            model.addAttribute("inicials", "US");
        }

        model.addAttribute("nomClient", nomMostrar);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRADOR")
                || a.getAuthority().equalsIgnoreCase("ADMINISTRADOR"));

        boolean isAgent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_AGENT")
                || a.getAuthority().equalsIgnoreCase("AGENT"));

        if (isAdmin) {
            model.addAttribute("clientsPendents", clientService.llistarClientsPendents());
            carregarDadesDashboardAdmin(model);
            return "administrador/dashboard";
        } else if (isAgent) {
            carregarDadesDashboardAgent(model);
            return "agent/dashboard";
        } else {
            if (clientLogat != null) {
                String dni = clientLogat.getDniUsuari();
                Reserva propera = reservaService.obtenirProperaReserva(dni);
                model.addAttribute("properaReserva", propera);
                model.addAttribute("lloguersCompletats", reservaService.comptarLloguersCompletats(dni));
            }
            return "client/inici-client";
        }
    }

    /**
     * Dies per davant (a partir d'avui) que es consideren "imminents" al tauler
     * de l'administrador.
     */
    private static final int DIES_IMMINENTS = 3;

    /**
     * Màxim de files de la taula d'entregues i devolucions imminents.
     */
    private static final int MAX_IMMINENTS = 8;

    /**
     * Carrega els indicadors del tauler de l'administrador: agents actius,
     * ocupació de la flota, reserves en curs i les entregues i devolucions
     * imminents (les d'avui, les dels propers {@value #DIES_IMMINENTS} dies i
     * les endarrerides).
     *
     * @param model model de la vista
     */
    private void carregarDadesDashboardAdmin(Model model) {
        LocalDate avui = LocalDate.now();
        model.addAttribute("avui", avui);
        model.addAttribute("dataActual", formatarDataCatala(avui));

        // Agents (només rol Agent; els administradors no compten): actius i total registrats
        List<Agent> agents = agentRepository.findAll().stream()
                .filter(a -> a.getRol() == Enums.Rol.Agent)
                .toList();
        model.addAttribute("agentsActius", agents.stream().filter(Agent::isActiu).count());
        model.addAttribute("totalAgentsRegistrats", agents.size());

        // Flota (mateix càlcul que la pantalla de vehicles i el tauler de l'agent)
        long totalVehicles = vehicleLogic.countVehicles();
        long vehiclesLlogats = vehicleLogic.countLlogats();
        long vehiclesInactius = vehicleLogic.countPerEstat(Enums.EstatVehicle.Inactiu);
        long vehiclesDisponibles = Math.max(0, totalVehicles - vehiclesLlogats - vehiclesInactius);
        long ocupacioFlota = totalVehicles == 0 ? 0 : Math.round(vehiclesLlogats * 100.0 / totalVehicles);
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("vehiclesDisponibles", vehiclesDisponibles);
        model.addAttribute("ocupacioFlota", ocupacioFlota);

        // Reserves en curs: Activa (pendent d'entrega) + Lliurada
        long pendentsEntrega = reservaRepository.countByEstat(Enums.EstatReserva.Activa);
        long lliurades = reservaRepository.countByEstat(Enums.EstatReserva.Lliurada);
        model.addAttribute("reservesPendentsEntrega", pendentsEntrega);
        model.addAttribute("reservesLliurades", lliurades);
        model.addAttribute("reservesActivesTotal", pendentsEntrega + lliurades);

        // Mateixa llista que veu l'agent (vegeu operacionsImminents)
        model.addAttribute("reservesImminents", operacionsImminents(avui));

        // Incidències dels retorns i alertes prioritàries: mateix càlcul que el tauler de l'agent.
        // Els clients pendents de validar ja tenen la targeta "Cua de validació" al tauler de
        // l'administrador, per això no es repeteixen com a alerta.
        List<Reserva> reserves = reservaRepository.findByEstatInOrderByDataIniciAsc(
                List.of(Enums.EstatReserva.Activa,
                        Enums.EstatReserva.Lliurada,
                        Enums.EstatReserva.Finalitzada));
        afegirIncidencies(model, reserves);
        model.addAttribute("alertesPrioritaries",
                construirAlertes(avui, LocalDateTime.now(), reserves, false));
    }

    /**
     * Entregues (reserves Activa, per data d'inici) i devolucions (reserves
     * Lliurada, per data de fi) que són d'avui, dels propers
     * {@value #DIES_IMMINENTS} dies o que ja van tard, ordenades per data i
     * amb un màxim de {@value #MAX_IMMINENTS} files.
     *
     * Aquesta és l'única definició d'"entregues i devolucions imminents": la
     * fan servir tant el tauler de l'administrador com el de l'agent, de
     * manera que mai poden mostrar operacions diferents.
     *
     * @param avui data d'avui
     * @return reserves amb l'operació imminent, ordenades per data
     */
    private List<Reserva> operacionsImminents(LocalDate avui) {
        LocalDate limit = avui.plusDays(DIES_IMMINENTS);
        return reservaRepository.findByEstatInOrderByDataIniciAsc(
                List.of(Enums.EstatReserva.Activa, Enums.EstatReserva.Lliurada)).stream()
                .filter(r -> dataOperacio(r) != null && !dataOperacio(r).isAfter(limit))
                .sorted(Comparator.comparing(DashBoardController::dataOperacio))
                .limit(MAX_IMMINENTS)
                .toList();
    }

    /**
     * Data en què s'ha de fer l'operació d'una reserva: devolució (fi) si està
     * lliurada, entrega (inici) si no.
     *
     * @param r reserva activa o lliurada
     * @return data de la devolució (si està lliurada) o de l'entrega
     */
    private static LocalDate dataOperacio(Reserva r) {
        return r.getEstat() == Enums.EstatReserva.Lliurada ? r.getDataFi() : r.getDataInici();
    }

    /**
     * Carrega totes les mètriques reals, moviments d'avui, alertes i estat de
     * la flota per al tauler de l'agent. Calcula els lliuraments i les
     * devolucions d'avui, les devolucions amb retard, les incidències dels
     * retorns, l'estat de la flota (disponibles, llogats i inactius, amb el seu
     * percentatge), la taula de moviments imminents i les alertes prioritàries
     * (devolucions amb retard, clients pendents de validar i documents
     * caducats).
     *
     * @param model model on es deixen totes les dades per a la plantilla
     */
    private void carregarDadesDashboardAgent(Model model) {
        LocalDate avui = LocalDate.now();
        LocalDateTime ara = LocalDateTime.now();
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fmtDataCurta = DateTimeFormatter.ofPattern("dd/MM");

        // 1. Data actual formatada en català (ex. "DIJOUS, 12 DE NOVEMBRE DE 2026")
        model.addAttribute("dataActualText", formatarDataCatala(avui));

        // 2. Reserves no anul·lades per calcular entregues, devolucions, retards i incidències
        List<Reserva> reserves = reservaRepository.findByEstatInOrderByDataIniciAsc(
                List.of(Enums.EstatReserva.Activa,
                        Enums.EstatReserva.Lliurada,
                        Enums.EstatReserva.Finalitzada));

        // Entregues d'avui (reserves amb data d'inici avui)
        long entreguesAvui = reserves.stream()
                .filter(r -> avui.equals(r.getDataInici()))
                .count();

        long entreguesPendentsAvui = reserves.stream()
                .filter(r -> avui.equals(r.getDataInici())
                && r.getEstat() == Enums.EstatReserva.Activa)
                .count();

        // Devolucions d'avui (reserves amb data de fi avui)
        long devolucionsAvui = reserves.stream()
                .filter(r -> avui.equals(r.getDataFi()))
                .count();

        long devolucionsPendentsAvui = reserves.stream()
                .filter(r -> avui.equals(r.getDataFi())
                && r.getEstat() == Enums.EstatReserva.Lliurada)
                .count();

        // Devolucions en curs que ja han superat l'hora/data límit de retorn
        List<Reserva> reservesAmbRetard = filtrarReservesAmbRetard(reserves, ara);

        // Incidències registrades als retorns (incidenciesObertes i incidenciesImputables al model)
        afegirIncidencies(model, reserves);

        // 3. Estat real de la flota (mateix càlcul que VehicleController)
        long totalVehicles = vehicleLogic.countVehicles();
        long vehiclesLlogats = vehicleLogic.countLlogats();
        long vehiclesInactius = vehicleLogic.countPerEstat(Enums.EstatVehicle.Inactiu);
        long vehiclesDisponibles = Math.max(0, totalVehicles - vehiclesLlogats - vehiclesInactius);

        long pctDisponibles = totalVehicles == 0 ? 0 : Math.round(vehiclesDisponibles * 100.0 / totalVehicles);
        long pctLlogats = totalVehicles == 0 ? 0 : Math.round(vehiclesLlogats * 100.0 / totalVehicles);
        long pctInactius = totalVehicles == 0 ? 0 : Math.max(0, 100 - pctDisponibles - pctLlogats);

        // 4. Taula d'entregues i devolucions imminents: mateixa llista que veu l'administrador
        //    (avui, propers dies i endarrerides), només que aquí es mostra amb l'estat operatiu.
        List<MovimentImminentDTO> movimentsImminents = new ArrayList<>();
        for (Reserva r : operacionsImminents(avui)) {
            LocalDate dataOp = dataOperacio(r);
            String etiquetaData = avui.equals(dataOp) ? "Avui" : dataOp.format(fmtDataCurta);

            if (r.getEstat() == Enums.EstatReserva.Activa) {
                // Entrega pendent: endarrerida si la data d'inici ja ha passat
                boolean endarrerida = dataOp.isBefore(avui);
                boolean vehiclePreparat = r.getVehicle() != null
                        && r.getVehicle().getEstat() == Enums.EstatVehicle.Disponible;
                String estat = endarrerida ? "Endarrerida" : (vehiclePreparat ? "✓ Preparat" : "No disponible");
                String color = (!endarrerida && vehiclePreparat) ? "VERD" : "ROIG";
                movimentsImminents.add(new MovimentImminentDTO(
                        r.getIdReserva(),
                        etiquetaData,
                        r.getCodiReserva(),
                        nomCompletClient(r.getClient()),
                        descripcioVehicle(r.getVehicle()),
                        "Entrega",
                        estat,
                        color
                ));
            } else {
                // Devolució pendent: amb retard si ja ha passat l'hora/data límit de retorn
                LocalDateTime limitRetorn = reservaService.calcularLimitRetorn(r);
                boolean retard = ara.isAfter(limitRetorn);
                String horaText = (avui.equals(dataOp) && r.getDataLliurament() != null)
                        ? limitRetorn.format(fmtHora) : etiquetaData;
                movimentsImminents.add(new MovimentImminentDTO(
                        r.getIdReserva(),
                        horaText,
                        r.getCodiReserva(),
                        nomCompletClient(r.getClient()),
                        descripcioVehicle(r.getVehicle()),
                        "Devolució",
                        retard ? "Amb retard" : "En curs",
                        retard ? "ROIG" : "AMBRE"
                ));
            }
        }

        // 5. Alertes prioritàries reals (retards, clients pendents i documents caducats)
        List<AlertaPrioritariaDTO> alertes = construirAlertes(avui, ara, reserves, true);

        // Enviem tot al Model
        model.addAttribute("entreguesAvui", entreguesAvui);
        model.addAttribute("entreguesPendentsAvui", entreguesPendentsAvui);
        model.addAttribute("devolucionsAvui", devolucionsAvui);
        model.addAttribute("devolucionsPendentsAvui", devolucionsPendentsAvui);
        model.addAttribute("devolucionsAmbRetard", reservesAmbRetard.size());

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("vehiclesDisponibles", vehiclesDisponibles);
        model.addAttribute("vehiclesLlogats", vehiclesLlogats);
        model.addAttribute("vehiclesInactius", vehiclesInactius);
        model.addAttribute("pctDisponibles", pctDisponibles);
        model.addAttribute("pctLlogats", pctLlogats);
        model.addAttribute("pctInactius", pctInactius);

        model.addAttribute("reservesImminents", movimentsImminents);
        model.addAttribute("alertesPrioritaries", alertes);
    }

    /**
     * Reserves lliurades (vehicle recollit i no retornat) que ja han superat
     * l'hora/data límit de retorn, de la més endarrerida a la menys.
     *
     * @param reserves reserves a revisar
     * @param ara moment actual
     * @return reserves amb la devolució endarrerida
     */
    private List<Reserva> filtrarReservesAmbRetard(List<Reserva> reserves, LocalDateTime ara) {
        return reserves.stream()
                .filter(r -> r.getEstat() == Enums.EstatReserva.Lliurada)
                .filter(r -> ara.isAfter(reservaService.calcularLimitRetorn(r)))
                .sorted(Comparator.comparing(reservaService::calcularLimitRetorn))
                .toList();
    }

    /**
     * Afegeix al model els indicadors d'incidències registrades als retorns:
     * quantes n'hi ha ({@code incidenciesObertes}) i quantes tenen un
     * desperfecte imputable al client ({@code incidenciesImputables}). Els fan
     * servir els taulers de l'administrador i de l'agent.
     *
     * @param model model de la vista
     * @param reserves reserves a revisar
     */
    private void afegirIncidencies(Model model, List<Reserva> reserves) {
        List<Reserva> ambIncidencia = reserves.stream()
                .filter(r -> r.getIncidenciaRetorn() != null && !r.getIncidenciaRetorn().isBlank())
                .toList();
        model.addAttribute("incidenciesObertes", ambIncidencia.size());
        model.addAttribute("incidenciesImputables", ambIncidencia.stream()
                .filter(r -> Boolean.TRUE.equals(r.getDesperfecteImputable()))
                .count());
    }

    /**
     * Construeix les alertes prioritàries: devolucions amb retard, clients
     * pendents de validació (opcional) i clients actius amb el permís de
     * conduir o el document d'identitat caducat.
     *
     * @param avui data d'avui
     * @param ara moment actual
     * @param reserves reserves a revisar (per detectar els retards)
     * @param incloureClientsPendents si és true s'afegeix una alerta per cada
     * client pendent de validar
     * @return alertes, amb les de retard (vermelles) primer
     */
    private List<AlertaPrioritariaDTO> construirAlertes(LocalDate avui, LocalDateTime ara,
            List<Reserva> reserves, boolean incloureClientsPendents) {
        List<AlertaPrioritariaDTO> alertes = new ArrayList<>();
        List<Reserva> reservesAmbRetard = filtrarReservesAmbRetard(reserves, ara);


        // A) Devolucions amb retard
        for (Reserva r : reservesAmbRetard) {
            long minutsRetard = Math.max(1, Duration.between(reservaService.calcularLimitRetorn(r), ara).toMinutes());
            String retardText = formatarRetard(minutsRetard);
            String telefon = (r.getClient() != null && r.getClient().getTelefon() != null)
                    ? " · " + r.getClient().getTelefon()
                    : " · Truca al client";
            alertes.add(new AlertaPrioritariaDTO(
                    "ROIG",
                    "Devolució amb +" + retardText + " de retard",
                    r.getCodiReserva() + " · " + nomCompletClient(r.getClient()) + telefon,
                    "/agent/reserves/" + r.getIdReserva()
            ));
        }

        // B) Clients pendents de validació
        if (incloureClientsPendents) {
            for (Client c : clientService.llistarClientsPendents()) {
                alertes.add(new AlertaPrioritariaDTO(
                        "AMBRE",
                        "Client pendent de validació",
                        "Client - " + nomCompletClient(c) + " (" + c.getDniUsuari() + ")",
                        "/admin/agent/validacions"
                ));
            }
        }

        // C) Clients actius amb permís de conduir o DNI caducat
        for (Client c : clientService.llistarNomesClients()) {
            if (!c.isActiu()) {
                continue;
            }
            if (c.getDataCaducitatLlicencia() != null && c.getDataCaducitatLlicencia().isBefore(avui)) {
                alertes.add(new AlertaPrioritariaDTO(
                        "AMBRE",
                        "Permís de conduir caducat",
                        "Client - " + nomCompletClient(c),
                        "/admin/agent/clients"
                ));
            } else if (c.getDataCaducitatDni() != null && c.getDataCaducitatDni().isBefore(avui)) {
                alertes.add(new AlertaPrioritariaDTO(
                        "AMBRE",
                        "Document d'identitat caducat",
                        "Client - " + nomCompletClient(c),
                        "/admin/agent/clients"
                ));
            }
        }

        return alertes;
    }

    /**
     * Dona format a una data en català i en majúscules, p. ex. "DIJOUS, 12 DE
     * NOVEMBRE DE 2026". Davant de vocal usa "D'" en lloc de "DE".
     *
     * @param data data a formatar
     * @return data en text
     */
    private String formatarDataCatala(LocalDate data) {
        Locale catala = Locale.forLanguageTag("ca-ES");
        String diaSetmana = data.format(DateTimeFormatter.ofPattern("EEEE", catala)).toUpperCase(catala);
        int dia = data.getDayOfMonth();
        String mes = data.format(DateTimeFormatter.ofPattern("LLLL", catala)).toLowerCase(catala);
        String prep = (mes.startsWith("a") || mes.startsWith("e") || mes.startsWith("i")
                || mes.startsWith("o") || mes.startsWith("u")) ? "D'" : "DE ";
        return diaSetmana + ", " + dia + " " + prep + mes.toUpperCase(catala) + " DE " + data.getYear();
    }

    /**
     * Converteix un retard en minuts a un text curt: "45 min", "2 h" o "2 h 15
     * min".
     *
     * @param minutsTotals retard total en minuts
     * @return retard en text
     */
    private String formatarRetard(long minutsTotals) {
        if (minutsTotals < 60) {
            return minutsTotals + " min";
        }
        long hores = minutsTotals / 60;
        long minuts = minutsTotals % 60;
        return minuts == 0 ? hores + " h" : hores + " h " + minuts + " min";
    }

    /**
     * Uneix nom i cognoms d'un client sense espais de més.
     *
     * @param c client (pot ser null)
     * @return nom complet, o "Client" si el client és null
     */
    private String nomCompletClient(Client c) {
        if (c == null) {
            return "Client";
        }
        String nom = c.getNom() == null ? "" : c.getNom().trim();
        String cognoms = c.getCognoms() == null ? "" : c.getCognoms().trim();
        return (nom + " " + cognoms).trim();
    }

    /**
     * Descripció curta d'un vehicle per a les taules, p. ex. "Seat Ibiza
     * (1234ABC)".
     *
     * @param v vehicle (pot ser null)
     * @return marca, model i matrícula, o "Vehicle" si és null
     */
    private String descripcioVehicle(Vehicle v) {
        if (v == null) {
            return "Vehicle";
        }
        return v.getMarca() + " " + v.getModel() + " (" + v.getMatricula() + ")";
    }

    /**
     * Calcula les inicials de l'usuari per a l'avatar (p. ex. "Joan Pérez" dóna
     * "JP").
     *
     * @param usuari usuari autenticat
     * @return inicials en majúscules, o "US" si no té nom ni cognoms
     */
    private String calcularInicials(Usuari usuari) {
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();

        String inicials = "";
        if (!nom.isEmpty()) {
            inicials += nom.charAt(0);
        }
        if (!cognoms.isEmpty()) {
            inicials += cognoms.charAt(0);
        }
        return inicials.isEmpty() ? "US" : inicials.toUpperCase();
    }

    /**
     * Fila de la taula de lliuraments i devolucions imminents del tauler de
     * l'agent.
     *
     * @param idReserva identificador de la reserva (per enllaçar al detall)
     * @param hora hora o data del moviment ("Avui", "dd/MM" o "HH:mm")
     * @param codiReserva codi de la reserva
     * @param clientNom nom complet del client
     * @param vehicleModel marca, model i matrícula del vehicle
     * @param tipusOperacio "Entrega" o "Devolució"
     * @param estat text de l'estat ("Amb retard", "En curs", "✓ Preparat"...)
     * @param colorEstat color de l'etiqueta: "ROIG", "AMBRE" o "VERD"
     */
    public record MovimentImminentDTO(
            Long idReserva,
            String hora,
            String codiReserva,
            String clientNom,
            String vehicleModel,
            String tipusOperacio,
            String estat,
            String colorEstat) {

    }

    /**
     * Alerta prioritària del tauler de l'agent.
     *
     * @param nivell gravetat de l'alerta: "ROIG" o "AMBRE"
     * @param titol títol curt de l'alerta
     * @param subtitol detall (codi de reserva, client, telèfon...)
     * @param url enllaç a la pantalla on es resol l'alerta
     */
    public record AlertaPrioritariaDTO(
            String nivell,
            String titol,
            String subtitol,
            String url) {

    }
}