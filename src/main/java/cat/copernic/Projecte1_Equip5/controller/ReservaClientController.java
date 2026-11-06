package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.config.UsuariPrincipal;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.service.DocumentacioService;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Controlador de les pàgines de reserves del client (rutes /client/...). Només
 * rep les peticions i prepara el model; la lògica és a {@link ReservaService}.
 * La cerca de vehicles (/client/reserva/nueva), el detall de vehicle i la
 * pantalla "Les meves reserves" són públics; la resta exigeix client amb sessió
 * (ho controla WebSecurityConfig) i, si no hi ha DNI, es redirigeix al login.
 */
@Controller
@RequestMapping("/client")
public class ReservaClientController {

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private final ReservaService reservaService;
    private final VehicleLogic vehicleLogic;
    private final DocumentacioService documentacioService;

    public ReservaClientController(
            ReservaService reservaService,
            VehicleLogic vehicleLogic,
            DocumentacioService documentacioService) {

        this.reservaService = reservaService;
        this.vehicleLogic = vehicleLogic;
        this.documentacioService = documentacioService;
    }

    /**
     * DNI de l'usuari que ha iniciat sessió, o null si no n'hi ha. El DNI es
     * llegeix del principal de Spring Security (UsuariPrincipal), que el posa
     * CustomUserDetailsService en fer el login: no cal consultar la BD. Si el
     * principal no és un UsuariPrincipal (p. ex. una sessió antiga d'abans del
     * canvi) es retorna null i el controlador redirigeix al login.
     */
    private String dniClientLogat(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof UsuariPrincipal principal) {
            return principal.getDni();
        }
        return null;
    }

    /**
     * RF52: pàgina "Les meves reserves".
     */
    @GetMapping("/reserves")
    public String llistarReserves(
            @RequestParam(name = "pagProperes", defaultValue = "0") int pagProperes,
            @RequestParam(name = "pagHistorial", defaultValue = "0") int pagHistorial,
            Authentication authentication, Model model) {
        String dni = dniClientLogat(authentication);
        if (dni == null) {
            // Anònim: la pantalla és pública. Es mostra sense reserves i amb un avís per iniciar sessió
            model.addAttribute("properes", Page.empty());
            model.addAttribute("historial", Page.empty());
            model.addAttribute("anonim", true);
            return "client/reserves";
        }
        model.addAttribute("properes", reservaService.llistarProperes(dni, pagProperes));
        model.addAttribute("historial", reservaService.llistarHistorial(dni, pagHistorial));
        return "client/reserves";
    }

    /**
     * RF50 passos 1-2: formulari de dates + filtre per categoria + vehicles
     * disponibles. "vehicles" només es posa al model quan ja s'ha cercat (la
     * plantilla amaga la secció si és null). POSSIBLEMENT INCORRECTE: la ruta
     * "nueva" està en castellà perquè així surt a moduls-client.html.
     */
    @GetMapping("/reserva/nueva")
    public String novaReserva(
            @RequestParam(name = "dataInici", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(name = "dataFi", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            @RequestParam(name = "categoria", required = false) Enums.CategoriaVehicle categoria,
            Model model) {

        // Pantalla pública: la cerca de vehicles la pot fer un anònim.
        // El login només es demana en confirmar (/client/reserva/confirmar).
        model.addAttribute("avui", LocalDate.now());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("categoria", categoria);
        model.addAttribute("categories", Enums.CategoriaVehicle.values());

        // Només es busca quan l'usuari ja ha enviat les dues dates
        if (dataInici != null && dataFi != null) {
            try {
                var vehicles = reservaService.vehiclesDisponibles(
                        dataInici, dataFi, categoria);

                model.addAttribute("vehicles", vehicles);

// Guardar la foto principal de cada vehicle.
                java.util.Map<String, VehicleArxiu> fotosPrincipals
                        = new java.util.HashMap<>();

                for (Vehicle v : vehicles) {

                    vehicleLogic.fotoPrincipalDe(v.getMatricula()).ifPresent(
                            foto -> fotosPrincipals.put(v.getMatricula(), foto));
                }

                model.addAttribute("fotosPrincipals", fotosPrincipals);
                model.addAttribute("dies", reservaService.calcularDies(dataInici, dataFi));
                model.addAttribute("hores", reservaService.calcularHores(dataInici, dataFi));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "client/novaReserva";
    }

    /**
     * RF50 passos 3-5: resum amb import, fiança i total a pagar. Si alguna cosa
     * falla, torna al pas 1 amb l'error.
     */
    @GetMapping("/reserva/confirmar")
    public String confirmarReserva(
            @RequestParam(name = "matricula") String matricula,
            @RequestParam(name = "dataInici") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(name = "dataFi") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            Authentication authentication, Model model, RedirectAttributes redirect) {

        String dni = dniClientLogat(authentication);
        if (dni == null) {
            return REDIRECT_LOGIN;
        }
        try {
            Vehicle vehicle = reservaService.obtenirVehicle(matricula);
            reservaService.validarDates(dataInici, dataFi);
            if (!reservaService.estaDisponible(vehicle, dataInici, dataFi)) {
                throw new IllegalStateException("El vehicle ja no està disponible per a les dates triades.");
            }
            var client = reservaService.obtenirClient(dni);
            BigDecimal importTotal = reservaService.calcularImportTotal(vehicle, dataInici, dataFi);
            BigDecimal fianca = reservaService.calcularFianca(vehicle, client);

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);
            model.addAttribute("dies", reservaService.calcularDies(dataInici, dataFi));
            model.addAttribute("importTotal", importTotal);
            model.addAttribute("fianca", fianca);
            model.addAttribute("totalPagar", importTotal.add(fianca));
            model.addAttribute("client", client);
            return "client/confirmarReserva";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reserva/nueva";
        }
    }

    /**
     * RF50 passos 5-7: valida la casella de documentació i crea la reserva. Si
     * falla, torna a la confirmació amb l'error (redirect després del POST).
     */
    @PostMapping("/reserva/crear")
    public String crearReserva(
            @RequestParam(name = "matricula") String matricula,
            @RequestParam(name = "dataInici")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(name = "dataFi")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            @RequestParam(name = "documentacio", defaultValue = "false") boolean documentacio,
            @RequestParam(name = "identitat") MultipartFile identitat,
            @RequestParam(name = "carnet") MultipartFile carnet,
            Authentication authentication,
            RedirectAttributes redirect) {

        String dni = dniClientLogat(authentication);
        if (dni == null) {
            return REDIRECT_LOGIN;
        }
        try {
            if (!documentacio) {
                throw new IllegalArgumentException(
                        "Cal confirmar que entens que l'agent et pot demanar documentació en recollir el vehicle.");
            }

            // Primer la documentació (valida i desa a Mongo) i després la reserva:
            // així no es crea mai una reserva si la documentació falla.
            documentacioService.guardarDocumentacio(dni, identitat, carnet);

            Reserva reserva = reservaService.crearReserva(
                    dni, matricula, dataInici, dataFi);
            return "redirect:/client/reserva/confirmada/" + reserva.getCodiReserva();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addAttribute("matricula", matricula);
            redirect.addAttribute("dataInici", dataInici);
            redirect.addAttribute("dataFi", dataFi);
            return "redirect:/client/reserva/confirmar";
        }
    }

    /**
     * RF50 pas 7: pàgina amb el codi de reserva. Passa "reserva" (objecte) i
     * "codi" (text) a la plantilla.
     */
    @GetMapping("/reserva/confirmada/{codi}")
    public String reservaConfirmada(@PathVariable("codi") String codi,
            Authentication authentication, Model model, RedirectAttributes redirect) {

        String dni = dniClientLogat(authentication);
        if (dni == null) {
            return REDIRECT_LOGIN;
        }
        try {
            Reserva reserva = reservaService.obtenirReservaDelClient(codi, dni);
            model.addAttribute("reserva", reserva);
            model.addAttribute("codi", reserva.getCodiReserva());
            return "client/reservaConfirmada";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reserves";
        }
    }

    @GetMapping("/reserves/{id}")
    public String detallReserva(@PathVariable("id") Long id,
            Authentication authentication, Model model, RedirectAttributes redirect) {

        String dni = dniClientLogat(authentication);
        if (dni == null) {
            return REDIRECT_LOGIN;
        }
        try {
            Reserva reserva = reservaService.obtenirDetallsReserva(id, dni);
            model.addAttribute("reserva", reserva);
            return "client/detallReserva";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reserves";
        }
    }

    @GetMapping("/reserves/{id}/anullar")
    public String mostrarConfirmacioAnullacio(@PathVariable Long id,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        String dniClient = dniClientLogat(authentication);
        if (dniClient == null) {
            return "redirect:/login";
        }

        try {
            Reserva reserva = reservaService.obtenirDetallsReserva(id, dniClient);

            BigDecimal devolucio = reservaService.calcularDevolucio(reserva);

            model.addAttribute("reserva", reserva);
            model.addAttribute("devolucio", devolucio);

            return "client/anullarReserva";

        } catch (IllegalArgumentException e) {
            // Si la reserva no existeix o no és seva, el tornem al llistat amb un error
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reserves";
        }
    }

    @PostMapping("/reserves/{id}/anullar")
    public String processarAnullacio(@PathVariable Long id,
            Authentication authentication, RedirectAttributes redirectAttributes) {

        String dniClient = dniClientLogat(authentication);
        if (dniClient == null) {
            return "redirect:/login";
        }

        try {
            BigDecimal devolucio = reservaService.anullarReservaClient(id, dniClient);

            redirectAttributes.addFlashAttribute("missatge",
                    "Reserva anul·lada correctament. Se t'han retornat " + devolucio + " €.");

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/reserves";
    }

    // Detall d'un vehicle per al client: només dades comercials (sense fotos, documents ni comentaris)
    @GetMapping("/vehicles/{matricula}")
    public String detallVehicle(@PathVariable String matricula,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            @RequestParam(required = false) String categoria,
            Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("vehicle", reservaService.obtenirVehicle(matricula));
            model.addAttribute("fotos", vehicleLogic.fotosDe(matricula));
            model.addAttribute("esPersonal", false);   // amaga la secció "Dades privades"
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);
            model.addAttribute("categoria", categoria);
            model.addAttribute("page", 0);
            return "vehicles/detall";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reserva/nueva";
        }
    }

    @GetMapping("/vehicles/{matricula}/imatges/{id}")
    public ResponseEntity<byte[]> imatgeVehicleClient(
            @PathVariable String matricula,
            @PathVariable String id) {

        VehicleArxiu arxiu = vehicleLogic.getArxiu(id).orElse(null);

        // Comprovar que existeix, que pertany al vehicle
        // i que realment és una fotografia.
        if (arxiu == null
                || !matricula.equals(arxiu.getMatricula())
                || arxiu.getTipus() != VehicleArxiu.Tipus.FOTO) {

            return ResponseEntity.notFound().build();
        }

        MediaType tipus = MediaType.parseMediaType(arxiu.getContentType());

        return ResponseEntity.ok()
                .contentType(tipus)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(arxiu.getNom(), StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(arxiu.getDades());
    }
}
