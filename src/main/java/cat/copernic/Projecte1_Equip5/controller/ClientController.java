package cat.copernic.Projecte1_Equip5.controller;

// En tu ClientController

import cat.copernic.Projecte1_Equip5.service.ClientService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }
    
    /* esto al principio para que carga el html de registro con la ruta /client/alta*/
    @GetMapping("/client/alta")
    public String formulariAlta() { // el nombre del meotod que se encarga de devolvere el alta 
        return "client/alta";
    }

    @PostMapping("/client/alta")
    public String registrarClient(@RequestParam String nom,
                                  @RequestParam String cognom,
                                  @RequestParam String dniUsuari,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  @RequestParam String adreca,
                                  @RequestParam String ciutat,
                                  @RequestParam String codiPostal,
                                  @RequestParam String nacionalitat,
                                  @RequestParam String tipusLlicencia,
                                  @RequestParam String targetaCredit,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatDocument,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatLlicencia,
                                  @RequestParam("imatgeIdentificacio") MultipartFile imatgeDocument,
                                  @RequestParam("imatgeLlicencia") MultipartFile imatgeLlicencia,
                                  Model model) {    
        try {
            // Client no tiene ciudad ni código postal:z de momento van dentro de la dirección
            String adrecaCompleta = adreca + ", " + codiPostal + " " + ciutat;

            clientService.registrar(dniUsuari, email, password, nom, cognom, dataCaducitatDocument,
                    tipusLlicencia, dataCaducitatLlicencia, targetaCredit, adrecaCompleta,
                    nacionalitat, imatgeDocument, imatgeLlicencia);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "client/alta";
        }
        return "redirect:/login";
    }

   
}