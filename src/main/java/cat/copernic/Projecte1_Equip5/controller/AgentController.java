package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import cat.copernic.Projecte1_Equip5.service.AgentService;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/agent")
public class AgentController {

    private final AgentService agentService;
    private final ClientService clientService;
    private final UsuariRepository usuariRepository;
    private final ClientRepository clientRepository;

    public AgentController(AgentService agentService, 
                           ClientService clientService, 
                           UsuariRepository usuariRepository,
                           ClientRepository clientRepository) {
        this.agentService = agentService;
        this.clientService = clientService;
        this.usuariRepository = usuariRepository;
        this.clientRepository = clientRepository;
    }

    // 1. Mostrar el formulari d'alta d'agent
    @GetMapping("/alta")
    public String mostrarFormulariAlta(Model model) {
        Agent agent = new Agent();
        agent.setRol(Enums.Rol.Agent);

        model.addAttribute("agent", agent);
        model.addAttribute("roles", Enums.Rol.values());
        return "administrador/alta-agent";
    }

    // 2. Processar el formulari d'alta d'agent
    @PostMapping("/alta")
    public String guardarAgent(@ModelAttribute("agent") Agent agent, Model model) {
        try {
            agentService.alta(agent);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Enums.Rol.values());
            return "administrador/alta-agent";
        }
        return "redirect:/admin/agent/llista";
    }

    // 3. Llistar agents (URL: /admin/agent/llista)
    @GetMapping("/llista")
    public String llistaAgents(@RequestParam(value = "dni", required = false) String dni,
                               Authentication authentication,
                               Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            usuariRepository.findByEmail(authentication.getName()).ifPresent(usuari -> {
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            });
        }
        model.addAttribute("agents", agentService.llistarAgents(dni));
        model.addAttribute("totalAgents", agentService.llistarAgents(null).size());
        model.addAttribute("dni", dni);
        
        return "administrador/llista";
    }

    // 4. Donar de baixa un agent
    @PostMapping("/baixa/{dni}")
    public String donarDeBaixa(@PathVariable("dni") String dni, RedirectAttributes ra) {
        try {
            agentService.baixa(dni);
            ra.addFlashAttribute("missatge", "Agent " + dni + " donat de baixa correctament.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/llista";
    }

    // 5. Cua de validació de clients
    @GetMapping("/validacions")
    public String llistarUsuarisPendents(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);
            if (usuariOpt.isPresent()) {
                Usuari usuari = usuariOpt.get();
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            }
        }
        
        model.addAttribute("clientsPendents", clientService.llistarClientsPendents());
        return "administrador/validar-clients";
    }

    // 6. Validar un client
    @PostMapping("/validacions/{dni}/validar")
    public String validarClient(@PathVariable("dni") String dni, 
                                Authentication authentication, 
                                RedirectAttributes ra) {
        try {
            Agent adminValidador = obtenirAgentAutenticat(authentication);
            clientService.validarClient(dni, adminValidador);
            ra.addFlashAttribute("missatge", "El client amb DNI " + dni + " ha sigut validat amb èxit.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/validacions";
    }

    // 7. Rebutjar un client
    @PostMapping("/validacions/{dni}/rebutjar")
    public String rebutjarClient(@PathVariable("dni") String dni, 
                                 Authentication authentication, 
                                 RedirectAttributes ra) {
        try {
            Agent adminValidador = obtenirAgentAutenticat(authentication);
            clientService.rebutjarClient(dni, adminValidador);
            ra.addFlashAttribute("missatge", "El client amb DNI " + dni + " ha sigut rebutjat.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/validacions";
    }

    // 8. Gestió i consulta general de clients
    @GetMapping("/clients")
    public String gestioClients(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);
            if (usuariOpt.isPresent()) {
                Usuari usuari = usuariOpt.get();
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            }
        }

        List<Client> totsElsClients = clientRepository.findAll();

        List<Client> clientsReals = totsElsClients.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().endsWith("@empresa.com"))
                .toList();

        model.addAttribute("clients", clientsReals);

        long validatsCount = clientsReals.stream()
                .filter(c -> c.getEstatValidacio() != null && 
                             c.getEstatValidacio() == Enums.EstatValidacio.Validat)
                .count();

        long pendentsCount = clientsReals.stream()
                .filter(c -> c.getEstatValidacio() == null || 
                             c.getEstatValidacio() == Enums.EstatValidacio.Pendent)
                .count();

        model.addAttribute("validatsCount", validatsCount);
        model.addAttribute("pendentsCount", pendentsCount);

        return "administrador/llistar clients";
    }

    // 9. Veure detall d'un client específic
    @GetMapping("/clients/{dni}")
    public String veureDetallClient(@PathVariable("dni") String dni, Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            usuariRepository.findByEmail(authentication.getName()).ifPresent(usuari -> {
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            });
        }

        Client client = clientRepository.findById(dni)
                .orElseThrow(() -> new IllegalArgumentException("Client no trobat amb DNI: " + dni));

        model.addAttribute("client", client);
        return "administrador/detall-client";
    }

    // 10. Mostrar la pantalla d'informes
    @GetMapping("/informes")
    public String mostrarInformes(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);
            if (usuariOpt.isPresent()) {
                Usuari usuari = usuariOpt.get();
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            }
        }
        return "administrador/informes";
    }

    private Agent obtenirAgentAutenticat(Authentication authentication) {
        String email = authentication.getName();
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuari no trobat a la sessió."));
        if (!(usuari instanceof Agent)) {
            throw new IllegalStateException("L'usuari autenticat no té permisos d'agent o administrador.");
        }
        return (Agent) usuari;
    }

    private String calcularInicials(Usuari usuari) {
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();

        String inicials = "";
        if (!nom.isEmpty()) inicials += nom.charAt(0);
        if (!cognoms.isEmpty()) inicials += cognoms.charAt(0);

        return inicials.isEmpty() ? "AG" : inicials.toUpperCase();
    }
}