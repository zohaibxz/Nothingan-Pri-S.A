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
 
/**
 * Controlador para subir la documentación (foto/PDF del DNI y del carnet) de un agente.
 * Los datos del agente están en MySQL (AgentRepository) y los ficheros se guardan en MongoDB
 * (DocumentacioRepository), enlazados por el DNI.
 */
@Controller
// Prefijo de todas las URLs de la clase. {dni} es una parte variable:
// /admin/agent/12345678A/documentacio -> dni = "12345678A"
@RequestMapping("/admin/agent/{dni}/documentacio")
public class DocumentacioController {
 
    // Tamaño máximo permitido por fichero: 5 MB (5 * 1024 * 1024 bytes).
    // La "L" indica que el número es long, para evitar desbordamientos al multiplicar
    private static final long MAX_BYTES = 5L * 1024 * 1024;
 
    // Tipos de fichero permitidos (tipo MIME): PDF, JPG y PNG
    private static final List<String> TIPUS = List.of(
            "application/pdf", "image/jpeg", "image/png");
 
    // Repositorio SQL (MySQL): para buscar al agente
    // @Autowired sobre el campo: Spring inyecta el objeto automáticamente
    // (la inyección por constructor, como en otros controladores, suele preferirse)
    @Autowired
    private AgentRepository agentRepository;
 
    // Repositorio de MongoDB: para guardar y buscar la documentación
    @Autowired
    private DocumentacioRepository documentacioRepository;
 
    // GET /admin/agent/{dni}/documentacio -> muestra el formulario de subida
    // (@GetMapping sin ruta usa la del @RequestMapping de la clase)
    @GetMapping
    public String formulari(@PathVariable("dni") String dni, Model model) {
        // Busca al agente por DNI. orElseThrow() sin mensaje lanza NoSuchElementException si no existe
        Agent agent = agentRepository.findById(dni).orElseThrow();
        // Datos para la plantilla: el DNI (para la URL del formulario) y el nombre completo para el título
        model.addAttribute("dni", dni);
        model.addAttribute("nomComplet", agent.getNom() + " " + agent.getCognoms());
        return "administrador/penjar-documentacio";
    }
 
    // POST /admin/agent/{dni}/documentacio -> recibe los ficheros y los guarda
    @PostMapping
    public String pujar(@PathVariable("dni") String dni,
                        // @RequestParam("identitat"): fichero del input con name="identitat" (documento de identidad)
                        @RequestParam("identitat") MultipartFile identitat,
                        // Fichero del input con name="carnet" (carnet de conducir)
                        @RequestParam("carnet") MultipartFile carnet,
                        Model model) throws IOException {
        // throws IOException: getBytes() puede lanzarla si falla la lectura del fichero
 
        // Se validan los dos ficheros. Se queda con el primer error que encuentre;
        // si identitat es válido (error == null), se valida carnet
        String error = validar(identitat);
        if (error == null) error = validar(carnet);
 
        // Si hay error, se vuelve a mostrar el formulario con el mensaje (sin guardar nada)
        if (error != null) {
            // Hay que volver a cargar los datos de la plantilla: el Model no se conserva entre peticiones
            Agent agent = agentRepository.findById(dni).orElseThrow();
            model.addAttribute("dni", dni);
            model.addAttribute("nomComplet", agent.getNom() + " " + agent.getCognoms());
            model.addAttribute("error", error);
            return "administrador/penjar-documentacio";
        }
 
        // Si ya existe documentación de este DNI se reutiliza (se actualiza);
        // si no, se crea un Documentacio nuevo. Así no se rompe el índice unique de dniUsuari
        Documentacio doc = documentacioRepository.findByDniUsuari(dni)
                .orElse(new Documentacio());
 
        // Se rellena el documento de Mongo con los datos de cada fichero:
        // nombre original, tipo (MIME) y contenido en bytes
        doc.setDniUsuari(dni);
        doc.setNomIdentitat(identitat.getOriginalFilename());
        doc.setTipusIdentitat(identitat.getContentType());
        doc.setIdentitat(identitat.getBytes());
        doc.setNomCarnet(carnet.getOriginalFilename());
        doc.setTipusCarnet(carnet.getContentType());
        doc.setCarnet(carnet.getBytes());
        // Cada vez que se sube documentación, vuelve a quedar pendiente de revisión
        doc.setEstat(Documentacio.Estat.PENDENT);
 
        // save: inserta si es nuevo o actualiza si ya existía (Mongo)
        documentacioRepository.save(doc);
 
        // Todo bien: redirect al dashboard (patrón Post/Redirect/Get, evita reenviar el formulario al recargar)
        return "redirect:/dashboard?rol=administrador";
    }
 
    /**
     * Valida un fichero subido. Devuelve el mensaje de error, o null si es correcto.
     */
    private String validar(MultipartFile f) {
        // ¿No se adjuntó nada?
        if (f.isEmpty()) return "Falta adjuntar un arxiu.";
        // ¿Supera los 5 MB?
        if (f.getSize() > MAX_BYTES) return "L'arxiu supera els 5 MB.";
        // ¿Su tipo no está en la lista permitida (PDF, JPG, PNG)?
        if (!TIPUS.contains(f.getContentType())) return "Format no permès (PDF, JPG o PNG).";
        // Todo correcto: null = sin error
        return null;
    }
}
 
