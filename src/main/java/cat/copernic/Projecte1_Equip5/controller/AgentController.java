package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.service.AgentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
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
}
