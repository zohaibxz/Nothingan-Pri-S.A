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
 * buscar por email, cambiar contraseña y modificar datos personales. Trabaja
 * con la clase padre Usuari, así que sirve para cualquier tipo de usuario.
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

    /**
     * Constructor amb injecció de dependències.
     *
     * @param usuariRepository repositori d'usuaris
     * @param passwordEncoder codificador de contrasenyes
     * @param usuariLogic regles de validació (contrasenya, telèfon...)
     */
    public UsuariService(UsuariRepository usuariRepository,
            PasswordEncoder passwordEncoder,
            UsuariLogic usuariLogic/* añadido para CRUD documents I RESET*/) {
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuariLogic = usuariLogic;
    }

    /**
     * Busca un usuari pel correu electrònic.
     *
     * @param email correu de l'usuari
     * @return l'usuari, si existeix
     */
    @Transactional(readOnly = true)
    public Optional<Usuari> cercarPerEmail(String email) {
        // Devuelve el Optional tal cual: quien lo llame decide qué hacer si no existe.
        // El objeto devuelto es de su clase real (Client o Agent)
        return usuariRepository.findByEmail(email);
    }

    /**
     * Canvia la contrasenya d'un usuari. Comprova la contrasenya actual, que la
     * nova sigui prou segura i que coincideixi amb la confirmació, i la desa
     * xifrada.
     *
     * @param email correu de l'usuari
     * @param actual contrasenya actual
     * @param nova contrasenya nova
     * @param confirmacio repetició de la contrasenya nova
     * @throws IllegalArgumentException si alguna comprovació falla
     */
    @Transactional
    public void canviarContrasenya(String email, String actual, String nova, String confirmacio) {
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("L'usuari no existeix."));
        if (actual == null || !passwordEncoder.matches(actual, usuari.getPasswordHash())) {
            throw new IllegalArgumentException("La contrasenya actual no és correcta.");
        }
        if (!usuariLogic.isPasswordSecure(nova)) {
            throw new IllegalArgumentException("La contrasenya nova ha de tenir entre 8 i 72 caràcters i incloure com a mínim una lletra i un número.");
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

    /**
     * Actualitza el nom i els cognoms d'un usuari.
     *
     * @param email correu de l'usuari
     * @param nom nom nou
     * @param cognoms cognoms nous
     * @return l'usuari desat
     * @throws IllegalArgumentException si l'usuari no existeix
     */
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
     * Fila del listado de usuarios dados de baja. tipus: "Client", "Agent" o
     * "Administrador".
     */
    public record UsuariBaixa(String dni, String nom, String cognoms, String email,
            String tipus, LocalDateTime dataCreacio) {

    }

    /**
     * Llista els usuaris donats de baixa, amb cerca opcional (DNI, nom, cognoms
     * o correu), ordenats per cognoms i nom. Serveix per a qualsevol tipus
     * d'usuari.
     *
     * @param cerca text a cercar (pot ser null)
     * @param nomesClients true si només s'han de veure clients (cas de l'agent)
     * @return usuaris donats de baixa
     */
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

    /**
     * Reactiva un usuari donat de baixa. Si nomesClients és true i no és un
     * client, es rebutja.
     *
     * @param dni DNI de l'usuari
     * @param nomesClients true si només es poden reactivar clients (cas de
     * l'agent)
     * @return l'usuari reactivat
     * @throws IllegalArgumentException si no existeix, ja és actiu o no té
     * permís
     */
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

    /**
     * Tipus d'usuari: el rol si és agent o administrador, o Client.
     *
     * @param usuari usuari a classificar
     * @return Agent, Administrador o Client
     */
    private String tipusDe(Usuari usuari) {
        if (usuari instanceof Agent agent && agent.getRol() != null) {
            return agent.getRol().name();
        }
        return "Client";
    }

    /**
     * Comprova si un text conté la cerca (en minúscules).
     *
     * @param text text a mirar (pot ser null)
     * @param q cerca ja en minúscules
     * @return true si el text la conté
     */
    private boolean conte(String text, String q) {
        return text != null && text.toLowerCase().contains(q);
    }
}
