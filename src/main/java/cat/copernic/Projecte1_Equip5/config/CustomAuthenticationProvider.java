package cat.copernic.Projecte1_Equip5.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
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
/*CustomAuthenticationProvider es la pieza que decide si un login es válido o no. Cuando alguien pulsa “Entrar”, 
Spring le pasa el email y la contraseña escritos, y esta clase responde “sí, es quien dice ser” o “no”. */
    private final UsuariDetailsService userDetailsService; /* Busca al usuario en la BD usando UsuariDetailsService */
    private final PasswordEncoder passwordEncoder; /* Compara la contraseña escrita con el hash guardado, usando PasswordEncoder. */

    /**
     * Constructor con inyección de dependencias.
     *
     * @param userDetailsService servicio que carga los usuarios de la BD
     * @param passwordEncoder codificador de contraseñas
     */
    public CustomAuthenticationProvider(UsuariDetailsService userDetailsService,
                                        PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Autentica al usuario con el email y la contraseña recibidos.
     *
     * @param authentication datos introducidos en el login
     * @return la autenticación completada, con los roles del usuario
     * @throws AuthenticationException si el usuario no existe o la contraseña es incorrecta
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName(); // Saca del login lo que escribió el usuario. getName() es el email (el campo de usuario) 
         // y getCredentials() es la contraseña.
        String password = authentication.getCredentials().toString();

        UserDetails user = userDetailsService.loadUserByUsername(email);
        // Busca al usuario en la BD. Si no existe, UsuariDetailsService lanza UsernameNotFoundException y el login falla. 
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Credencials incorrectes");
        }
        /* Aquí está la comprobación clave. matches coge la contraseña escrita (1234), la pasa por BCrypt y la compara con el hash guardado ($2a$10$...).
        Si no coinciden, lanza BadCredentialsException. */
        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
        // Si todo va bien, devuelve el “carnet” del usuario autenticado: quién es (user) y qué roles tiene (getAuthorities(), por ejemplo ROLE_Administrador). 
        //Spring guarda esto en la sesión y lo consulta en cada petición para decidir si puede entrar a cada URL.
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
    /* Le dice a Spring: “yo me encargo de los logins con usuario y contraseña”. 
    Spring puede tener varios proveedores (por ejemplo, uno para login con Google), 
     y este método sirve para elegir cuál usar en cada caso. */
}
