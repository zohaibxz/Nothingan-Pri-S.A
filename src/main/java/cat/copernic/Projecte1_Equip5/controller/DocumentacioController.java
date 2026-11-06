package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.service.DocumentacioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
// Si el DNI no existe, redirige con un mensaje en vez de lanzar un error.
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Subida de documentación (DNI y carnet) de un cliente, agente o administrador.
 * Solo administradores (lo exige /admin/agent/** en WebSecurityConfig).
 * Los datos del usuario están en MySQL y los ficheros en MongoDB, enlazados por el DNI.
 * La validación y el guardado están en {@link DocumentacioService}.
 */
@Controller
@RequestMapping("/admin/agent/{dni}/documentacio")
public class DocumentacioController {

    private static final String VISTA_FORMULARI = "administrador/penjar-documentacio";
    private static final String REDIRECT_CLIENTS = "redirect:/admin/agent/clients";

    // ClientRepository encuentra clientes y también agentes (Agent extiende Client)
    private final ClientRepository clientRepository;
    private final DocumentacioService documentacioService;

    public DocumentacioController(ClientRepository clientRepository,
            DocumentacioService documentacioService) {
        this.clientRepository = clientRepository;
        this.documentacioService = documentacioService;
    }

    // GET -> formulario de subida
    @GetMapping
    public String formulari(@PathVariable("dni") String dni,
                            Model model,
                            RedirectAttributes ra) {
        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + ".");
            return REDIRECT_CLIENTS;
        }
        ompleFormulari(model, dni, client);
        return VISTA_FORMULARI;
    }

    // POST -> recibe los ficheros y los guarda en Mongo
    @PostMapping
    public String pujar(@PathVariable("dni") String dni,
                        @RequestParam("identitat") MultipartFile identitat,
                        @RequestParam("carnet") MultipartFile carnet,
                        Model model,
                        RedirectAttributes ra) {

        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + ".");
            return REDIRECT_CLIENTS;
        }

        try {
            // Valida y guarda (si ya había documentación de este DNI, la sustituye)
            documentacioService.guardarDocumentacio(dni, identitat, carnet);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Si hay error, se vuelve a mostrar el formulario con el mensaje
            ompleFormulari(model, dni, client);
            model.addAttribute("error", e.getMessage());
            return VISTA_FORMULARI;
        }

        // Vuelve a la ficha del usuario con un mensaje de confirmación
        ra.addFlashAttribute("missatge", "La documentació s'ha pujat correctament.");
        return "redirect:/admin/agent/validacions/" + dni + "/revisa";
    }

    private void ompleFormulari(Model model, String dni, Client client) {
        model.addAttribute("dni", dni);
        model.addAttribute("nomComplet", client.getNom() + " " + client.getCognoms());
    }
}
 