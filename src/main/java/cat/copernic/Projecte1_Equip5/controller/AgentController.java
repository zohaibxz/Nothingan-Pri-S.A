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

    // 1. Mostrar el formulario de alta de agente
    @GetMapping("/alta")
    public String mostrarFormulariAlta(Model model) {
        Agent agent = new Agent();
        agent.setRol(Enums.Rol.Agent); // Rol por defecto

        model.addAttribute("agent", agent);
        model.addAttribute("roles", Enums.Rol.values());
        return "administrador/alta-agent";
    }

    // 2. Procesar el formulario de alta de agente
    @PostMapping("/alta")
    public String guardarAgent(@ModelAttribute("agent") Agent agent, Model model) {
        try {
            agentService.alta(agent);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Enums.Rol.values());
            return "administrador/alta-agent";
        }
        return "redirect:/admin/agent/" + agent.getDniUsuari() + "/documentacio";
    }

    // 3. Llistar agents per donar-los de baixa (amb cerca opcional per DNI)
    @GetMapping("/baixa")
    public String llistarAgents(@RequestParam(value = "dni", required = false) String dni,
                                Model model) {
        model.addAttribute("agents", agentService.llistarAgents(dni));
        model.addAttribute("filtreDni", dni);
        return "administrador/baixa-agent";
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
        return "redirect:/admin/agent/baixa";
    }

    // 5. Cua de validació de clients (Pantalla del Administrador)
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

    // 6. Aprovar la validació d'un client
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

    // 7. Rebutjar la validació d'un client
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

        // 1. Recuperamos todos los usuarios de la base de datos
        List<Usuari> totsElsUsuaris = usuariRepository.findAll();

        // 2. Filtramos únicamente las instancias de Client (Laia, Marc, Joan, etc.)
        List<Client> totsElsClients = totsElsUsuaris.stream()
                .filter(u -> u instanceof Client)
                .map(u -> (Client) u)
                .toList();

        model.addAttribute("clients", totsElsClients);

        // 3. Contadores dinámicos para las tarjetas KPI
        long validatsCount = totsElsClients.stream()
                .filter(c -> c.getEstatValidacio() != null && 
                             c.getEstatValidacio() == Enums.EstatValidacio.Validat)
                .count();

        long pendentsCount = totsElsClients.stream()
                .filter(c -> c.getEstatValidacio() == null || 
                             c.getEstatValidacio() == Enums.EstatValidacio.Pendent)
                .count();

        model.addAttribute("validatsCount", validatsCount);
        model.addAttribute("pendentsCount", pendentsCount);

        return "agent/agent-gestio clients";
    }


    // Mètode privat auxiliar per recuperar l'Agent/Admin autenticat de la sessió
    private Agent obtenirAgentAutenticat(Authentication authentication) {
        String email = authentication.getName();
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuari no trobat a la sessió."));
        if (!(usuari instanceof Agent)) {
            throw new IllegalStateException("L'usuari autenticat no té permisos d'agent o administrador.");
        }
        return (Agent) usuari;
    }

    // Mètode privat auxiliar per calcular les inicials de l'usuari
    private String calcularInicials(Usuari usuari) {
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();

        String inicials = "";
        if (!nom.isEmpty()) inicials += nom.charAt(0);
        if (!cognoms.isEmpty()) inicials += cognoms.charAt(0);

        return inicials.isEmpty() ? "AG" : inicials.toUpperCase();
    }
}
