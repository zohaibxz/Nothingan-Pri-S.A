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

        return switch (rol.toLowerCase()) {
            case "administrador" -> "administrador/dashboard";
            case "agent" -> "agent/dashboard";
            default -> "client/inici-client";
        };
    }
}

