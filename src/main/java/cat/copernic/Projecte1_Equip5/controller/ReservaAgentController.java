package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agent")
public class ReservaAgentController {

    private final ReservaService reservaService;
    private final UsuariRepository usuariRepository;

    public ReservaAgentController(
            ReservaService reservaService,
            UsuariRepository usuariRepository) {
        this.reservaService = reservaService;
        this.usuariRepository = usuariRepository;
    }

    @GetMapping("/reserves")
    public String llistarReserves(Authentication authentication, Model model) {

        // Comprovar que hi ha un usuari autenticat
        if (authentication == null) {
            return "redirect:/login";
        }

        // Buscar l'usuari que ha iniciat sessió
        Optional<Usuari> usuariOpt
                = usuariRepository.findByEmail(authentication.getName());

        if (usuariOpt.isEmpty() || !(usuariOpt.get() instanceof Agent)) {
            return "redirect:/acces-denegat";
        }

        Usuari usuari = usuariOpt.get();

        // Dades de la capçalera
        model.addAttribute("nomAgent", usuari.getNom());
        model.addAttribute("inicials", calcularInicials(usuari));

        // Reserves actives i vehicles ja lliurats
        model.addAttribute("reservesActives",
                reservaService.llistarReservesAgent());

        // Comptadors
        model.addAttribute("pendentsEntregaCount",
                reservaService.comptarReservesActives());

        model.addAttribute("lliuradesCount",
                reservaService.comptarReservesLliurades());

        return "agent/agent-reserves";
    }

    @GetMapping("/reserves/{id}")
    public String veureDetallsReserva(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        if (authentication == null) {
            return "redirect:/login";
        }

        Optional<Usuari> usuariOpt
                = usuariRepository.findByEmail(authentication.getName());

        if (usuariOpt.isEmpty() || !(usuariOpt.get() instanceof Agent)) {
            return "redirect:/acces-denegat";
        }

        Usuari usuari = usuariOpt.get();

        try {
            Reserva reserva = reservaService.obtenirDetallsReservaAgent(id);

            model.addAttribute("reserva", reserva);
            model.addAttribute("nomAgent", usuari.getNom());
            model.addAttribute("inicials", calcularInicials(usuari));

            return "agent/agent-reserva-detalle";

        } catch (IllegalArgumentException e) {
            return "redirect:/agent/reserves";
        }
    }
    
    @PostMapping("/reserves/{id}/anular")
    public String anularReserva(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/login";
        }

        Optional<Usuari> usuariOpt
                = usuariRepository.findByEmail(authentication.getName());

        if (usuariOpt.isEmpty() || !(usuariOpt.get() instanceof Agent)) {
            return "redirect:/acces-denegat";
        }

        try {
            BigDecimal devolucio = reservaService.anullarReservaAgent(id);

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

        return inicials.isEmpty() ? "AG" : inicials.toUpperCase();
    }
}
