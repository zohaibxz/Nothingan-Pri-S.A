package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import cat.copernic.Projecte1_Equip5.service.ReservaArxiuService;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de les reserves per a agents i administradors: llistat amb
 * filtres (RF52 i RF54), detall, anul·lació (RF55), lliurament del vehicle
 * (RF49), devolució (RF48) i consulta de les fotos del lliurament i de la
 * devolució, i creació de reserves en nom d'un client (assistent en 3 passos:
 * client, vehicle i dates, confirmació). La reserva pròpia de l'agent
 * reutilitza les pantalles del client (/client/reserva/nueva).
 */
@Controller
@RequestMapping("/agent")
public class ReservaAgentController {

    private final ReservaService reservaService;
    private final ReservaArxiuService reservaArxiuService;
    private final VehicleLogic vehicleLogic;

    /**
     * Constructor amb injecció de dependències.
     *
     * @param reservaService servei amb la lògica de negoci de les reserves
     * @param reservaArxiuService servei de les fotos de lliurament i devolució
     * @param vehicleLogic lògica de vehicles (fotos principals)
     */
    public ReservaAgentController(ReservaService reservaService,
            ReservaArxiuService reservaArxiuService,
            VehicleLogic vehicleLogic) {
        this.reservaService = reservaService;
        this.reservaArxiuService = reservaArxiuService;
        this.vehicleLogic = vehicleLogic;
    }

    private static final String VISTA_NOVA = "agent/agent-reserva-nova";
    private static final String VISTA_CONFIRMAR = "agent/agent-reserva-confirmar";

    /**
     * Passos 1 i 2 de la reserva per a un client. Sense client triat mostra el
     * cercador de clients (només actius i validats). Amb client triat en
     * comprova l'aptitud i, si ja s'han indicat les dates, llista els vehicles
     * disponibles. Tot el que no es pot mostrar es deixa fora del model; la
     * plantilla decideix què ensenyar segons els atributs presents.
     *
     * @param q text de cerca de client (nom, cognoms, DNI o correu)
     * @param dniClient DNI del client triat (opcional)
     * @param dataInici primer dia del lloguer (opcional)
     * @param dataFi dia de devolució (opcional)
     * @param categoria categoria de vehicle per filtrar (opcional)
     * @param model model de la vista
     * @return plantilla de l'assistent de nova reserva
     */
    @GetMapping("/reserves/nova")
    public String novaReserva(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String dniClient,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            @RequestParam(required = false) Enums.CategoriaVehicle categoria,
            Model model) {

        model.addAttribute("q", q);
        model.addAttribute("avui", LocalDate.now());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("categoria", categoria);
        model.addAttribute("categories", Enums.CategoriaVehicle.values());

        // Pas 1: cap client triat, es mostra el cercador
        if (dniClient == null || dniClient.isBlank()) {
            model.addAttribute("clients", reservaService.cercarClientsReservables(q));
            return VISTA_NOVA;
        }

        try {
            Client client = reservaService.obtenirClient(dniClient);
            String motiu = reservaService.motiuClientNoApte(client, dataFi);
            model.addAttribute("client", client);
            model.addAttribute("motiuClientNoApte", motiu);

            // Pas 2: només es busquen vehicles si el client és apte i hi ha les dues dates
            if (motiu == null && dataInici != null && dataFi != null) {
                var vehicles = reservaService.vehiclesDisponibles(dataInici, dataFi, categoria);
                model.addAttribute("vehicles", vehicles);
                model.addAttribute("fotosPrincipals", vehicleLogic.fotosPrincipalsDe(vehicles));
                model.addAttribute("dies", reservaService.calcularDies(dataInici, dataFi));
                model.addAttribute("hores", reservaService.calcularHores(dataInici, dataFi));
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("clients", reservaService.cercarClientsReservables(q));
        }
        return VISTA_NOVA;
    }

    /**
     * Pas 3: resum de la reserva amb import, fiança i total a pagar abans de
     * crear-la. Si alguna cosa falla torna al pas 2 amb l'error.
     *
     * @param dniClient DNI del client
     * @param matricula matrícula del vehicle escollit
     * @param dataInici primer dia del lloguer
     * @param dataFi dia de devolució
     * @param model model de la vista
     * @param redirect atributs flash per mostrar l'error
     * @return plantilla del resum, o redirecció a l'assistent si hi ha un error
     */
    @GetMapping("/reserves/nova/confirmar")
    public String confirmarNovaReserva(
            @RequestParam String dniClient,
            @RequestParam String matricula,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            Model model, RedirectAttributes redirect) {

        try {
            Client client = reservaService.obtenirClient(dniClient);
            Vehicle vehicle = reservaService.obtenirVehicle(matricula);
            String motiu = reservaService.motiuClientNoApte(client, dataFi);
            if (motiu != null) {
                throw new IllegalStateException(motiu);
            }
            reservaService.validarReservable(vehicle, dataInici, dataFi);

            BigDecimal importTotal = reservaService.calcularImportTotal(vehicle, dataInici, dataFi);
            BigDecimal fianca = reservaService.calcularFianca(vehicle, client);

            model.addAttribute("client", client);
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);
            model.addAttribute("dies", reservaService.calcularDies(dataInici, dataFi));
            model.addAttribute("importTotal", importTotal);
            model.addAttribute("fianca", fianca);
            model.addAttribute("totalPagar", importTotal.add(fianca));
            model.addAttribute("fotosPrincipals", vehicleLogic.fotosPrincipalsDe(List.of(vehicle)));
            return VISTA_CONFIRMAR;
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addAttribute("dniClient", dniClient);
            redirect.addAttribute("dataInici", dataInici);
            redirect.addAttribute("dataFi", dataFi);
            return "redirect:/agent/reserves/nova";
        }
    }

    /**
     * Crea la reserva en nom del client (la validació i el càlcul els fa
     * {@link ReservaService#crearReservaPerClient}). Si va bé porta al detall
     * de la reserva; si falla torna al resum amb l'error.
     *
     * @param dniClient DNI del client
     * @param matricula matrícula del vehicle
     * @param dataInici primer dia del lloguer
     * @param dataFi dia de devolució
     * @param redirect atributs per mostrar el resultat
     * @return redirecció al detall de la reserva creada, o al resum si falla
     */
    @PostMapping("/reserves/nova/crear")
    public String crearNovaReserva(
            @RequestParam String dniClient,
            @RequestParam String matricula,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            RedirectAttributes redirect) {

        try {
            Reserva reserva = reservaService.crearReservaPerClient(
                    dniClient, matricula, dataInici, dataFi);
            redirect.addFlashAttribute("missatgeExit",
                    "Reserva " + reserva.getCodiReserva() + " creada correctament per a "
                    + reserva.getClient().getNom() + " " + reserva.getClient().getCognoms() + ".");
            return "redirect:/agent/reserves/" + reserva.getIdReserva();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addAttribute("dniClient", dniClient);
            redirect.addAttribute("matricula", matricula);
            redirect.addAttribute("dataInici", dataInici);
            redirect.addAttribute("dataFi", dataFi);
            return "redirect:/agent/reserves/nova/confirmar";
        }
    }

    /**
     * Llista les reserves de tots els clients amb filtres opcionals
     * combinables.
     *
     * @param dni DNI del client
     * @param email correu del client
     * @param data dia que ha d'estar dins del període de la reserva
     * @param matricula matrícula del vehicle
     * @param codi codi de la reserva
     * @param estat estat de la reserva (Activa, Lliurada, Finalitzada o
     * Anul_lada)
     * @param incidencies si és true, només es mostren les reserves amb una
     * incidència registrada al retorn
     * @param model model de la vista
     * @return plantilla del llistat de reserves
     */
    @GetMapping("/reserves")
    public String llistarReserves(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String estat,
            @RequestParam(required = false) Boolean incidencies,
            Model model) {

        // Convertim l'estat seleccionat a l'enum corresponent
        Enums.EstatReserva estatSeleccionat = null;

        if (estat != null && !estat.isBlank()) {
            try {
                estatSeleccionat = Enums.EstatReserva.valueOf(estat);
            } catch (IllegalArgumentException e) {
                estatSeleccionat = null;
            }
        }

        // Llistat amb tots els filtres
        List<Reserva> llistat = reservaService.llistarReservesAgentFiltrades(
                dni, email, data, matricula, codi, estatSeleccionat);

        // Filtre "només amb incidència": reserves amb text d'incidència al retorn
        boolean nomesIncidencies = Boolean.TRUE.equals(incidencies);
        if (nomesIncidencies) {
            llistat = llistat.stream()
                    .filter(r -> r.getIncidenciaRetorn() != null && !r.getIncidenciaRetorn().isBlank())
                    .toList();
        }
        model.addAttribute("reservesActives", llistat);
        model.addAttribute("nomesIncidencies", nomesIncidencies);

        // Comptadors del tauler
        model.addAttribute("pendentsEntregaCount",
                reservaService.comptarReservesActives());

        model.addAttribute("lliuradesCount",
                reservaService.comptarReservesLliurades());

        // Conservem els valors del formulari després de filtrar
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("data", data);
        model.addAttribute("matricula", matricula);
        model.addAttribute("codi", codi);
        model.addAttribute("estatSeleccionat", estat);

        return "agent/agent-reserves";
    }

    /**
     * Mostra el detall d'una reserva amb les accions que permet el seu estat.
     * Si no es pot lliurar, passa el motiu perquè la pantalla l'expliqui en
     * lloc del formulari.
     *
     * @param id identificador de la reserva
     * @param model model de la vista
     * @return plantilla del detall, o redirecció al llistat si no existeix
     */
    @GetMapping("/reserves/{id}")
    public String veureDetallsReserva(
            @PathVariable Long id,
            Model model) {

        try {
            Reserva reserva = reservaService.obtenirDetallsReservaAgent(id);

            model.addAttribute("reserva", reserva);
            // null si es pot lliurar; si no, el motiu que es mostra en lloc del formulari
            model.addAttribute("motiuNoLliurable", reservaService.motiuNoLliurable(reserva));
            // Hora actual (sense segons) com a valor per defecte del formulari de lliurament
            model.addAttribute("araLocal",
                    LocalDateTime.now().withSecond(0).withNano(0).toString());
            // Moment límit de devolució (per mostrar-lo al formulari de retorn)
            model.addAttribute("limitRetorn", reservaService.calcularLimitRetorn(reserva));
            model.addAttribute("fotosLliurament", reservaArxiuService.llistarFotos(
                    id, ReservaArxiu.Moment.LLIURAMENT));
            model.addAttribute("fotosRetorn", reservaArxiuService.llistarFotos(
                    id, ReservaArxiu.Moment.RETORN));

            return "agent/agent-reserva-detalle";

        } catch (IllegalArgumentException e) {
            return "redirect:/agent/reserves";
        }
    }

    /**
     * Anul·la una reserva activa en nom del client i calcula la devolució de
     * l'import.
     *
     * @param id identificador de la reserva
     * @param motiuAnulacio motiu de l'anul·lació
     * @param percentatgeDevolucio percentatge (0-100) de l'import i la fiança
     * que es retorna
     * @param redirectAttributes atributs per mostrar el resultat
     * @return redirecció al detall de la reserva
     */
    @PostMapping("/reserves/{id}/anular")
    public String anularReserva(
            @PathVariable Long id,
            @RequestParam String motiuAnulacio,
            @RequestParam BigDecimal percentatgeDevolucio,
            RedirectAttributes redirectAttributes) {

        try {
            BigDecimal devolucio = reservaService.anullarReservaAgent(
                    id, motiuAnulacio, percentatgeDevolucio);

            redirectAttributes.addFlashAttribute(
                    "missatgeExit",
                    "Reserva anul·lada correctament. Import retornat: "
                    + devolucio + " €");

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "missatgeError", e.getMessage());
        }

        return "redirect:/agent/reserves/" + id;
    }

    /**
     * RF49: lliura el vehicle d'una reserva (data i hora, descripció de l'estat
     * i fotos opcionals). Els camps no són required perquè el servei els valida
     * i torna un missatge en català en lloc d'una pàgina d'error.
     *
     * @param id identificador de la reserva
     * @param dataHoraLliurament data i hora en què es lliura el vehicle
     * @param descripcioEstat descripció de l'estat del vehicle en el moment del
     * lliurament
     * @param fotos fotos opcionals de l'estat del vehicle (pot ser null)
     * @param redirectAttributes atributs per mostrar el missatge a la pantalla
     * següent
     * @return redirecció al detall de la reserva
     */
    @PostMapping("/reserves/{id}/lliurar")
    public String lliurarVehicle(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraLliurament,
            @RequestParam(required = false) String descripcioEstat,
            @RequestParam(name = "fotos", required = false) List<MultipartFile> fotos,
            RedirectAttributes redirectAttributes) {

        try {
            reservaService.lliurarVehicle(id, dataHoraLliurament, descripcioEstat, fotos);
            redirectAttributes.addFlashAttribute("missatgeExit",
                    "Vehicle lliurat correctament. La reserva ha passat a Lliurada.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("missatgeError", e.getMessage());
        }

        return "redirect:/agent/reserves/" + id;
    }

    /**
     * RF48: registra el retorn del vehicle (data i hora, comentari, incidència,
     * si el desperfecte és imputable al client i fotos opcionals) i finalitza
     * la reserva. El missatge diu què passa amb la fiança.
     *
     * @param id identificador de la reserva
     * @param dataHoraRetorn data i hora en què es retorna el vehicle
     * @param comentariAgent comentari de l'agent sobre el retorn
     * @param incidencia incidència detectada en el retorn (opcional)
     * @param desperfecteImputable true si el desperfecte s'imputa al client (es
     * reté la fiança)
     * @param fotos fotos opcionals de l'estat del vehicle (pot ser null)
     * @param redirectAttributes atributs per mostrar el missatge a la pantalla
     * següent
     * @return redirecció al detall de la reserva
     */
    @PostMapping("/reserves/{id}/devolver")
    public String retornarVehicle(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraRetorn,
            @RequestParam(required = false) String comentariAgent,
            @RequestParam(required = false) String incidencia,
            @RequestParam(name = "desperfecteImputable", defaultValue = "false") boolean desperfecteImputable,
            @RequestParam(name = "fotos", required = false) List<MultipartFile> fotos,
            RedirectAttributes redirectAttributes) {

        try {
            Reserva r = reservaService.retornarVehicle(id, dataHoraRetorn,
                    comentariAgent, incidencia, desperfecteImputable, fotos);

            StringBuilder missatge = new StringBuilder(
                    "Vehicle retornat correctament. La reserva ha passat a Finalitzada. ");
            if (Boolean.TRUE.equals(r.getDesperfecteImputable())) {
                missatge.append("Desperfecte imputable al client: fiança retinguda (")
                        .append(r.getFianca()).append(" €).");
            } else {
                missatge.append("Fiança retornada: ").append(r.getImportRetornat()).append(" €");
                if (r.getHoresRetard() != null && r.getHoresRetard() > 0) {
                    missatge.append(" (descomptats ").append(r.getDescompteRetard())
                            .append(" € per ").append(r.getHoresRetard())
                            .append(r.getHoresRetard() == 1 ? " hora" : " hores")
                            .append(" de retard)");
                }
                missatge.append(".");
            }
            redirectAttributes.addFlashAttribute("missatgeExit", missatge.toString());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("missatgeError", e.getMessage());
        }

        return "redirect:/agent/reserves/" + id;
    }

    /**
     * Mostra una foto d'una reserva (només personal: la ruta és de /agent/**).
     * Torna 404 si la foto no existeix o no és d'aquesta reserva.
     *
     * @param id identificador de la reserva
     * @param idArxiu identificador de la foto a MongoDB
     * @return la imatge amb el seu tipus de contingut, o 404 si no existeix
     */
    @GetMapping("/reserves/{id}/fotos/{idArxiu}")
    public ResponseEntity<byte[]> veureFoto(
            @PathVariable Long id,
            @PathVariable String idArxiu) {

        try {
            ReservaArxiu foto = reservaArxiuService.obtenirFoto(id, idArxiu);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(foto.getContentType()))
                    .body(foto.getDades());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
