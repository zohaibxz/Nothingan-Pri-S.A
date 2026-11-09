package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.service.ReservaArxiuService;
import cat.copernic.Projecte1_Equip5.service.ReservaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agent")
public class ReservaAgentController {

    private final ReservaService reservaService;
    private final ReservaArxiuService reservaArxiuService;

    public ReservaAgentController(ReservaService reservaService,
            ReservaArxiuService reservaArxiuService) {
        this.reservaService = reservaService;
        this.reservaArxiuService = reservaArxiuService;
    }

    @GetMapping("/reserves")
    public String llistarReserves(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String estat,
            Model model) {

        // Convertim l'estat seleccionat a l'enum corresponent
        Enums.EstatReserva estatSeleccionat = null;

        if (estat != null && !estat.isBlank()) {
            try {
                estatSeleccionat = Enums.EstatReserva.valueOf(estat);
            } catch (IllegalArgumentException e) {
                estatSeleccionat = null;
            }
        }

        // Llistat amb tots els filtres
        model.addAttribute("reservesActives",
                reservaService.llistarReservesAgentFiltrades(
                        dni, email, data, matricula, codi, estatSeleccionat));

        // Comptadors del tauler
        model.addAttribute("pendentsEntregaCount",
                reservaService.comptarReservesActives());

        model.addAttribute("lliuradesCount",
                reservaService.comptarReservesLliurades());

        // Conservem els valors del formulari després de filtrar
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("data", data);
        model.addAttribute("matricula", matricula);
        model.addAttribute("codi", codi);
        model.addAttribute("estatSeleccionat", estat);

        return "agent/agent-reserves";
    }

    @GetMapping("/reserves/{id}")
    public String veureDetallsReserva(
            @PathVariable Long id,
            Model model) {

        try {
            Reserva reserva = reservaService.obtenirDetallsReservaAgent(id);

            model.addAttribute("reserva", reserva);
            // null si es pot lliurar; si no, el motiu que es mostra en lloc del formulari
            model.addAttribute("motiuNoLliurable", reservaService.motiuNoLliurable(reserva));
            // Hora actual (sense segons) com a valor per defecte del formulari de lliurament
            model.addAttribute("araLocal",
                    LocalDateTime.now().withSecond(0).withNano(0).toString());
            model.addAttribute("fotosLliurament", reservaArxiuService.llistarFotos(
                    id, ReservaArxiu.Moment.LLIURAMENT));

            return "agent/agent-reserva-detalle";

        } catch (IllegalArgumentException e) {
            return "redirect:/agent/reserves";
        }
    }

    @PostMapping("/reserves/{id}/anular")
    public String anularReserva(
            @PathVariable Long id,
            @RequestParam String motiuAnulacio,
            @RequestParam BigDecimal percentatgeDevolucio,
            RedirectAttributes redirectAttributes) {

        try {
            BigDecimal devolucio = reservaService.anullarReservaAgent(
                    id, motiuAnulacio, percentatgeDevolucio);

            redirectAttributes.addFlashAttribute(
                    "missatgeExit",
                    "Reserva anul·lada correctament. Import retornat: "
                    + devolucio + " €");

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "missatgeError", e.getMessage());
        }

        return "redirect:/agent/reserves/" + id;
    }

    /**
     * RF49: lliura el vehicle d'una reserva (data i hora, descripció de l'estat
     * i fotos opcionals). Els camps no són required perquè el servei els valida
     * i torna un missatge en català en lloc d'una pàgina d'error.
     */
    @PostMapping("/reserves/{id}/lliurar")
    public String lliurarVehicle(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraLliurament,
            @RequestParam(required = false) String descripcioEstat,
            @RequestParam(name = "fotos", required = false) List<MultipartFile> fotos,
            RedirectAttributes redirectAttributes) {

        try {
            reservaService.lliurarVehicle(id, dataHoraLliurament, descripcioEstat, fotos);
            redirectAttributes.addFlashAttribute("missatgeExit",
                    "Vehicle lliurat correctament. La reserva ha passat a Lliurada.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("missatgeError", e.getMessage());
        }

        return "redirect:/agent/reserves/" + id;
    }

    /**
     * Mostra una foto d'una reserva (només personal: la ruta és de /agent/**).
     * Torna 404 si la foto no existeix o no és d'aquesta reserva.
     */
    @GetMapping("/reserves/{id}/fotos/{idArxiu}")
    public ResponseEntity<byte[]> veureFoto(
            @PathVariable Long id,
            @PathVariable String idArxiu) {

        try {
            ReservaArxiu foto = reservaArxiuService.obtenirFoto(id, idArxiu);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(foto.getContentType()))
                    .body(foto.getDades());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
