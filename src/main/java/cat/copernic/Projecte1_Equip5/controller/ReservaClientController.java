package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.service.ReservaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de les pàgines de reserves del client (rutes /client/...).
 * Només rep les peticions i prepara el model per a la vista; la lògica de
 * negoci és a {@link ReservaService}.
 */
@Controller
@RequestMapping("/client")
public class ReservaClientController {

    /**
     * TEMPORAL: DNI d'un client de prova (ha d'existir a la base de dades).
     * Quan existeixi el login (RF80), s'ha de substituir pel client que ha
     * iniciat sessió.
     */
    private static final String DNI_CLIENT_PROVA = "12345678A";

    private final ReservaService reservaService;

    /**
     * @param reservaService servei de reserves (injectat per Spring)
     */
    public ReservaClientController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * Mostra la pàgina "Les meves reserves" (RF52).
     *
     * @param model model on es deixen les llistes que utilitza la vista
     * @return nom de la plantilla (templates/client/reserves.html)
     */
    @GetMapping("/reserves")
    public String llistarReserves(Model model) {
        model.addAttribute("properes", reservaService.llistarProperes(DNI_CLIENT_PROVA));
        model.addAttribute("historial", reservaService.llistarHistorial(DNI_CLIENT_PROVA));
        return "client/reserves";
    }
}
