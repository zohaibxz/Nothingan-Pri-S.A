package cat.copernic.Projecte1_Equip5.controller;

// En tu ClientController

import cat.copernic.Projecte1_Equip5.entity.Client;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientController {

    @GetMapping("/client/alta")
    public String formulariAlta(Model model) {
        model.addAttribute("client", new Client());
        return "client/alta";   // templates/client/alta-client.html
    }
    
    @GetMapping("/client/inici")
    public String iniciClient(Model model) {
        model.addAttribute("nomClient", "Marta");   // luego, el nombre del usuario logueado
        model.addAttribute("inicials", "MR");
        return "client/inici-client";
    }
}