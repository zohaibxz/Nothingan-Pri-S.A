package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@Controller
@RequestMapping("/admin/agent/{dni}/documentacio")
public class DocumentacioController {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> TIPUS = List.of("application/pdf", "image/jpeg", "image/png");

    // ClientRepository encuentra clientes y también agentes (Agent extiende Client)
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentacioRepository documentacioRepository;

    // GET -> formulario de subida
    @GetMapping
    public String formulari(@PathVariable("dni") String dni,
                            Model model,
                            RedirectAttributes ra) { //Si el DNI no existe, redirige con un mensaje en vez de lanzar un error.
        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + "."); 
            return "redirect:/admin/agent/clients";
        }
        model.addAttribute("dni", dni);
        model.addAttribute("nomComplet", client.getNom() + " " + client.getCognoms());
        return "administrador/penjar-documentacio";
    }

    // POST -> recibe los ficheros y los guarda en Mongo
    @PostMapping
    public String pujar(@PathVariable("dni") String dni,
                        @RequestParam("identitat") MultipartFile identitat,
                        @RequestParam("carnet") MultipartFile carnet,
                        Model model,
                        RedirectAttributes ra) throws IOException {

        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + ".");
            return "redirect:/admin/agent/clients";
        }

        String error = validar(identitat);
        if (error == null) error = validar(carnet);

        // Si hay error, se vuelve a mostrar el formulario con el mensaje
        if (error != null) {
            model.addAttribute("dni", dni);
            model.addAttribute("nomComplet", client.getNom() + " " + client.getCognoms());
            model.addAttribute("error", error);
            return "administrador/penjar-documentacio";
        }

        // Si ya hay documentación de este DNI se actualiza; si no, se crea
        Documentacio doc = documentacioRepository.findByDniUsuari(dni)
                .orElse(new Documentacio());

        doc.setDniUsuari(dni);
        doc.setNomIdentitat(identitat.getOriginalFilename());
        doc.setTipusIdentitat(identitat.getContentType());
        doc.setIdentitat(identitat.getBytes());
        doc.setNomCarnet(carnet.getOriginalFilename());
        doc.setTipusCarnet(carnet.getContentType());
        doc.setCarnet(carnet.getBytes());
        doc.setEstat(Documentacio.Estat.PENDENT);

        documentacioRepository.save(doc);

        // Vuelve a la ficha del usuario con un mensaje de confirmación
        ra.addFlashAttribute("missatge", "La documentació s'ha pujat correctament.");
        return "redirect:/admin/agent/validacions/" + dni + "/revisa";
    }

    private String validar(MultipartFile f) {
        if (f.isEmpty()) return "Falta adjuntar un arxiu.";
        if (f.getSize() > MAX_BYTES) return "L'arxiu supera els 5 MB.";
        if (!TIPUS.contains(f.getContentType())) return "Format no permès (PDF, JPG o PNG).";
        return null;
    }
}