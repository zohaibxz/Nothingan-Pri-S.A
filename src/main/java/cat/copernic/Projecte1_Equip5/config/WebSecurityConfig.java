package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad: define qué rutas son públicas y
 * qué rol necesita cada una. Todo lo demás exige estar autenticado.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final CustomAuthenticationProvider authenticationProvider;

    /**
     * Constructor con inyección del proveedor de autenticación.
     *
     * @param authenticationProvider proveedor que valida email y contraseña
     */
    public WebSecurityConfig(CustomAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    /**
     * Define las reglas de acceso por ruta, el login y el logout.
     *
     * @param http configuración HTTP de Spring Security
     * @param deniedHandler manejador de accesos denegados
     * @return la cadena de filtros de seguridad
     * @throws Exception si la configuración falla
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AccesDenegatHandler deniedHandler) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                // Permite acceso libre a login, pantalla de acceso denegado y recursos estáticos (CSS, JS e imágenes)
                .requestMatchers("/", "/index", "/login", "/logout", "/client/alta", "/acces-denegat", "/css/**", "/js/**", "/img/**").permitAll()
                // Restricciones por rol de AuthorityType
                .requestMatchers("/admin/**", "/api/admin/**").hasRole(AuthorityType.ADMINISTRADOR.name())
                .requestMatchers("/agent/**").hasAnyRole(AuthorityType.ADMINISTRADOR.name(), AuthorityType.AGENT.name())
                .requestMatchers("/client/**").hasRole(AuthorityType.CLIENT.name())
                // Denegar cualquier otra ruta si no se ha iniciado sesión
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/dashboard", true) // Redirige directamente al DashBoardController
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout") // ruta del enlace 
                .logoutSuccessUrl("/login?logout") // Redirección tras tancar sessió
                .invalidateHttpSession(true)               // Destruye la sesión del servidor
                .clearAuthentication(true)                 // Borra las credenciales del usuario
                .deleteCookies("JSESSIONID")               // Elimina la cookie de sesión
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedHandler(deniedHandler))
            // Desactiva la caché del navegador para evitar ver datos antiguos tras cerrar sesión
            .headers(headers -> headers
                .cacheControl(cache -> cache.disable())
            );  

        return http.build();
    }
}