package cat.copernic.Projecte1_Equip5.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String mostrarLogin(Authentication authentication) {
        // Si el usuario ya está autenticado (y no es un usuario anónimo)
        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getPrincipal().equals("anonymousUser")) {
            // Redirige directamente al dashboard si ya tiene sesión activa
            return "redirect:/dashboard";
        }
        
        // Si no tiene sesión, muestra la vista de login habitualmente
        return "login";
    }
}