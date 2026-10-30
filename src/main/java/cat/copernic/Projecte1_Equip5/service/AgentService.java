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
import org.springframework.transaction.annotation.Transactional;
 
/**
 * Servicio con la lógica de negocio de los agentes: alta, listado y baja.
 * Lo usa AgentController (zona de administración).
 */
// @Service: Spring la registra como componente de la capa de servicio,
// para poder inyectarla en los controladores
@Service
public class AgentService {
 
    // Repositorio de agentes (tabla agents en MySQL)
    private final AgentRepository agentRepository;
    // Repositorio de usuarios: para comprobar duplicados de DNI y email
    private final UsuariRepository usuariRepository;
    // Repositorio de MongoDB: aquí solo se usa para borrar la documentación al dar de baja
    private final DocumentacioRepository documentacioRepository;
    // Cifra las contraseñas (BCrypt) antes de guardarlas
    private final PasswordEncoder passwordEncoder;
 
    // Inyección por constructor: Spring pasa las cuatro dependencias automáticamente
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
    // Se llama desde AgentController.guardarAgent. Si algo no es válido lanza IllegalArgumentException,
    // que el controlador captura y muestra en el formulario.
    // @Transactional: si el método lanza una excepción, se deshace cualquier cambio en la BD
    @Transactional
    public Agent alta(Agent agent) {
        // existsById: el DNI es la clave primaria (@Id) de Usuari, así que busca por ahí
        if (usuariRepository.existsById(agent.getDniUsuari())) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest DNI.");
        }
        // El email tampoco puede repetirse (es lo que se usa para el login)
        if (usuariRepository.existsByEmail(agent.getEmail())) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest correu.");
        }
 
        // El formulario rellena passwordHash con la contraseña en texto plano (por el @ModelAttribute);
        // aquí se sustituye por su hash cifrado. Nunca se guarda en texto plano
        agent.setPasswordHash(passwordEncoder.encode(agent.getPasswordHash()));
        // Fecha de alta
        agent.setDataCreacio(LocalDateTime.now());
        agent.setEstatValidacio(Enums.EstatValidacio.Validat); // Los agentes nacen validados
        // Los agentes no son clientes premium
        agent.setPremium(false);
 
        // save: INSERT en MySQL; devuelve el agente guardado
        return agentRepository.save(agent);
    }
 
    // LISTADO PARA LA BAJA (con filtro opcional por DNI)
    // readOnly = true: transacción de solo lectura, más eficiente porque no se modifica nada
    @Transactional(readOnly = true)
    public List<Agent> llistarAgents(String dni) {
        // Operador ternario: condición ? si_cierto : si_falso
        // Si no hay DNI (null o solo espacios en blanco) -> todos los agentes con rol Agent.
        // Si hay DNI -> los que tengan rol Agent y cuyo DNI contenga ese texto (sin distinguir mayúsculas).
        // trim() quita los espacios sobrantes del texto buscado.
        // Siempre se filtra por Enums.Rol.Agent: los administradores nunca aparecen en este listado
        return (dni == null || dni.isBlank())
                ? agentRepository.findByRol(Enums.Rol.Agent)
                : agentRepository.findByRolAndDniUsuariContainingIgnoreCase(Enums.Rol.Agent, dni.trim());
    }
 
    // BAJA
    @Transactional
    public void baixa(String dni) {
        // orElse(null): si no existe, agent vale null en vez de lanzar excepción
        Agent agent = agentRepository.findById(dni).orElse(null);
 
        // Solo se pueden dar de baja agentes con rol Agent (nunca administradores)
        // Se comprueba primero agent == null para no dar NullPointerException en getRol()
        if (agent == null || agent.getRol() != Enums.Rol.Agent) {
            throw new IllegalArgumentException("No s'ha trobat cap agent amb aquest DNI.");
        }
 
        try {
            // DELETE en MySQL
            agentRepository.deleteById(dni);
        } catch (DataIntegrityViolationException e) {
            // Si el agente ya validó clientes (la clave foránea agentValidador de Client apunta a él),
            // MySQL no deja borrarlo. Se traduce a un mensaje legible para el usuario
            throw new IllegalArgumentException("No es pot donar de baixa l'agent " + dni
                    + ": té dades associades (clients validats o altres registres).");
        }
 
        // Solo si MySQL ha borrado bien, eliminamos sus documentos de MongoDB
        // (son dos bases de datos distintas: no hay clave foránea que lo haga automáticamente)
        documentacioRepository.deleteByDniUsuari(dni);
    }
}
 