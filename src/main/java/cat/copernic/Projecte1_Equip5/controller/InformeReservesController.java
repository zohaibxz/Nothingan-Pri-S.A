package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import cat.copernic.Projecte1_Equip5.service.InformeReservesService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * RF91: informe de reserves per client, a partir de l'històric. L'accés
 * (administrador i agent) el controla WebSecurityConfig amb la ruta
 * /admin/agent/informes/**.
 */
@Controller
@RequestMapping("/admin/agent/informes/reserves")
public class InformeReservesController {

    private final InformeReservesService informeReservesService;

    public InformeReservesController(InformeReservesService informeReservesService) {
        this.informeReservesService = informeReservesService;
    }

    /**
     * Mostra l'informe. Sense paràmetres surten totes les reserves de tots els
     * clients. Els valors del filtre es tornen a la vista perquè no es buidi.
     *
     * @param dni DNI del client a filtrar (opcional)
     * @param senseAnullades si és true, no es mostren les reserves anul·lades
     * @param model model de la vista
     * @return plantilla de l'informe
     */
    @GetMapping
    public String veureInforme(
            @RequestParam(name = "dni", required = false) String dni,
            @RequestParam(name = "senseAnullades", defaultValue = "false") boolean senseAnullades,
            Model model) {

        model.addAttribute("dniSeleccionat", dni == null ? "" : dni);
        model.addAttribute("senseAnullades", senseAnullades);

        try {
            List<HistoricReserva> reserves
                    = informeReservesService.reservesPerClient(dni, senseAnullades);
            model.addAttribute("clients", informeReservesService.llistarClients());
            model.addAttribute("reserves", reserves);
            model.addAttribute("totalReserves", reserves.size());
            model.addAttribute("importTotal", informeReservesService.sumarImports(reserves));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("clients", Map.of());
            model.addAttribute("reserves", List.of());
            model.addAttribute("totalReserves", 0);
            model.addAttribute("importTotal", BigDecimal.ZERO);
        }

        return "administrador/informe-reserves";
    }
}
