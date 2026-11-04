package cat.copernic.Projecte1_Equip5.service;
 
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
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
}
 

