package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashBoardController {

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false, defaultValue = "client") String rol,
                            HttpSession session,
                            Model model) {

        Usuari usuario = (Usuari) session.getAttribute("usuarioLogueado");

        if (usuario instanceof Client client) {
            model.addAttribute("client", client);
        } else {
            Client dummy = new Client();
            dummy.setNom(usuario != null ? usuario.getNom() : "Usuari");
            dummy.setEmail(usuario != null ? usuario.getEmail() : "email@empresa.com");
            model.addAttribute("client", dummy);
        }
        
        if (usuario != null) {
        model.addAttribute("nomClient", usuario.getNom());
        model.addAttribute("inicials", calcularInicials(usuario));
        }
        return switch (rol.toLowerCase()) {
            case "administrador" -> "administrador/dashboard";
            case "agent" -> "agent/dashboard";
            default -> "client/inici-client";
        };
        
    }
    
    // Aquí va el método nuevo, ya fuera de dashboard()
    private String calcularInicials(Usuari usuari) {
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();

        String inicials = "";
        if (!nom.isEmpty()) inicials += nom.charAt(0);
        if (!cognoms.isEmpty()) inicials += cognoms.charAt(0);

        return inicials.isEmpty() ? "CL" : inicials.toUpperCase();
    }
    
}

