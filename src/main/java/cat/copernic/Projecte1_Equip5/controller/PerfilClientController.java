package cat.copernic.Projecte1_Equip5.controller;
 
import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import cat.copernic.Projecte1_Equip5.service.UsuariService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
/**
 * Perfil del cliente: ver y modificar sus datos, cambiar la contraseña
 * y hacer el CRUD de sus documentos (DNI y carnet).
 *
 * Accesible para cualquier usuario autenticado (client, agent y administrador):
 * /perfil no tiene regla propia en WebSecurityConfig y cae en anyRequest().authenticated().
 *
 * REGLA CLAVE: el DNI nunca llega por la URL ni por el formulario. Siempre se
 * obtiene del usuario autenticado (auth.getName() = email del login), así un
 * cliente solo puede tocar sus propios datos y documentos.
 *
 * El controlador solo recibe la petición, llama al servicio y devuelve la vista;
 * las reglas de negocio están en ClientService, UsuariService y UsuariLogic.
 */
@Controller
@RequestMapping("/perfil") // prefijo de todas las rutas de esta clase
public class PerfilClientController {
    // REDIRECT_PERFIL para no confundir en los POST return client/perfil-client por return REDIRECT_PERFIL 
    private static final String REDIRECT_PERFIL = "redirect:/perfil";
    private final ClientService clientService;   // datos propios y documentos
    private final UsuariService usuariService;   // cambio de contraseña (común a todos los usuarios)
 
    // Inyección por constructor: Spring pasa las dos dependencias
    public PerfilClientController(ClientService clientService,
                                  UsuariService usuariService) {
        this.clientService = clientService;
        this.usuariService = usuariService;
    }
 
    // ------------------------------------------------------------------
    // GET /perfil -> muestra datos, formulario de contraseña y documentos
    // ------------------------------------------------------------------
    @GetMapping
    public String perfil(Authentication auth, Model model) {
        // El cliente se busca por el email del login, no por un DNI recibido
        Client client = clientService.obtenirClientPropi(auth.getName());
        // Documentación de Mongo: puede ser null si todavía no ha subido nada
        Documentacio doc = clientService.obtenirDocumentacioPropia(auth.getName());
 
        model.addAttribute("client", client);
        // La plantilla usa estos booleanos para mostrar "Veure/Substitueix/Elimina" o "Puja"
        model.addAttribute("hiHaIdentitat", doc != null && teBytes(doc.getIdentitat()));
        model.addAttribute("hiHaCarnet", doc != null && teBytes(doc.getCarnet()));
        model.addAttribute("nomIdentitat", doc != null ? doc.getNomIdentitat() : null);
        model.addAttribute("nomCarnet", doc != null ? doc.getNomCarnet() : null);
        return "perfil-client"; // templates/perfil-client.html
    }
 
    // ------------------------------------------------------------------
    // POST /perfil/dades -> modificar datos personales
    // El DNI y el correo no se reciben: no se pueden cambiar desde aquí.
    // ------------------------------------------------------------------
    @PostMapping("/dades")
    public String modificarDades(@RequestParam String nom,
                                 @RequestParam String cognoms,
                                 @RequestParam String telefon,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNaixement,
                                 @RequestParam String nacionalitat,
                                 @RequestParam String adreca,
                                 @RequestParam String tipusLlicencia,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatDni,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatLlicencia,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        try {
            // Reutiliza ClientService.actualitzarDades, con sus mismas validaciones
            clientService.actualitzarDadesPropies(auth.getName(), nom, cognoms, telefon, dataNaixement,
                    nacionalitat, adreca, tipusLlicencia, dataCaducitatDni, dataCaducitatLlicencia);
            ra.addFlashAttribute("missatge", "Les teves dades s'han desat correctament.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            // El servicio lanza estas excepciones con un mensaje legible para el usuario
            ra.addFlashAttribute("error", e.getMessage());
        }
        // Post/Redirect/Get: evita reenviar el formulario si recarga la página.
        // Los flash attributes (missatge/error) sobreviven a esta única redirección.
        return REDIRECT_PERFIL; // canviador por la url manual client/perfil-client... 
    }
 
    // ------------------------------------------------------------------
    // POST /perfil/contrasenya -> cambiar contraseña (pide la actual)
    // ------------------------------------------------------------------
    @PostMapping("/contrasenya")
    public String canviarContrasenya(@RequestParam String actual,
                                     @RequestParam String nova,
                                     @RequestParam String confirmacio,
                                     Authentication auth,
                                     RedirectAttributes ra) {
        try {
            // La comprobación de la actual, la longitud y el cifrado están en UsuariService
            usuariService.canviarContrasenya(auth.getName(), actual, nova, confirmacio);
            ra.addFlashAttribute("missatge", "La contrasenya s'ha canviat correctament.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_PERFIL;
    }
 
    // ------------------------------------------------------------------
    // READ: GET /perfil/document/identitat (o carnet) -> devuelve el fichero propio
    // ------------------------------------------------------------------
    @GetMapping("/document/{tipus}")
    public ResponseEntity<byte[]> veureDocument(@PathVariable("tipus") String tipus, Authentication auth) {
        Documentacio doc = clientService.obtenirDocumentacioPropia(auth.getName());
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        // DocumentResponse decide si se muestra en el navegador o se descarga
        // (la misma lógica que usa RevisaClientController)
        if ("identitat".equals(tipus)) {
            return DocumentResponse.de(doc.getIdentitat(), doc.getTipusIdentitat());
        }
        if ("carnet".equals(tipus)) {
            return DocumentResponse.de(doc.getCarnet(), doc.getTipusCarnet());
        }
        return ResponseEntity.notFound().build(); // cualquier otro valor de {tipus}
    }
 
    // ------------------------------------------------------------------
    // CREATE / UPDATE: POST /perfil/document/identitat (o carnet)
    // Sube un fichero nuevo o sustituye el que ya había.
    // ------------------------------------------------------------------
    @PostMapping("/document/{tipus}")
    public String pujarDocument(@PathVariable("tipus") String tipus,
                                @RequestParam("fitxer") MultipartFile fitxer,
                                Authentication auth,
                                RedirectAttributes ra) {
        try {
            // El servicio valida tamaño y tipo (UsuariLogic) y lo guarda en MongoDB
            clientService.desarDocumentPropi(auth.getName(), tipus, fitxer);
            ra.addFlashAttribute("missatge", "El document s'ha desat correctament.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_PERFIL;
    }
 
    // ------------------------------------------------------------------
    // DELETE: POST /perfil/document/identitat/eliminar (o carnet)
    // Se usa POST y no DELETE porque los formularios HTML solo envían GET o POST.
    // ------------------------------------------------------------------
    @PostMapping("/document/{tipus}/eliminar")
    public String eliminarDocument(@PathVariable("tipus") String tipus,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        try {
            clientService.eliminarDocumentPropi(auth.getName(), tipus);
            ra.addFlashAttribute("missatge", "El document s'ha eliminat.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_PERFIL;
    }
 
    // Auxiliar: ¿el fichero guardado tiene contenido?
    private boolean teBytes(byte[] b) {
        return b != null && b.length > 0;
    }
}
 