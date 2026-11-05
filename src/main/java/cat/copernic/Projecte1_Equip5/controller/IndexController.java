package cat.copernic.Projecte1_Equip5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

@Controller
public class IndexController {

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
