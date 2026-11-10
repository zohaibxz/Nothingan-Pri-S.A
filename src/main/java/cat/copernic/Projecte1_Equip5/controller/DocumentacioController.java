package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.service.DocumentacioService;
import org.springframework.security.core.Authentication;
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
 * CRUD de la documentación (DNI y carnet) de un cliente por parte del personal:
 * subir o sustituir uno o los dos documentos y eliminar uno de ellos. Consultar y listar
 * se hace en la ficha "Revisa" (RevisaClientController).
 *
 * En el diagrama de casos de uso "CRUD documentació de client" es un {@code <<extend>>}
 * de "CRUD Clients", así que lo pueden usar administradores y agentes
 * (lo permite /admin/agent/*&#47;documentacio/** en WebSecurityConfig).
 * La misma regla que ya se aplica para ver y modificar datos:
 *  - el administrador puede gestionar la documentación de cualquier usuario;
 *  - el agente solo la de clientes normales, no la de otros agentes ni administradores.
 * Esta segunda parte no se puede dejar solo en la URL, por eso se comprueba aquí ({@link #potGestionar}).
 *
 * Los datos del usuario están en MySQL y los ficheros en MongoDB, enlazados por el DNI.
 * La validación y el guardado están en {@link DocumentacioService}.
 */
@Controller
@RequestMapping("/admin/agent/{dni}/documentacio")
public class DocumentacioController {

    private static final String VISTA_FORMULARI = "administrador/penjar-documentacio";
    private static final String REDIRECT_CLIENTS = "redirect:/admin/agent/clients";
    private static final String MISSATGE_SENSE_PERMIS =
            "No tens permís per gestionar la documentació d'aquest usuari.";

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
                            @RequestParam(value = "origen", required = false) String origen,
                            Model model,
                            Authentication authentication,
                            RedirectAttributes ra) {
        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + ".");
            return REDIRECT_CLIENTS;
        }
        if (!potGestionar(authentication, client)) {
            ra.addFlashAttribute("error", MISSATGE_SENSE_PERMIS);
            return REDIRECT_CLIENTS;
        }
        ompleFormulari(model, dni, client, origen);
        return VISTA_FORMULARI;
    }

    // POST -> recibe uno o los dos ficheros y los guarda en Mongo (crea o sustituye)
    @PostMapping
    public String pujar(@PathVariable("dni") String dni,
                        @RequestParam(value = "identitat", required = false) MultipartFile identitat,
                        @RequestParam(value = "carnet", required = false) MultipartFile carnet,
                        @RequestParam(value = "origen", required = false) String origen,
                        Model model,
                        Authentication authentication,
                        RedirectAttributes ra) {

        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + ".");
            return REDIRECT_CLIENTS;
        }
        if (!potGestionar(authentication, client)) {
            ra.addFlashAttribute("error", MISSATGE_SENSE_PERMIS);
            return REDIRECT_CLIENTS;
        }

        try {
            // Cada documento es opcional (hace falta al menos uno): solo se sustituye el que se sube,
            // el otro queda como estaba. Si no hay documentación de este DNI, se crea.
            documentacioService.guardarDocumentsOpcionals(dni, identitat, carnet);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Si hay error, se vuelve a mostrar el formulario con el mensaje
            ompleFormulari(model, dni, client, origen);
            model.addAttribute("error", e.getMessage());
            return VISTA_FORMULARI;
        }

        // Vuelve a la ficha del usuario con un mensaje de confirmación
        ra.addFlashAttribute("missatge", "La documentació s'ha pujat correctament.");
        return tornarAFitxa(dni, origen);
    }

    // POST -> elimina UN documento (tipus = "identitat" o "carnet"); el otro queda como estaba
    @PostMapping("/{tipus}/elimina")
    public String eliminar(@PathVariable("dni") String dni,
                           @PathVariable("tipus") String tipus,
                           @RequestParam(value = "origen", required = false) String origen,
                           Authentication authentication,
                           RedirectAttributes ra) {

        Client client = clientRepository.findById(dni).orElse(null);
        if (client == null) {
            ra.addFlashAttribute("error", "No s'ha trobat cap usuari amb DNI " + dni + ".");
            return REDIRECT_CLIENTS;
        }
        if (!potGestionar(authentication, client)) {
            ra.addFlashAttribute("error", MISSATGE_SENSE_PERMIS);
            return REDIRECT_CLIENTS;
        }

        try {
            // El servicio valida el tipo y, si no queda ningún documento, borra la ficha entera de Mongo
            documentacioService.eliminarDocument(dni, tipus);
            ra.addFlashAttribute("missatge", "El document s'ha eliminat correctament.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return tornarAFitxa(dni, origen);
    }

    /*
     * Mismo criterio que RevisaClientController.potVeure y ClientService.comprovarPotModificar:
     * el administrador gestiona a cualquiera y el agente solo a clientes normales.
     * Sin esto, un agente podría escribir en la URL el DNI de un administrador.
     */
    private boolean potGestionar(Authentication authentication, Client client) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        return esAdmin || AuthorityType.of(client) == AuthorityType.CLIENT;
    }

    // "origen" solo sirve para que la ficha sepa a dónde volver; se acepta un único valor conocido
    private String origenNet(String origen) {
        return "clients".equals(origen) ? "clients" : null;
    }

    private String tornarAFitxa(String dni, String origen) {
        return "redirect:/admin/agent/validacions/" + dni + "/revisa"
                + (origenNet(origen) != null ? "?origen=clients" : "");
    }

    private void ompleFormulari(Model model, String dni, Client client, String origen) {
        model.addAttribute("dni", dni);
        model.addAttribute("nomComplet", client.getNom() + " " + client.getCognoms());
        model.addAttribute("origen", origenNet(origen));
    }
}
