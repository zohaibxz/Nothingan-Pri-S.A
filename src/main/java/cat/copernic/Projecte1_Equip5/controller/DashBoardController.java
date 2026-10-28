package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class DashBoardController {

    private final UsuariRepository usuariRepository;

    public DashBoardController(UsuariRepository usuariRepository) {
        this.usuariRepository = usuariRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Obtener el correo del usuario autenticado en Spring Security
        String email = authentication.getName();
        Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);

        String nomMostrar = "Usuari"; // Nombre por defecto estricto

        if (usuariOpt.isPresent()) {
            Usuari usuari = usuariOpt.get();
            
            // Si el campo 'nom' del registro existe en la BD, usamos ese valor exacto
            if (usuari.getNom() != null && !usuari.getNom().trim().isEmpty()) {
                nomMostrar = usuari.getNom();
            }

            model.addAttribute("inicials", calcularInicials(usuari));

            if (usuari instanceof Client client) {
                model.addAttribute("client", client);
            }
        } else {
            model.addAttribute("inicials", "US");
        }

        model.addAttribute("nomClient", nomMostrar);

        // Control de redirección según el rol de Spring Security
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRADOR") 
                            || a.getAuthority().equalsIgnoreCase("ADMINISTRADOR"));
                            
        boolean isAgent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_AGENT") 
                            || a.getAuthority().equalsIgnoreCase("AGENT"));

        if (isAdmin) {
            return "administrador/dashboard";
        } else if (isAgent) {
            return "agent/dashboard";
        } else {
            return "client/inici-client";
        }
    }

    private String calcularInicials(Usuari usuari) {
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();

        String inicials = "";
        if (!nom.isEmpty()) inicials += nom.charAt(0);
        if (!cognoms.isEmpty()) inicials += cognoms.charAt(0);

        return inicials.isEmpty() ? "US" : inicials.toUpperCase();
    }
}

