package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
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

@Controller
@RequestMapping("/agent")
public class ReservaAgentController {

    private final ReservaService reservaService;

    public ReservaAgentController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping("/reserves")
    public String llistarReserves(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String estat,
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
        model.addAttribute("reservesActives",
                reservaService.llistarReservesAgentFiltrades(
                        dni, email, data, matricula, codi, estatSeleccionat));

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

    @GetMapping("/reserves/{id}")
    public String veureDetallsReserva(
            @PathVariable Long id,
            Model model) {

        try {
            Reserva reserva = reservaService.obtenirDetallsReservaAgent(id);

            model.addAttribute("reserva", reserva);

            return "agent/agent-reserva-detalle";

        } catch (IllegalArgumentException e) {
            return "redirect:/agent/reserves";
        }
    }

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
}
