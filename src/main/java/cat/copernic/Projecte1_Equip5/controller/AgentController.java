package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.AgentRepository;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/agent")
public class AgentController {

    @Autowired
    private AgentRepository agentRepository;

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
    public String guardarAgent(@ModelAttribute("agent") Agent agent) {
        // Asignar campos automáticos obligatorios
        agent.setDataCreacio(LocalDateTime.now());
        agent.setEstatValidacio(Enums.EstatValidacio.Validat); // Los agentes nacen validados
        agent.setPremium(false);

        // Guardar en la base de datos (se inserta en usuaris, clients y agents)
        agentRepository.save(agent);

        // Redirigir al dashboard de administrador tras guardar
        return "redirect:/admin/agent/" + agent.getDniUsuari() + "/documentacio";
    }
    @Autowired
    private DocumentacioRepository documentacioRepository;
    // 3. Llistar agents per donar-los de baixa (amb cerca opcional per DNI)
    @GetMapping("/baixa")
    public String llistarAgents(@RequestParam(value = "dni", required = false) String dni,
                                Model model) {
        List<Agent> agents = (dni == null || dni.isBlank())
                ? agentRepository.findByRol(Enums.Rol.Agent)
                : agentRepository.findByRolAndDniUsuariContainingIgnoreCase(Enums.Rol.Agent, dni.trim());

        model.addAttribute("agents", agents);
        model.addAttribute("filtreDni", dni);
        return "administrador/baixa-agent";
    }

    // 4. Donar de baixa un agent
    @PostMapping("/baixa/{dni}")
    public String donarDeBaixa(@PathVariable("dni") String dni, RedirectAttributes ra) {

        Agent agent = agentRepository.findById(dni).orElse(null);

        // Només es poden donar de baixa agents amb rol Agent (mai administradors)
        if (agent == null || agent.getRol() != Enums.Rol.Agent) {
            ra.addFlashAttribute("error", "No s'ha trobat cap agent amb aquest DNI.");
            return "redirect:/admin/agent/baixa";
        }

        try {
            agentRepository.deleteById(dni);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "No es pot donar de baixa l'agent " + dni
                    + ": té dades associades (clients validats o altres registres).");
            return "redirect:/admin/agent/baixa";
        }

        // Només si MySQL ha esborrat bé, eliminem els seus documents de MongoDB
        documentacioRepository.deleteByDniUsuari(dni);

        ra.addFlashAttribute("missatge", "Agent " + dni + " donat de baixa correctament.");
        return "redirect:/admin/agent/baixa";
    }
}
