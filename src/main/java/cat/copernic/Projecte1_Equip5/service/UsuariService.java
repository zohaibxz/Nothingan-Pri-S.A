package cat.copernic.Projecte1_Equip5.service;
 
import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
 
/**
 * Servicio con la lógica común a todos los usuarios (clientes y agentes):
 * buscar por email, cambiar contraseña y modificar datos personales.
 * Trabaja con la clase padre Usuari, así que sirve para cualquier tipo de usuario.
 */
// @Service: Spring la registra como componente de la capa de servicio (lógica de negocio),
// para poder inyectarla en los controladores
@Service
public class UsuariService {
 
    // Repositorio para leer y guardar usuarios en la BD
    private final UsuariRepository usuariRepository;
 
    // Codificador de contraseñas (normalmente BCrypt): cifra las contraseñas antes de guardarlas
    private final PasswordEncoder passwordEncoder;
 
    // para tema CRUD documents i RESET password 
    private final UsuariLogic usuariLogic;
    
    // Inyección por constructor: Spring pasa las dos dependencias automáticamente
    public UsuariService(UsuariRepository usuariRepository,
                         PasswordEncoder passwordEncoder,
                         UsuariLogic usuariLogic/* añadido para CRUD documents I RESET*/) {
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuariLogic = usuariLogic;
    }
 
    // Buscar cualquier tipo de usuario por email (para login o perfil)
    // @Transactional(readOnly = true): ejecuta el método dentro de una transacción de solo lectura.
    // Mantiene la sesión de Hibernate abierta (evita LazyInitializationException) y es más eficiente
    // porque indica que no se va a modificar nada
    @Transactional(readOnly = true)
    public Optional<Usuari> cercarPerEmail(String email) {
        // Devuelve el Optional tal cual: quien lo llame decide qué hacer si no existe.
        // El objeto devuelto es de su clase real (Client o Agent)
        return usuariRepository.findByEmail(email);
    }
    
    @Transactional
    public void canviarContrasenya(String email, String actual, String nova, String confirmacio) {
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("L'usuari no existeix."));
        if (actual == null || !passwordEncoder.matches(actual, usuari.getPasswordHash())) {
            throw new IllegalArgumentException("La contrasenya actual no és correcta.");
        }
        if (!usuariLogic.isPasswordSecure(nova)) {
            throw new IllegalArgumentException("La contrasenya nova ha de tenir com a mínim 6 caràcters.");
        }
        if (!nova.equals(confirmacio)) {
            throw new IllegalArgumentException("La contrasenya nova i la confirmació no coincideixen.");
        }
        if (passwordEncoder.matches(nova, usuari.getPasswordHash())) {
            throw new IllegalArgumentException("La contrasenya nova ha de ser diferent de l'actual.");
        }
        // Cifra la contraseña nueva y la guarda (nunca en texto plano)
        usuari.setPasswordHash(passwordEncoder.encode(nova));
        usuariRepository.save(usuari);
    }
 
    // Caso de uso: Modificar dades personals generals (Compartido)
    @Transactional
    public Usuari actualitzarDadesPersonals(String email, String nom, String cognoms) {
        // Se busca al usuario por email; si no existe, error
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("L'usuari no existeix."));
 
        // Se cambian solo el nombre y los apellidos
        usuari.setNom(nom);
        usuari.setCognoms(cognoms);
        // save devuelve el usuario actualizado, que se devuelve a quien llamó al método
        return usuariRepository.save(usuari);
    }

    /**
     * Fila del listado de usuarios dados de baja.
     * tipus: "Client", "Agent" o "Administrador".
     */
    public record UsuariBaixa(String dni, String nom, String cognoms, String email,
            String tipus, LocalDateTime dataCreacio) {
    }

    // LISTADO de usuarios dados de baja (actiu = false), con búsqueda opcional.
    // Sirve para cualquier tipo de usuario porque trabaja con la clase padre Usuari.
    // nomesClients = true (lo usa el agente): solo devuelve clientes, nunca agentes ni administradores
    @Transactional(readOnly = true)
    public List<UsuariBaixa> llistarUsuarisDeBaixa(String cerca, boolean nomesClients) {
        // Texto buscado en minúsculas; vacío = sin filtro
        String q = (cerca == null) ? "" : cerca.trim().toLowerCase();
        return usuariRepository.findByActiuFalse().stream()
                .filter(u -> !nomesClients || "Client".equals(tipusDe(u)))
                .filter(u -> q.isEmpty()
                        || conte(u.getDniUsuari(), q)
                        || conte(u.getNom(), q)
                        || conte(u.getCognoms(), q)
                        || conte(u.getEmail(), q))
                .sorted(Comparator.comparing(Usuari::getCognoms, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Usuari::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(u -> new UsuariBaixa(u.getDniUsuari(), u.getNom(), u.getCognoms(),
                        u.getEmail(), tipusDe(u), u.getDataCreacio()))
                .toList();
    }

    // REACTIVAR un usuario dado de baja: vuelve a poner actiu = true.
    // Funciona igual para clientes, agentes y administradores.
    // nomesClients = true (lo usa el agente): si el usuario no es un cliente, se rechaza
    @Transactional
    public Usuari reactivar(String dni, boolean nomesClients) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("Cal indicar el DNI de l'usuari.");
        }
        Usuari usuari = usuariRepository.findById(dni.trim())
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat cap usuari amb aquest DNI."));
        if (nomesClients && !"Client".equals(tipusDe(usuari))) {
            throw new IllegalArgumentException("Només es poden reactivar clients.");
        }
        if (usuari.isActiu()) {
            throw new IllegalArgumentException("L'usuari " + dni + " ja està actiu.");
        }
        usuari.setActiu(true);
        return usuariRepository.save(usuari);
    }

    // Agent extiende Client, así que se mira primero si es Agent y se usa su rol (Agent/Administrador)
    private String tipusDe(Usuari usuari) {
        if (usuari instanceof Agent agent && agent.getRol() != null) {
            return agent.getRol().name();
        }
        return "Client";
    }

    private boolean conte(String text, String q) {
        return text != null && text.toLowerCase().contains(q);
    }
}
 

