package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.AgentRepository;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final UsuariRepository usuariRepository;
    private final DocumentacioRepository documentacioRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentService(AgentRepository agentRepository,
                        UsuariRepository usuariRepository,
                        DocumentacioRepository documentacioRepository,
                        PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.usuariRepository = usuariRepository;
        this.documentacioRepository = documentacioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ALTA
    public Agent alta(Agent agent) {
        if (usuariRepository.existsById(agent.getDniUsuari())) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest DNI.");
        }
        if (usuariRepository.existsByEmail(agent.getEmail())) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest correu.");
        }

        agent.setPasswordHash(passwordEncoder.encode(agent.getPasswordHash()));
        agent.setDataCreacio(LocalDateTime.now());
        agent.setEstatValidacio(Enums.EstatValidacio.Validat); // Los agentes nacen validados
        agent.setPremium(false);

        return agentRepository.save(agent);
    }

    // LISTADO PARA LA BAJA (con filtro opcional por DNI)
    public List<Agent> llistarAgents(String dni) {
        return (dni == null || dni.isBlank())
                ? agentRepository.findByRol(Enums.Rol.Agent)
                : agentRepository.findByRolAndDniUsuariContainingIgnoreCase(Enums.Rol.Agent, dni.trim());
    }

    // BAJA
    public void baixa(String dni) {
        Agent agent = agentRepository.findById(dni).orElse(null);

        // Solo se pueden dar de baja agentes con rol Agent (nunca administradores)
        if (agent == null || agent.getRol() != Enums.Rol.Agent) {
            throw new IllegalArgumentException("No s'ha trobat cap agent amb aquest DNI.");
        }

        try {
            agentRepository.deleteById(dni);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("No es pot donar de baixa l'agent " + dni
                    + ": té dades associades (clients validats o altres registres).");
        }

        // Solo si MySQL ha borrado bien, eliminamos sus documentos de MongoDB
        documentacioRepository.deleteByDniUsuari(dni);
    }
}