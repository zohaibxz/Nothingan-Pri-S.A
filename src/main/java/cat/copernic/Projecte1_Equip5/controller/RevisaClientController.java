package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantalla "Revisa" de la cola de validación. La usan administradores y agentes:
 *  - Los dos ven los datos del cliente y sus documentos (DNI y carnet, guardados en MongoDB)
 *    y pueden modificar los datos.
 *  - Solo el administrador ve los botones de validar / rebutjar (lo decide la plantilla con rolUsuari).
 *
 * La validación y el rechazo siguen haciéndose con los métodos que ya existen en AgentController:
 * POST /admin/agent/validacions/{dni}/validar y POST /admin/agent/validacions/{dni}/rebutjar.
 *
 * El nombre, las iniciales y el rol de la cabecera (nomUsuari, inicialsUsuari, rolUsuari)
 * los añade NomUsuaris (@ControllerAdvice) a todas las vistas.
 */
@Controller
@RequestMapping("/admin/agent/validacions")
public class RevisaClientController {

    // Tipos de archivo que se muestran en el navegador; cualquier otro se descarga
    private static final Set<String> TIPUS_VISIBLES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private final ClientRepository clientRepository;
    private final DocumentacioRepository documentacioRepository;
    private final ClientService clientService;

    public RevisaClientController(ClientRepository clientRepository,
                                  DocumentacioRepository documentacioRepository,
                                  ClientService clientService) {
        this.clientRepository = clientRepository;
        this.documentacioRepository = documentacioRepository;
        this.clientService = clientService;
    }

    // GET /admin/agent/validacions/{dni}/revisa -> ficha del cliente + documentos
    @GetMapping("/{dni}/revisa")
    public String revisarClient(@PathVariable("dni") String dni,
                                @RequestParam(value = "origen", required = false) String origen, // añadido para el tema de html que cuando vaya de un sitio a otro que sepa de dodne viene cuando se comparte la misma plantilla para otros tipo de html agent o admin
                                Model model,
                                Authentication authentication,   // <- nuevo para tema de seguridad 
                                RedirectAttributes ra) {

        Optional<Client> clientOpt = clientRepository.findById(dni);
        if (clientOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No s'ha trobat cap client amb DNI " + dni + ".");
            return "redirect:/admin/agent/validacions";
        }
        Client client = clientOpt.get();
        
        // <- nuevo para ver 
        if (!potVeure(authentication, client)) { 
            ra.addFlashAttribute("error", "No tens permís per veure aquest usuari.");
            return "redirect:/admin/agent/validacions";
        }

        // Documentos de Mongo (puede no haberlos si el cliente se registró antes de guardarse)
        Documentacio doc = documentacioRepository.findByDniUsuari(dni).orElse(null);

        // Tarjeta enmascarada: el revisor no necesita ver el número completo
        String targeta = client.getNumTargetaCredit();
        String targetaMascara = (targeta != null && targeta.length() >= 4)
                ? "•••• •••• •••• " + targeta.substring(targeta.length() - 4)
                : "—";

        model.addAttribute("client", client);
        model.addAttribute("pendent", client.getEstatValidacio() == Enums.EstatValidacio.Pendent);
        model.addAttribute("targetaMascara", targetaMascara);
        model.addAttribute("hiHaDocumentacio", doc != null);
        model.addAttribute("tipusIdentitat", doc != null ? doc.getTipusIdentitat() : null);
        model.addAttribute("tipusCarnet", doc != null ? doc.getTipusCarnet() : null);
        model.addAttribute("nomIdentitat", doc != null ? doc.getNomIdentitat() : null);
        model.addAttribute("nomCarnet", doc != null ? doc.getNomCarnet() : null);
        // para solucionar problema de que si entro en detalls cliente desde llista.html (llista agents) que luego al vovler no me lleve a cua de validacions clients per validar
        // es para añadir en el html de revisar-documentacio-html que sea si agent o client o admin 
        model.addAttribute("esAgent", client instanceof Agent);
       // rol real del usuario para mostrarlo en la ficha: Agent / Administrador, o Client si es un cliente normal 
        model.addAttribute("rolClient", client instanceof Agent a ? a.getRol().name() : "Client");
       // añadido para controlar en los html de donde viene .. etc 
        model.addAttribute("origen", origen);
        return "administrador/revisar-documentacio-client";
    }

    // POST /admin/agent/validacions/{dni}/modifica -> guarda los datos editados y vuelve a la ficha
    @PostMapping("/{dni}/modifica")
    public String modificarClient(@PathVariable("dni") String dni,
                                  @RequestParam(value = "origen", required = false) String origen,
                                  @RequestParam String nom,
                                  @RequestParam String cognoms,
                                  @RequestParam String email,
                                  @RequestParam String telefon,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNaixement,
                                  @RequestParam String nacionalitat,
                                  @RequestParam String adreca,
                                  @RequestParam String tipusLlicencia,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatDni,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatLlicencia,
                                  @RequestParam(value = "premium", required = false) Boolean premium,
                                  Authentication authentication, // añadido para el tema de moficacion que auntentifique quien es 
                                  RedirectAttributes ra) {
        try {
            clientService.actualitzarDades(authentication.getName(), dni, nom, cognoms, email, telefon, dataNaixement,
                    nacionalitat, adreca, tipusLlicencia, dataCaducitatDni, dataCaducitatLlicencia);
            if (premium != null) {
                clientService.canviarPremium(authentication.getName(), dni, premium);
            }        
            ra.addFlashAttribute("missatge", "Les dades del client s'han actualitzat correctament.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/validacions/" + dni 
                + "/revisa" + (origen != null ? "?origen=" + origen : "");
    }

    // GET /admin/agent/validacions/{dni}/document/identitat (o /carnet) -> devuelve el archivo
    @GetMapping("/{dni}/document/{tipus}")
    public ResponseEntity<byte[]> veureDocument(@PathVariable("dni") String dni,
                                                @PathVariable("tipus") String tipus,
                                                Authentication authentication) {   // <- nuevo) {

        // <- nuevo para authenticaction para confirmar que pueda ver documento o no porque 
        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null || !potVeure(authentication, client)) {
            return ResponseEntity.notFound().build();
        }
        Documentacio doc = documentacioRepository.findByDniUsuari(dni).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes;
        String contentType;
        if ("identitat".equals(tipus)) {
            bytes = doc.getIdentitat();
            contentType = doc.getTipusIdentitat();
        } else if ("carnet".equals(tipus)) {
            bytes = doc.getCarnet();
            contentType = doc.getTipusCarnet();
        } else {
            return ResponseEntity.notFound().build();
        }
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

         // DocumentResponse comprueba que haya contenido y decide si se muestra o se descarga
        return DocumentResponse.de(bytes, contentType);
    }
    
    /* esto es solo para evitar este fallo --> Ahora los agentes llegan a RevisaClientController. 
    Sus métodos GET (revisarClient y veureDocument) cargan lo que haya con ese DNI, y como Agent extiende Client,
    también encuentran a agentes y administradores. Un agente podría escribir en la URL el DNI de un administrador y
    ver sus datos y sus documentos, lo que contradice tu regla.
     */
    private boolean potVeure(Authentication authentication, Client client) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        return esAdmin || AuthorityType.of(client) == AuthorityType.CLIENT;
   }
}
