package cat.copernic.Projecte1_Equip5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Controlador de la pàgina d'inici pública.
 */
@Controller
public class IndexController {

    /**
     * Crea el controlador de la pàgina d'inici.
     */
    public IndexController() {
    }

    /**
     * Mostra l'inici. Qui ja té sessió va al seu dashboard i la resta veu
     * l'inici públic.
     *
     * @param authentication usuari autenticat, si n'hi ha
     * @return redirecció al dashboard o plantilla d'inici públic
     */
    @GetMapping({"/", "/index"})
    public String inici(Authentication authentication) {
        // Con sesión: cada rol va a su dashboard (cliente, agente o administrador)
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        // Sin sesión: el inicio público
        return "client/inici-client";
    }
}
