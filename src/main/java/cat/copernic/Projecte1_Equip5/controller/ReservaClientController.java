package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de les pàgines de reserves del client (rutes /client/...). Només
 * rep les peticions i prepara el model; la lògica és a {@link ReservaService}.
 * El client és el que ha iniciat sessió a LoginController (atribut de sessió
 * "usuarioLogueado"); si no n'hi ha, es redirigeix al login.
 */
@Controller
@RequestMapping("/client")
public class ReservaClientController {

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private final ReservaService reservaService;

    public ReservaClientController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * DNI de l'usuari que ha iniciat sessió, o null si no n'hi ha. Quan es faci
     * el login definitiu (RF80), és l'únic lloc que caldrà canviar.
     */
    private String dniClientLogat(HttpSession session) {
        Object usuari = session.getAttribute("usuarioLogueado");
        if (usuari instanceof Usuari u) {
            return u.getDniUsuari();
        }
        return null;
    }

    /**
     * RF52: pàgina "Les meves reserves".
     */
    @GetMapping("/reserves")
    public String llistarReserves(HttpSession session, Model model) {
        String dni = dniClientLogat(session);
        if (dni == null) {
            return REDIRECT_LOGIN;
        }
        model.addAttribute("properes", reservaService.llistarProperes(dni));
        model.addAttribute("historial", reservaService.llistarHistorial(dni));
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
            HttpSession session, Model model) {

        if (dniClientLogat(session) == null) {
            return REDIRECT_LOGIN;
        }

        model.addAttribute("avui", LocalDate.now());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("categoria", categoria);
        model.addAttribute("categories", Enums.CategoriaVehicle.values());

        // Només es busca quan l'usuari ja ha enviat les dues dates
        if (dataInici != null && dataFi != null) {
            try {
                model.addAttribute("vehicles", reservaService.vehiclesDisponibles(dataInici, dataFi, categoria));
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
            HttpSession session, Model model, RedirectAttributes redirect) {

        String dni = dniClientLogat(session);
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
            BigDecimal fianca = reservaService.calcularFianca(client);

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
            @RequestParam(name = "dataInici") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(name = "dataFi") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            @RequestParam(name = "documentacio", defaultValue = "false") boolean documentacio,
            HttpSession session, RedirectAttributes redirect) {

        String dni = dniClientLogat(session);
        if (dni == null) {
            return REDIRECT_LOGIN;
        }
        try {
            if (!documentacio) {
                throw new IllegalArgumentException(
                        "Cal confirmar que entens que l'agent et pot demanar documentació en recollir el vehicle.");
            }
            Reserva reserva = reservaService.crearReserva(dni, matricula, dataInici, dataFi);
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
            HttpSession session, Model model, RedirectAttributes redirect) {

        String dni = dniClientLogat(session);
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
}
