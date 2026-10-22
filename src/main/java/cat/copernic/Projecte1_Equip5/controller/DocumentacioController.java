package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.repository.AgentRepository;
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

@Controller
@RequestMapping("/admin/agent/{dni}/documentacio")
public class DocumentacioController {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> TIPUS = List.of(
            "application/pdf", "image/jpeg", "image/png");

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private DocumentacioRepository documentacioRepository;

    @GetMapping
    public String formulari(@PathVariable("dni") String dni, Model model) {
        Agent agent = agentRepository.findById(dni).orElseThrow();
        model.addAttribute("dni", dni);
        model.addAttribute("nomComplet", agent.getNom() + " " + agent.getCognoms());
        return "administrador/penjar-documentacio";
    }

    @PostMapping
    public String pujar(@PathVariable("dni") String dni,
                        @RequestParam("identitat") MultipartFile identitat,
                        @RequestParam("carnet") MultipartFile carnet,
                        Model model) throws IOException {

        String error = validar(identitat);
        if (error == null) error = validar(carnet);

        if (error != null) {
            Agent agent = agentRepository.findById(dni).orElseThrow();
            model.addAttribute("dni", dni);
            model.addAttribute("nomComplet", agent.getNom() + " " + agent.getCognoms());
            model.addAttribute("error", error);
            return "administrador/penjar-documentacio";
        }

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

        return "redirect:/dashboard?rol=administrador";
    }

    private String validar(MultipartFile f) {
        if (f.isEmpty()) return "Falta adjuntar un arxiu.";
        if (f.getSize() > MAX_BYTES) return "L'arxiu supera els 5 MB.";
        if (!TIPUS.contains(f.getContentType())) return "Format no permès (PDF, JPG o PNG).";
        return null;
    }
}
