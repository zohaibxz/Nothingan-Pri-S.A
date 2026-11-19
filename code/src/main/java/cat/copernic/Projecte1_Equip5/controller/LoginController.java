package cat.copernic.Projecte1_Equip5.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la pantalla d'inici de sessió.
 */
@Controller
public class LoginController {

    /**
     * Crea el controlador del login.
     */
    public LoginController() {
    }

    /**
     * Mostra el login. Si l'usuari ja té sessió activa el porta directament al
     * dashboard.
     *
     * @param authentication usuari autenticat, si n'hi ha
     * @return plantilla de login o redirecció al dashboard
     */
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
