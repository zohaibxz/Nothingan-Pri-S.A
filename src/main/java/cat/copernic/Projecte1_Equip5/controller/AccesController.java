package cat.copernic.Projecte1_Equip5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccesController {

    /**
     * Muestra la página de acceso denegado.
     *
     * @return nombre de la plantilla acces-denegat
     */
    @GetMapping("/acces-denegat")
    public String accesDenegat() {
        return "acces-denegat";
    }
}
