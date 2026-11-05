package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Proveedor de autenticación propio: busca el usuario por email y
 * comprueba que la contraseña introducida coincide con el hash guardado.
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    /* CustomAuthenticationProvider es la pieza que decide si un login es válido o no. Cuando alguien pulsa “Entrar”, 
       Spring le pasa el email y la contraseña escritos, y esta clase responde “sí, es quien dice ser” o “no”. */
    private final CustomUserDetailsService customUserDetailsService; /* Busca al usuario en la BD usando CustomUserDetailsService */
    /* Cargar los datos del usuario en el formato que entiende Spring Security (email, hash, roles) */
    private final PasswordEncoder passwordEncoder; /* Compara la contraseña escrita con el hash guardado, usando PasswordEncoder. */
    private final UsuariRepository usuariRepository; /* Añadido para la validación del estado del cliente */
    // Buscar el usuario en la BD por su email y mirar su estado
    

    /**
     * Constructor con inyección de dependencias.
     *
     * @param customUserDetailsService servicio que carga los usuarios de la BD
     * @param passwordEncoder          codificador de contraseñas
     * @param usuariRepository         repositorio para consultar el estado del usuario
     */
    public CustomAuthenticationProvider(CustomUserDetailsService customUserDetailsService,
                                         PasswordEncoder passwordEncoder,
                                         UsuariRepository usuariRepository) {
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.usuariRepository = usuariRepository;
    }

    /**
     * Autentica al usuario con el email y la contraseña recibidos.
     *
     * @param authentication datos introducidos en el login
     * @return la autenticación completada, con los roles del usuario
     * @throws AuthenticationException si el usuario no existe, está deshabilitado o la contraseña es incorrecta
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName(); // Saca del login el email
        String password = authentication.getCredentials().toString();

        // 1. Obtener entidad de la BD para comprobar el estado de validación
        Usuari usuariBD = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credencials incorrectes"));
        
        
        // 3. Cargar el UserDetails y validar la contraseña
        UserDetails user = customUserDetailsService.loadUserByUsername(email);
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Credencials incorrectes");
        }
        
        // aqui comprueba que el usuario que quiere hacer login este activo 
         /* Por qué aquí y no en CustomUserDetailsService: tu proveedor es propio y construye él mismo el token de autenticación, así que no pasa por las 
        comprobaciones automáticas de Spring (cuenta deshabilitada, etc.). Si no pones tu propia comprobación, el agente “de baja” entraría igual. 
        Va después de la contraseña para que alguien que no la conoce no pueda saber qué cuentas existen o están de baja.

        Tienes una limitación que debes conocer: si el agente tiene la sesión abierta cuando lo das de baja, sigue dentro hasta que caduque. 
        Para la práctica es aceptable */
        if (!usuariBD.isActiu()) {
            throw new DisabledException("El teu compte ha estat donat de baixa.");
        }
          
        // 2. Si es de tipo Client, comprobar que esté Validat
        if (usuariBD instanceof Client client) {
            if (client.getEstatValidacio() == Enums.EstatValidacio.Pendent) {
                throw new DisabledException("El teu compte està pendent de validació per un administrador.");
            } else if (client.getEstatValidacio() == Enums.EstatValidacio.Rebutjat) {
                throw new DisabledException("El teu compte ha estat rebutjat.");
            }
        }
        
        

        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
    }

    /**
     * Indica que este proveedor gestiona logins de usuario y contraseña.
     *
     * @param authentication tipo de autenticación a comprobar
     * @return true si el tipo es soportado
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}