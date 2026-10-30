package cat.copernic.Projecte1_Equip5.service;
 
import cat.copernic.Projecte1_Equip5.entity.Usuari;
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
 
    // Inyección por constructor: Spring pasa las dos dependencias automáticamente
    public UsuariService(UsuariRepository usuariRepository, PasswordEncoder passwordEncoder) {
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
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
 
    // Caso de uso: Reset password / Modificar contrasenya (Compartido)
    // @Transactional (sin readOnly): los cambios se confirman en la BD si todo va bien,
    // y se deshacen si el método lanza una excepción
    @Transactional
    public void canviarContrasenya(String email, String novaContrasenya) {
        // Se busca al usuario; si no existe, se lanza IllegalArgumentException
        // (los controladores la capturan para mostrar el mensaje)
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("L'usuari no existeix."));
 
        // encode() cifra la contraseña (hash). Nunca se guarda en texto plano:
        // en la BD queda solo el hash y en el login se compara con el PasswordEncoder
        usuari.setPasswordHash(passwordEncoder.encode(novaContrasenya));
        // save: actualiza la fila del usuario con el nuevo hash
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
 

