package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

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
}
